// Planet Bowl — agent d'impression (print-agent)
//
// Contrat consommé par la tablette Android connectée en direct à une
// imprimante Epson (TCP/ESC-POS, port 9100). La tablette n'a jamais accès à
// la base de données ni à la clé service_role : elle ne parle qu'à cette
// Edge Function, qui seule sait lire/écrire dans print_jobs (via les
// fonctions SQL claim_print_job / confirm_print_job / release_print_job,
// définies dans supabase/migrations/0006_print_agent.sql).
//
// Routes exposées sous /functions/v1/print-agent :
//   GET  /jobs?restaurantId=<uuid>&limit=20
//   POST /jobs/{orderId}/claim?restaurantId=<uuid>
//   POST /jobs/{orderId}/printed   { claimToken, printedAt }
//   POST /jobs/{orderId}/failed    { claimToken, errorCode, errorMessage }
//
// Authentification : header `Authorization: Bearer <PRINT_AGENT_TOKEN>`,
// comparé en temps constant pour ne pas faciliter une attaque par timing.

import { createClient, type SupabaseClient } from "npm:@supabase/supabase-js@2";

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

// ---------------------------------------------------------------------------
// Types du contrat JSON (voir description dans la consigne / doc Android)
// ---------------------------------------------------------------------------

interface CustomizationsPayload {
  base: string | null;
  protein: string | null;
  garnish: string[];
  extra_protein: string[];
  topping: string[];
  sauce: string[];
}

interface OrderItemPayload {
  name: string;
  quantity: number;
  unitPriceChf: string;
  customizations: CustomizationsPayload;
}

interface PrintJobPayload {
  orderId: string;
  orderCode: string;
  payloadVersion: number;
  createdAt: string;
  orderType: "TAKEAWAY";
  pickupSlot: string;
  customerName: string;
  phone: string | null;
  address: null;
  items: OrderItemPayload[];
  totalChf: string;
  kitchenNote: string | null;
  restaurantId: string;
}

interface RawOrderItemRow {
  name: string;
  quantity: number;
  unit_price: number | string;
  customizations: Record<string, unknown> | null;
}

interface RawProfileRow {
  first_name: string | null;
  last_name: string | null;
  phone: string | null;
}

interface RawOrderRow {
  code: string;
  created_at: string;
  pickup_slot: string;
  total: number | string;
  note: string | null;
  profiles: RawProfileRow | null;
  order_items: RawOrderItemRow[] | null;
}

interface RawPrintJobRow {
  order_id: string;
  restaurant_id: string;
  orders: RawOrderRow | null;
}

interface ClaimRpcResult {
  resultStatus: "CLAIMED" | "NOT_FOUND" | "ALREADY_PRINTED" | "MAX_ATTEMPTS_REACHED" | "ALREADY_CLAIMED";
  claimToken?: string;
  claimExpiresAt?: string;
}

interface ConfirmRpcResult {
  resultStatus: "CONFIRMED" | "ALREADY_CONFIRMED" | "CLAIM_MISMATCH";
}

interface ReleaseRpcResult {
  resultStatus: "PENDING" | "MANUAL_REVIEW" | "CLAIM_MISMATCH";
}

interface PrintedBody {
  claimToken?: string;
  printedAt?: string;
}

interface FailedBody {
  claimToken?: string;
  errorCode?: string;
  errorMessage?: string;
}

// ---------------------------------------------------------------------------
// Client Supabase (service_role : contourne volontairement RLS côté serveur)
// ---------------------------------------------------------------------------

const supabaseUrl = Deno.env.get("SUPABASE_URL");
const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

function getSupabaseClient(): SupabaseClient {
  if (!supabaseUrl || !serviceRoleKey) {
    // Ne devrait jamais arriver en production : ces variables sont
    // auto-injectées par le runtime Edge Functions de Supabase.
    throw new Error("Configuration Supabase manquante");
  }
  return createClient(supabaseUrl, serviceRoleKey, {
    auth: { persistSession: false },
  });
}

// ---------------------------------------------------------------------------
// Utilitaires
// ---------------------------------------------------------------------------

