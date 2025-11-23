package com.aac.kpi.controller;

import com.aac.kpi.model.CommonRow;
import com.aac.kpi.model.Encounter;
import com.aac.kpi.model.EventSession;
import com.aac.kpi.model.Patient;
import com.aac.kpi.model.QuestionnaireResponse;
import com.aac.kpi.service.AgentService;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * Optional Assist (RAG) tab. Fully read-only: does not mutate the workbook.
 * Works offline by using a lightweight, local retriever over the loaded lists.
 */
public class AgentAssistController {
    @FXML private TextArea promptArea;
    @FXML private Button runButton;
    @FXML private TextArea traceArea;
    @FXML private TextArea answerArea;
    @FXML private Label statusLabel;

    private AgentService agentService;

    public void init(ObservableList<Patient> patients,
                     ObservableList<EventSession> sessions,
                     ObservableList<Encounter> encounters,
                     ObservableList<QuestionnaireResponse> questionnaires,
                     ObservableList<CommonRow> commons) {
        this.agentService = new AgentService(patients, sessions, encounters, questionnaires, commons);
        if (statusLabel != null) {
            statusLabel.setText("Offline assistant: reads current workbook; no network/LLM required.");
        }
    }

    @FXML
    private void onRun() {
        String prompt = promptArea != null && promptArea.getText() != null ? promptArea.getText().trim() : "";
        if (prompt.isEmpty()) {
            if (answerArea != null) answerArea.setText("Please enter a question or instruction.");
            return;
        }
        AgentService.Response resp = agentService.answer(prompt);
        if (answerArea != null) answerArea.setText(resp.answer());
        if (traceArea != null) traceArea.setText(resp.trace());
    }

    @FXML
    private void onClear() {
        if (promptArea != null) promptArea.clear();
        if (traceArea != null) traceArea.clear();
        if (answerArea != null) answerArea.clear();
    }
}
