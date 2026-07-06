# Regles ProGuard/R8 — Planete Bowl Print Agent.
# Gson utilise la reflexion sur les DTO : on les conserve tels quels.
-keep class ch.planetebowl.printagent.data.remote.dto.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Room genere son propre code, pas de regle specifique necessaire au-dela du
# plugin AGP par defaut. Hilt/Dagger fournissent leurs propres regles via
# consumer-rules embarquees dans leurs aar.
