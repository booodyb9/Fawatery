# Invoice integrity and validation

This change fixes the nine reviewed issues without adding a server, login or paid API.

- Records use a SQLite transaction containing a versioned typed snapshot. UI state changes only after commit. Failed transactions keep the prior snapshot. An unreadable database/snapshot is never replaced with empty data.
- A legacy development SharedPreferences snapshot is read only if the database has no row; it is retained during migration.
- New installations start with empty invoices and clients. The existing product catalog remains available.
- Invoice number reservations persist independently of record count and are never reused after deletion. Gaps after cancelled/failed creation are possible.
- Line amounts: quantity × unit price − absolute SAR discount. Round each line to two decimal places with HALF_UP, sum rounded lines, then calculate and round VAT to two decimal places. Maximum supported invoice amount is SAR 999,999,999,999.99; malformed, non-finite and out-of-range values are rejected.
- Seller identity, selected company logo, bank details and actual UTC issuance time are captured on first issuance. Unissued drafts take current seller details when issued; later company-profile changes do not rewrite issued snapshots.
- Drafts are excluded from billed balances, collection totals and VAT summaries. Client balances derive from linked invoices and payment status.
- Reports cover the currently displayed calendar quarter and export every non-draft invoice in that period. Invoice totals occur only on the first CSV line per invoice, so summing them does not double count multi-line invoices.
- Seller and buyer tax numbers are optional; supplied numbers must be valid. Issuance, status changes, PDF and CSV export work without one. Tax rates and money calculations remain independent of the optional identifier. Tax TLV QR is omitted when the issued seller has no tax number.
- Logos are selected with the Android image picker, normalized to at most 256 pixels with orientation/alpha/aspect ratio preserved, and copied to immutable app-owned files. Inputs larger than 10 MiB or unreadable images are rejected. Profile saves reference the copy, and issued seller snapshots retain the original logo. Old JSON profiles without a logo field keep the built-in logo. Logo files referenced by issued invoices are retained when the current logo changes.
- QR uses a real ZXing encoder and five-field TLV payload. This does not implement ZATCA Phase 2 signing, clearance or reporting; no certification is claimed.
- PDF uses Arabic-aware StaticLayout wrapping and page breaks. Long descriptions, notes and payment terms are retained. The logo preserves its aspect ratio.
- Reminder suggestions use the actual date and persisted switches. WhatsApp/SMS sending remains a user action; no background customer messaging is introduced.

Automated checks cover decimal boundaries, storage recreation and rollback, numbering, drafts/issuance, balances, reminder timing/options, CSV completeness/escaping, QR encode/decode, and rendering a long Arabic PDF whose printed QR is decoded again.

The PDF renderer check runs as an instrumented Android test on an API 29 emulator; JVM unit tests do not emulate the platform PDF engine.

CI uploads only synthetic PDF/PNG samples under invoice-pdf-validation. No real customer records are used. On-device visual verification of mixed Arabic/English text and the external sharing apps remains recommended.


## Quotations and document-specific logos
Quotation records live in an optional `quotes` array in the existing version-1 JSON payload; the SQLite schema is unchanged. Old snapshots default to an empty quote list. No existing data is reset. Quote numbering uses a separate durable QTE-year sequence. Quotes are never inserted into the invoice list or collection/VAT/balance summaries.

Conversion checks validity and writes the new invoice, number reservation and quote link in one transaction. Repeating conversion returns the same invoice. A deleted linked invoice is not recreated silently. Converted quotes cannot be edited; deleting the quote keeps its invoice. Conversion preserves the agreed item/tax/branding snapshot, uses today's invoice date and a default due date 14 days later.

The creation editor supports a local logo override for each document without changing the global profile. Item state, numeric input drafts and validation errors survive saved-state recreation; invalid text cannot silently fall back to previously valid amounts.

PDFs now use an RTL item table. All cells wrap and split on whole layout-line boundaries, repeated headers identify continued pages, and totals have a separate summary. Quotations show validity dates and omit tax-invoice QR encoding even when seller VAT is present.

CI uses synthetic records for quote storage/conversion/rollback tests and native editor creation/restoration tests. Fixed synthetic invoice and quotation PNG previews are printed in bounded chunks for visual inspection; no repository/customer documents are rendered by this step.
