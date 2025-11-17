AAC KPI Data Tool — Developer Guide and Clickable Mindmap

Overview
- Desktop JavaFX app to generate, edit, analyze, and export KPI data to Excel.
- Core flows: load/generate data → link across sheets → validate → build Common (resident_report and other sections) → export.

How To View/Use The Mindmap
- This README embeds a Mermaid diagram. On GitHub or IDEs that support Mermaid, nodes are clickable.
- Click any node to jump to the method implementation. If your viewer does not support click actions, use the “Method Index” list below.

Architecture Mindmap (clickable)
```mermaid
flowchart TD
    subgraph App[JavaFX App]
        A1[Main.start]:::link
        A2[MainController.initialize]:::link
        click A1 "src/main/java/com/aac/kpi/Main.java#L11" "Main.start()"
        click A2 "src/main/java/com/aac/kpi/MainController.java#L49" "MainController.initialize()"
    end

    subgraph UI[Editors (Tabs)]
        U1[PatientMaster.onGenerate]:::link
        U2[EventSession.onGenerateSessions]:::link
        U3[EncounterMaster.onGenerate]:::link
        U4[QuestionnaireResponse.onGenerate]:::link
        U5[Common.onBuild]:::link
        click U1 "src/main/java/com/aac/kpi/controller/PatientMasterController.java#L118" "Generate Patients"
        click U2 "src/main/java/com/aac/kpi/controller/EventSessionController.java#L120" "Generate Sessions"
        click U3 "src/main/java/com/aac/kpi/controller/EncounterMasterController.java#L79" "Generate Encounters"
        click U4 "src/main/java/com/aac/kpi/controller/QuestionnaireResponseMasterController.java#L78" "Generate Questionnaires"
        click U5 "src/main/java/com/aac/kpi/controller/CommonController.java#L75" "Build Common"
    end

    subgraph IO[Excel I/O]
        I1[ExcelReader.readPatients]:::link
        I2[ExcelReader.readEventSessions]:::link
        I3[ExcelReader.readEncounters]:::link
        I4[ExcelReader.readQuestionnaires]:::link
        I5[ExcelWriter.saveToExcel]:::link
        I6[ExcelWriter.writeCombinedCommonSheet]:::link
        I7[ExcelWriter.writeResidentReportSection]:::link
        click I1 "src/main/java/com/aac/kpi/service/ExcelReader.java#L17" "readPatients(File)"
        click I2 "src/main/java/com/aac/kpi/service/ExcelReader.java#L148" "readEventSessions(File)"
        click I3 "src/main/java/com/aac/kpi/service/ExcelReader.java#L60"  "readEncounters(File)"
        click I4 "src/main/java/com/aac/kpi/service/ExcelReader.java#L86"  "readQuestionnaires(File)"
        click I5 "src/main/java/com/aac/kpi/service/ExcelWriter.java#L47" "saveToExcel(...)"
        click I6 "src/main/java/com/aac/kpi/service/ExcelWriter.java#L314" "writeCombinedCommonSheet(...)"
        click I7 "src/main/java/com/aac/kpi/service/ExcelWriter.java#L422" "writeResidentReportSection(...)"
    end

    subgraph Logic[Linking + Build + Validate]
        L1[LinkService.fillPatientAttendedRefs]:::link
        L2[CommonBuilderService.build]:::link
        V1[ValidatorService.validatePatients]:::link
        V2[ValidatorService.validateSessions]:::link
        click L1 "src/main/java/com/aac/kpi/service/LinkService.java#L14" "fillPatientAttendedRefs(...)"
        click L2 "src/main/java/com/aac/kpi/service/CommonBuilderService.java#L16" "build(...)"
        click V1 "src/main/java/com/aac/kpi/service/ValidatorService.java#L14" "validatePatients(...)"
        click V2 "src/main/java/com/aac/kpi/service/ValidatorService.java#L43" "validateSessions(...)"
    end

    subgraph Util[Utilities]
        T1[RandomDataUtil.uuid32]:::link
        T2[RandomDataUtil.randomEventDateTimeFY2025_26AndDuration]:::link
        T3[ExcelWriter.nowIsoOffset]:::link
        click T1 "src/main/java/com/aac/kpi/service/RandomDataUtil.java#L31" "uuid32()"
        click T2 "src/main/java/com/aac/kpi/service/RandomDataUtil.java#L101" "randomEventDateTimeFY2025_26AndDuration()"
        click T3 "src/main/java/com/aac/kpi/service/ExcelWriter.java#L1067" "nowIsoOffset(...)"
    end

    A1 --> A2
    A2 --> UI
    UI --> Logic
    Logic --> IO
    Logic --> Util
```

