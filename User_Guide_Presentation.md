% KPI Data Generator User Guide
% AAC KPI Team
% Today

# Agenda
- Overview of KPI Data Generator
- Tab responsibilities
- Data flow and generation logic
- Export pipelines and validation
- Next steps and recommendations

# KPI Data Generator Overview
## Purpose
- Rapidly compose consistent KPI inputs for reporting
- Blend dummy generation with uploaded sheets for validation
- Provide JSON + CSV exports for downstream systems

# Tab Responsibilities
- Patient Master: holds demographics, KPI groups, attendance refs
- Event Session: schedules, attendance, purpose, and venue info
- Practitioner Master: volunteer metadata derived from master data
- Encounter Master: cross-referenced encounter logs with patients
- QuestionnaireResponse Master: question scores per patient
- Common: aggregates all sheets into export-ready rows
- KPI JSON: drives external converter to build FHIR bundles
- JSON → CSV: flattens JSON into AAC_ID/Raw_Resource pairs

# Data Flow and Generation Logic
- Upload Excel fills every observable list via ExcelReader
- Generators use RandomDataUtil, NRICGeneratorUtil, and dialogs per tab
- LinkService keeps Patient/Event references synchronized
- CommonBuilderService combines all sheets with selected CFS + risk tags
- MasterDataService generates volunteers, wired into Practitioner tab

# Export Pipelines & Validation
- ExcelWriter persists all lists plus Common rows; volunteer count prompted
- JsonExportService runs external JAR with counts auto-filled
- JsonCsvController walks JSON folders, extracts AAC IDs with Gson, emits CSV
- Analyze buttons on tables run validation heuristics (IDs, formats, duplicates)

# Visual Diagram Recap
- Patient/Event/Practitioner/Encounter/Questionnaire feed Common
- Common → Excel + Json Export → JSON → CSV pipeline
- Master data seeds Practitioner Master, gravitates toward exports

# Recommendations
- Use User Guide tab as single reference for tab behavior and data sources
- Refresh master data when volunteer roster changes
- Validate generated data with Analyze before exporting
- Share PPT copy with leadership before demo