function isUuid(value: string | null | undefined): value is string {
  return typeof value === "string" && UUID_RE.test(value);
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json; charset=utf-8" },
  });
}

// Comparaison en temps constant : évite qu'un attaquant devine le token
// caractère par caractère en mesurant le temps de réponse.
function timingSafeEqual(a: string, b: string): boolean {
  const bufA = new TextEncoder().encode(a);
  const bufB = new TextEncoder().encode(b);
  const length = Math.max(bufA.length, bufB.length, 1);
  let diff = bufA.length === bufB.length ? 0 : 1;
  for (let i = 0; i < length; i++) {
    const byteA = i < bufA.length ? bufA[i] : 0;
    const byteB = i < bufB.length ? bufB[i] : 0;
    diff |= byteA ^ byteB;
  }
  return diff === 0;
}

function isAuthorized(req: Request): boolean {
  const expected = Deno.env.get("PRINT_AGENT_TOKEN");
  if (!expected) {
    // Secret non configuré : on refuse tout plutôt que d'ouvrir l'accès par défaut.
    console.error("print-agent: PRINT_AGENT_TOKEN n'est pas configuré");
    return false;
  }
  const header = req.headers.get("Authorization") ?? "";
  if (!header.startsWith("Bearer ")) {
    return false;
  }
  const provided = header.slice("Bearer ".length).trim();
  return timingSafeEqual(provided, expected);
}

// Acceptée pour compatibilité avec le client Android ; en v1 le claim_token
// suffit déjà à garantir l'idempotence des routes printed/failed, donc cette
// clé est simplement lue et ignorée (pas de table de dédoublonnage dédiée).
function readIdempotencyKey(req: Request): string | null {
  return req.headers.get("Idempotency-Key");
}

function formatChf(value: number | string | null | undefined): string {
  const numeric = typeof value === "string" ? Number(value) : (value ?? 0);
  if (!Number.isFinite(numeric)) return "0.00";
  return numeric.toFixed(2);
}

function toStringArray(value: unknown): string[] {
  if (!Array.isArray(value)) return [];
  return value.filter((entry): entry is string => typeof entry === "string");
}

function toCustomizationsPayload(raw: Record<string, unknown> | null): CustomizationsPayload {
  if (!raw) {
    return { base: null, protein: null, garnish: [], extra_protein: [], topping: [], sauce: [] };
  }
  return {
    base: typeof raw.base === "string" ? raw.base : null,
    protein: typeof raw.protein === "string" ? raw.protein : null,
    garnish: toStringArray(raw.garnish),
    extra_protein: toStringArray(raw.extra_protein),
    topping: toStringArray(raw.topping),
    sauce: toStringArray(raw.sauce),
  };
}

function rowToPayload(row: RawPrintJobRow): PrintJobPayload | null {
  const order = row.orders;
  if (!order) {
    // Ne devrait jamais arriver (order_id est not null + FK vers orders),
    // garde-fou défensif pour ne pas planter toute la liste sur une anomalie.
    return null;
  }

  const profile = order.profiles;
  const customerName =
    [profile?.first_name, profile?.last_name].filter((part) => Boolean(part && part.trim())).join(" ").trim() ||
    "Client";

  return {
    orderId: row.order_id,
    orderCode: order.code,
    payloadVersion: 1,
    createdAt: order.created_at,
    // Le schéma Planet Bowl ne distingue pas encore sur-place / livraison :
    // seul le retrait (pickup_slot) existe aujourd'hui. Valeur figée en
    // attendant une évolution du schéma (ajout d'un vrai type de commande).
    orderType: "TAKEAWAY",
    pickupSlot: order.pickup_slot,
    customerName,
    phone: profile?.phone ?? null,
    address: null,
    items: (order.order_items ?? []).map((item) => ({
      name: item.name,
      quantity: item.quantity,
      unitPriceChf: formatChf(item.unit_price),
      customizations: toCustomizationsPayload(item.customizations),
    })),
    totalChf: formatChf(order.total),
    kitchenNote: order.note ?? null,
    restaurantId: row.restaurant_id,
  };
}