Method Index (clickable list)
- Main app
  - Main.start: src/main/java/com/aac/kpi/Main.java#L11
  - MainController.initialize: src/main/java/com/aac/kpi/MainController.java#L49

- Editors (tabs)
  - PatientMasterController.onGenerate: src/main/java/com/aac/kpi/controller/PatientMasterController.java#L118
  - EventSessionController.onGenerateSessions: src/main/java/com/aac/kpi/controller/EventSessionController.java#L120
  - EncounterMasterController.onGenerate: src/main/java/com/aac/kpi/controller/EncounterMasterController.java#L79
  - QuestionnaireResponseMasterController.onGenerate: src/main/java/com/aac/kpi/controller/QuestionnaireResponseMasterController.java#L78
  - CommonController.onBuild: src/main/java/com/aac/kpi/controller/CommonController.java#L75

- Excel I/O
  - ExcelReader.readPatients: src/main/java/com/aac/kpi/service/ExcelReader.java#L17
  - ExcelReader.readEventSessions: src/main/java/com/aac/kpi/service/ExcelReader.java#L148
  - ExcelReader.readEncounters: src/main/java/com/aac/kpi/service/ExcelReader.java#L60
  - ExcelReader.readQuestionnaires: src/main/java/com/aac/kpi/service/ExcelReader.java#L86
  - ExcelWriter.saveToExcel: src/main/java/com/aac/kpi/service/ExcelWriter.java#L47
  - ExcelWriter.writeCombinedCommonSheet: src/main/java/com/aac/kpi/service/ExcelWriter.java#L314
  - ExcelWriter.writeResidentReportSection: src/main/java/com/aac/kpi/service/ExcelWriter.java#L422

- Linking / Build / Validation
  - LinkService.fillPatientAttendedRefs: src/main/java/com/aac/kpi/service/LinkService.java#L14
  - CommonBuilderService.build: src/main/java/com/aac/kpi/service/CommonBuilderService.java#L16
  - ValidatorService.validatePatients: src/main/java/com/aac/kpi/service/ValidatorService.java#L14
  - ValidatorService.validateSessions: src/main/java/com/aac/kpi/service/ValidatorService.java#L43

- Utilities
  - RandomDataUtil.uuid32: src/main/java/com/aac/kpi/service/RandomDataUtil.java#L31
  - RandomDataUtil.randomEventDateTimeFY2025_26AndDuration: src/main/java/com/aac/kpi/service/RandomDataUtil.java#L101
  - ExcelWriter.nowIsoOffset: src/main/java/com/aac/kpi/service/ExcelWriter.java#L1067

Coding Strategy (high‑level)
- Data is generated or loaded per tab, then normalized and linked.
- Linking is resilient to identifiers like Patient/<id> vs <id>.
- Resident report requires ≥1 encounter, picks latest completed questionnaire, and derives programme periods from sessions.
- Excel writing enforces column order and formatting (e.g., date cells for yyyy‑MM‑dd, ISO timestamps where applicable).
- Export creates a combined Common sheet followed by individual master/report sheets.

Tips
- Use the editors’ Analyze actions to validate data before export.
- In IntelliJ, Ctrl/Cmd‑Click links above to jump to code. If Mermaid links are not clickable in your viewer, use the Method Index list.

Summary (what it does and how it links)
- What this app does
  - A desktop JavaFX app to make KPI spreadsheets quickly: generate sample data, edit it in tables, validate it, link records across sheets, and export to a clean Excel file.
  - Data types covered: Patients, Event Sessions, Practitioners, Encounters, Questionnaire Responses, plus a derived “Common” section with resident_report and related summaries.

