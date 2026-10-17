---
name: invoice-pdf-validation
description: Validate invoice calculations, Arabic PDF output, local persistence, and privacy for an offline-first Android invoicing app.
---

# Invoice PDF Validation

1. Inspect existing calculation and PDF generation paths before edits.
2. Verify line totals, quantity/unit math, discounts, tax rules, subtotals, and final totals using representative and boundary cases.
3. Test Arabic shaping, RTL order, fonts, long names, page breaks, and logo scaling in exported PDFs.
4. Check SAR formatting and make rounding rules explicit.
5. Ensure sensitive customer data is not logged or sent externally without consent.
6. Verify local save/load and empty-state behavior; never delete existing records during testing.
7. Run relevant tests/build and inspect a generated sample PDF if tools allow.
8. Report exactly what was tested and what remains unverified.