// ---------------------------------------------------------------------------
// Handlers
// ---------------------------------------------------------------------------

async function handleListJobs(supabase: SupabaseClient, url: URL): Promise<Response> {
  const restaurantId = url.searchParams.get("restaurantId");
  if (!isUuid(restaurantId)) {
    return jsonResponse({ code: "INVALID_RESTAURANT_ID", message: "restaurantId invalide ou manquant." }, 400);
  }

  const limitParam = url.searchParams.get("limit");
  let limit = 20;
  if (limitParam !== null) {
    const parsed = Number(limitParam);
    if (!Number.isInteger(parsed) || parsed < 1 || parsed > 100) {
      return jsonResponse({ code: "INVALID_LIMIT", message: "limit doit être un entier entre 1 et 100." }, 400);
    }
    limit = parsed;
  }

  const nowIso = new Date().toISOString();

  const { data, error } = await supabase
    .from("print_jobs")
    .select(
      `
      order_id,
      restaurant_id,
      orders (
        code,
        created_at,
        pickup_slot,
        total,
        note,
        profiles ( first_name, last_name, phone ),
        order_items ( name, quantity, unit_price, customizations )
      )
    `,
    )
    .eq("restaurant_id", restaurantId)
    .or(`status.eq.PENDING,and(status.eq.CLAIMED,claim_expires_at.lt.${nowIso})`)
    .order("created_at", { ascending: true })
    .limit(limit);

  if (error) {
    console.error("print-agent: erreur liste des jobs", error.message);
    return jsonResponse({ code: "INTERNAL_ERROR", message: "Erreur interne du serveur." }, 500);
  }

  const jobs = (data as unknown as RawPrintJobRow[] | null ?? [])
    .map(rowToPayload)
    .filter((job): job is PrintJobPayload => job !== null);

  return jsonResponse({ jobs });
}

async function handleClaim(supabase: SupabaseClient, url: URL, orderId: string): Promise<Response> {
  const restaurantId = url.searchParams.get("restaurantId");
  if (!isUuid(restaurantId)) {
    return jsonResponse({ code: "INVALID_RESTAURANT_ID", message: "restaurantId invalide ou manquant." }, 400);
  }

  const { data, error } = await supabase.rpc("claim_print_job", {
    p_order_id: orderId,
    p_restaurant_id: restaurantId,
  });

  if (error) {
    console.error("print-agent: erreur claim", error.message);
    return jsonResponse({ code: "INTERNAL_ERROR", message: "Erreur interne du serveur." }, 500);
  }

  const result = data as ClaimRpcResult;

  switch (result.resultStatus) {
    case "CLAIMED":
      return jsonResponse({ claimToken: result.claimToken, expiresAt: result.claimExpiresAt });
    case "NOT_FOUND":
      return jsonResponse({ code: "NOT_FOUND", message: "Commande inconnue pour ce restaurant." }, 404);
    case "ALREADY_PRINTED":
      return jsonResponse({ code: "ALREADY_PRINTED" }, 409);
    case "MAX_ATTEMPTS_REACHED":
      return jsonResponse({ code: "MAX_ATTEMPTS_REACHED" }, 409);
    case "ALREADY_CLAIMED":
    default:
      return jsonResponse({ code: "ALREADY_CLAIMED" }, 409);
  }
}

async function handlePrinted(supabase: SupabaseClient, req: Request, orderId: string): Promise<Response> {
  let body: PrintedBody;
  try {
    body = await req.json();
  } catch {
    return jsonResponse({ code: "INVALID_BODY", message: "Corps JSON invalide." }, 400);
  }

  if (!isUuid(body.claimToken)) {
    return jsonResponse({ code: "INVALID_BODY", message: "claimToken requis." }, 400);
  }

  let printedAt = new Date();
  if (body.printedAt !== undefined) {
    printedAt = new Date(body.printedAt);
    if (Number.isNaN(printedAt.getTime())) {
      return jsonResponse({ code: "INVALID_BODY", message: "printedAt invalide." }, 400);
    }
  }

  const { data, error } = await supabase.rpc("confirm_print_job", {
    p_order_id: orderId,
    p_claim_token: body.claimToken,
    p_printed_at: printedAt.toISOString(),
  });

  if (error) {
    console.error("print-agent: erreur printed", error.message);
    return jsonResponse({ code: "INTERNAL_ERROR", message: "Erreur interne du serveur." }, 500);
  }

  const result = data as ConfirmRpcResult;

  if (result.resultStatus === "CONFIRMED") {
    return jsonResponse({ status: "CONFIRMED" });
  }
  if (result.resultStatus === "ALREADY_CONFIRMED") {
    return jsonResponse({ status: "ALREADY_CONFIRMED" });
  }
  return jsonResponse({ code: "CLAIM_MISMATCH" }, 409);
}