- How the UI is wired
  - Main starts the JavaFX app, shows a short splash, then loads the main window with tabs.
  - MainController loads one controller per tab and gives them shared lists (one list per data type). All tabs see the same in‑memory data and stay in sync.
  - Each tab lets you generate data, paste/edit cells, analyze for basic issues, and export.

- Core models (simple data holders)
  - Patient: id, identifier, birthdate, postal code, AAC, KPI fields (CFS, risk, program dates, computed KPI type/group).
  - EventSession: composition id, event id/mode, start/end/duration, venue, capacity, patient reference, attended flag, purpose of contact.
  - Practitioner: id, identifier/system, position, name, capacity, age, remarks.
  - Encounter: id, status, start, purpose, contacted staff, referred by, patient reference.
  - QuestionnaireResponse: id, status, Q1..Q10 (mix of dates and 1–5 ratings), patient reference.
  - CommonRow: one row per resident for resident_report, plus metadata (version, month, author, references) and extended fields used in reports.

- Key services (logic layer)
  - ExcelReader: reads .xlsx sheets into model lists; tolerant to minor sheet/column name differences where needed.
  - ExcelWriter: writes the export workbook. It creates:
    - A combined Common sheet with sections: aac_report, resident_report, volunteer_attendance_report, event_report, organization_report, and location_report (each with a colored title row, headers, and data rows).
    - Master sheets: Event Sessions, Patient (Master), Practitioner (Master), Encounter (Master), QuestionnaireResponse (Master) when data exists.
    - Applies consistent formatting (dates, date‑times with +08:00, column order, autosizing) and derives helpful aggregates (e.g., per‑event attendees, counts).
  - LinkService: fills Patient.attended_event_references by looking at sessions for that patient (sanitizes ids so “Patient/<id>” and “<id>” both match).
  - CommonBuilderService: builds resident_report rows (CommonRow) by joining Patients ↔ Encounters ↔ Questionnaires ↔ Sessions. It:
    - Requires at least one Encounter reference (prefers finished + valid purposes; falls back to any if none match).
    - Picks the latest completed Questionnaire per patient.
    - Derives programme period start/end from earliest/ latest attended session dates.
    - Fills extended fields from Patient (CFS, risk, recommendations) and adds sensible defaults when blank.
  - ValidatorService: basic checks (e.g., patient id uniqueness, age ≥ 60, postal code; session ids unique, patient references exist).
  - KpiService: computes each patient’s KPI Type/Group for a financial year (FY) using thresholds from KpiConfig (editable in “KPI Settings”).
  - RandomDataUtil: helpers to create realistic random ids, dates, times, venues, and phone/postal codes.
  - NRICGeneratorUtil (+ NricMode): creates NRIC‑like identifiers; tries an online generator via headless Chrome when allowed, otherwise uses a reliable offline checksum‑based fallback.
  - PractitionerMasterGenerator: optional utility to generate a standalone Practitioner_Master.xlsx with realistic volunteers.
  - AppState + KpiConfig: keeps current file path, a dirty flag, the active KPI thresholds, and the chosen number of practitioners to include in the volunteer_attendance_report section.

- How data links end‑to‑end
  - All tabs share the same backing lists, so generating in one tab is visible everywhere.
  - Sessions link to Patients via event_session_patient_references1; Encounters and Questionnaires link via their patient_reference fields.
  - LinkService back‑fills a readable list of a patient’s attended session ids into the Patient table for convenience.
  - CommonBuilderService uses those link fields (with id sanitization) to join records and construct resident_report rows.
  - Export uses the current lists (and built Common rows) to write a single Excel file with “Common” sections and master tabs; the volunteer_attendance_report section respects the number you enter when prompted.

- Typical click flow (quick mental model)
  - Patient tab: generate patients (optionally choose NRIC mode), analyze to compute KPI types, adjust as needed.
  - Event Session tab: generate many sessions per resident for a chosen KPI type, or load from Excel; analyze for basic issues.
  - Practitioner / Encounter / Questionnaire tabs: generate or load, tweak, analyze.
  - Common tab: click Build to create resident_report rows from everything above; analyze; Export to write Excel.
