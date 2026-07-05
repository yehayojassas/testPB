# Design QA

- source visual truth path: `references/customize.png`
- implementation screenshot path: `qa/customize-final.png`
- comparison evidence: `qa/customize-comparison.png`
- viewport: `390 × 844`
- state: `À composer → Compose ton bowl → étape 1 Base`

**Findings**

- No actionable P0/P1/P2 findings remain.
- Typography: Cormorant Garamond and DM Sans preserve the reference hierarchy and readable mobile scale.
- Spacing and layout rhythm: header, product hero, four-step navigation, choice controls, and sticky action area preserve the source composition without overlap at 390 px.
- Colors and tokens: cream, forest green, lime, and coral remain consistent with the source and have accessible contrast.
- Image quality: official Planet Bowl product photography replaces the earlier generic bowl imagery. The different crop is intentional and source-grounded.
- Copy and content: official Saint-Laurent menu names and prices are used for Signature and À composer.
- Interaction: carousel auto-advances; Signature and À composer switch content; every product opens customization; customization begins at step 1; restaurant selection and ordering flow remain functional.
- Accessibility: semantic buttons, labels, alt text, practical tap targets, selected states, and visible button states are present.

**Open Questions**

- The ninth restaurant address is still a data placeholder because the official Planet Bowl site currently exposes eight public addresses.

**Implementation Checklist**

- [x] Automatic promotional carousel.
- [x] Company story section below the first viewport.
- [x] Official menu names, prices, and photographs.
- [x] Functional Signature and À composer categories.
- [x] Product selection opens customization at step 1.
- [x] Mobile build completed.

**Focused Region Comparison**

The combined comparison clearly exposes the complete 390 px customization viewport, including typography, product imagery, step navigation, controls, and sticky CTA; no additional crop was required.

**Patches made since previous QA pass**

- Replaced the account icon on customization with the cart state shown by the design system.
- Replaced invented products with official Planet Bowl items and imagery.
- Reset customization to Base (step 1) for every product entry.
- Added the auto-advancing carousel and company section.

**Follow-up Polish**

- P3: add swipe gestures to the hero carousel if desired; dots and timed auto-advance already work.

final result: passed
