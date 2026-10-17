# Invoice App Agent Rules

- Preserve offline-first behavior and locally stored customer/quote data unless requested.
- Treat customer names, phone numbers, addresses, and tax details as sensitive; never log or expose real records.
- Preserve SAR currency, Arabic/RTL layout, PDF readability, and correct totals/rounding.
- Test quantity, unit price, discount, tax, subtotal, and total before changing calculations.
- Avoid adding login, paid APIs, or server dependencies unless explicitly approved.
- Never claim a PDF or APK was generated unless the file exists and was checked.
- Do not overwrite user data, change schemas, or publish a release without approval.
