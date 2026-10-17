# ChatGPT / ChatGPT Work activation prompt

Copy this prompt into a ChatGPT Work task after connecting the correct GitHub repository. It does not grant permissions or automatically activate tools.

---
Before editing, read AGENTS.md, docs/AGENT-SKILLS-SETUP.md, and relevant skills under skills/ and .agents/skills/. Establish branch/commit, architecture, build/test commands, current features, and CI baseline.

Preserve offline-first behavior and local customer/quote data. Verify arithmetic, rounding, currency formatting (SAR), PDF page breaks, Arabic text shaping/fonts, logo sizing, and data persistence. Do not send customer data to external APIs unless explicitly requested and disclosed. Never claim a PDF or APK is correct without running the relevant tests/build and inspecting outputs.

Use a separate branch and minimal changes. Do not commit credentials, signing keys, customer records, or private app data. For security tasks, use authorized scope only and separate confirmed findings, suspected risks, and untested controls. Do not deploy/release or change production data without explicit approval.

Run available checks and report exact output. Separate status into proposed, coded, built, deployed, and production-tested.
---