// Nettoie les messages d'erreur imprimante avant stockage : jamais de saut
// de ligne (casserait un affichage ligne par ligne côté dashboard) et une
// longueur bornée pour rester dans les colonnes text sans mauvaise surprise.
function sanitize(value: string | undefined, maxLength: number): string | null {
  if (!value) return null;
  return value.replace(/[\r\n]+/g, " ").slice(0, maxLength);
}

async function handleFailed(supabase: SupabaseClient, req: Request, orderId: string): Promise<Response> {
  let body: FailedBody;
  try {
    body = await req.json();
  } catch {
    return jsonResponse({ code: "INVALID_BODY", message: "Corps JSON invalide." }, 400);
  }

  if (!isUuid(body.claimToken) || !body.errorCode) {
    return jsonResponse({ code: "INVALID_BODY", message: "claimToken et errorCode requis." }, 400);
  }

  const { data, error } = await supabase.rpc("release_print_job", {
    p_order_id: orderId,
    p_claim_token: body.claimToken,
    p_error_code: sanitize(body.errorCode, 100),
    p_error_message: sanitize(body.errorMessage, 500),
  });

  if (error) {
    console.error("print-agent: erreur failed", error.message);
    return jsonResponse({ code: "INTERNAL_ERROR", message: "Erreur interne du serveur." }, 500);
  }

  const result = data as ReleaseRpcResult;

  if (result.resultStatus === "PENDING") {
    return jsonResponse({ status: "RELEASED" });
  }
  if (result.resultStatus === "MANUAL_REVIEW") {
    return jsonResponse({ status: "MANUAL_REVIEW" });
  }
  return jsonResponse({ code: "CLAIM_MISMATCH" }, 409);
}

// ---------------------------------------------------------------------------
// Routage
// ---------------------------------------------------------------------------

Deno.serve(async (req: Request) => {
  try {
    if (!isAuthorized(req)) {
      return jsonResponse({ code: "UNAUTHORIZED", message: "Authentification requise." }, 401);
    }

    // Lue une fois pour tous les POST ; ignorée en v1 (voir readIdempotencyKey).
    readIdempotencyKey(req);

    const url = new URL(req.url);
    const segments = url.pathname.split("/").filter(Boolean);
    const anchor = segments.indexOf("print-agent");
    const route = anchor >= 0 ? segments.slice(anchor + 1) : segments;

    const supabase = getSupabaseClient();

    if (req.method === "GET" && route.length === 1 && route[0] === "jobs") {
      return await handleListJobs(supabase, url);
    }

    if (req.method === "POST" && route.length === 3 && route[0] === "jobs") {
      const [, orderId, action] = route;
      if (!isUuid(orderId)) {
        return jsonResponse({ code: "INVALID_ORDER_ID", message: "orderId invalide." }, 400);
      }
      if (action === "claim") return await handleClaim(supabase, url, orderId);
      if (action === "printed") return await handlePrinted(supabase, req, orderId);
      if (action === "failed") return await handleFailed(supabase, req, orderId);
    }

    return jsonResponse({ code: "NOT_FOUND", message: "Route inconnue." }, 404);
  } catch (error) {
    console.error("print-agent: erreur interne", error instanceof Error ? error.message : error);
    return jsonResponse({ code: "INTERNAL_ERROR", message: "Erreur interne du serveur." }, 500);
  }
});
