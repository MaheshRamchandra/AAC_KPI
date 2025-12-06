package com.aac.kpi.converter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.HashMap;

import static com.aac.kpi.converter.DateTime.convertToDate;
import static com.aac.kpi.converter.ValueUtil.toInteger;

public class Questionnaire {
    JsonObject questionnaireObject = new JsonObject();

    String questionnaire_id;
    String questionnaire_status;
    String questionnaire_q1_answer;
    String questionnaire_q2_answer;
    String questionnaire_q3_answer;
    String questionnaire_q4_answer;
    String questionnaire_q5_answer;
    String questionnaire_q6_answer;
    String questionnaire_q7_answer;
    String questionnaire_q8_answer;
    String questionnaire_q9_answer;
    String questionnaire_q10_answer;

    public Questionnaire(HashMap<String, String> questionnaire) {
        this.questionnaire_id = questionnaire.get("questionnaire_id");
        this.questionnaire_status = questionnaire.get("questionnaire_status");
        this.questionnaire_q1_answer = questionnaire.get("questionnaire_q1_answer");
        this.questionnaire_q2_answer = questionnaire.get("questionnaire_q2_answer");
        this.questionnaire_q3_answer = questionnaire.get("questionnaire_q3_answer");
        this.questionnaire_q4_answer = questionnaire.get("questionnaire_q4_answer");
        this.questionnaire_q5_answer = questionnaire.get("questionnaire_q5_answer");
        this.questionnaire_q6_answer = questionnaire.get("questionnaire_q6_answer");
        this.questionnaire_q7_answer = questionnaire.get("questionnaire_q7_answer");
        this.questionnaire_q8_answer = questionnaire.get("questionnaire_q8_answer");
        this.questionnaire_q9_answer = questionnaire.get("questionnaire_q9_answer");
        this.questionnaire_q10_answer = questionnaire.get("questionnaire_q10_answer");
    }

    public JsonObject generateQuestionnaireObject() {
        questionnaireObject.addProperty("resourceType", "QuestionnaireResponse");
        questionnaireObject.addProperty("id", this.questionnaire_id);
        questionnaireObject.addProperty("status", this.questionnaire_status);

        JsonArray itemArray = new JsonArray();

        JsonObject q1Object = new JsonObject();
        q1Object.addProperty("linkId", "Q1");
        q1Object.addProperty("text", "Overall Programme Satisfaction Survey Date");
        JsonArray q1AnswerArray = new JsonArray();
        JsonObject q1AnswerObject = new JsonObject();
        q1AnswerObject.addProperty("valueDate", convertToDate(this.questionnaire_q1_answer));
        q1AnswerArray.add(q1AnswerObject);
        q1Object.add("answer", q1AnswerArray);
        itemArray.add(q1Object);

        JsonObject q2Object = new JsonObject();
        q2Object.addProperty("linkId", "Q2");
        q2Object.addProperty("text", "Overall Programme Satisfaction Survey Score");
        JsonArray q2AnswerArray = new JsonArray();
        JsonObject q2AnswerObject = new JsonObject();
        q2AnswerObject.addProperty("valueInteger", toInteger(this.questionnaire_q2_answer));
        q2AnswerArray.add(q2AnswerObject);
        q2Object.add("answer", q2AnswerArray);
        itemArray.add(q2Object);

        JsonObject q3Object = new JsonObject();
        q3Object.addProperty("linkId", "Q3");
        q3Object.addProperty("text", "AAP Satisfaction Survey Date");
        JsonArray q3AnswerArray = new JsonArray();
        JsonObject q3AnswerObject = new JsonObject();
        q3AnswerObject.addProperty("valueDate", convertToDate(this.questionnaire_q3_answer));
        q3AnswerArray.add(q3AnswerObject);
        q3Object.add("answer", q3AnswerArray);
        itemArray.add(q3Object);

        JsonObject q4Object = new JsonObject();
        q4Object.addProperty("linkId", "Q4");
        q4Object.addProperty("text", "AAP Satisfaction Survey Score");
        JsonArray q4AnswerArray = new JsonArray();
        JsonObject q4AnswerObject = new JsonObject();
        q4AnswerObject.addProperty("valueInteger", toInteger(this.questionnaire_q4_answer));
        q4AnswerArray.add(q4AnswerObject);
        q4Object.add("answer", q4AnswerArray);
        itemArray.add(q4Object);

        JsonObject q5Object = new JsonObject();
        q5Object.addProperty("linkId", "Q5");
        q5Object.addProperty("text", "B & B Satisfaction Survey Date");
        JsonArray q5AnswerArray = new JsonArray();
        JsonObject q5AnswerObject = new JsonObject();
        q5AnswerObject.addProperty("valueDate", convertToDate(this.questionnaire_q5_answer));
        q5AnswerArray.add(q5AnswerObject);
        q5Object.add("answer", q5AnswerArray);
        itemArray.add(q5Object);

        JsonObject q6Object = new JsonObject();
        q6Object.addProperty("linkId", "Q6");
        q6Object.addProperty("text", "B & B Satisfaction Survey Score");
        JsonArray q6AnswerArray = new JsonArray();
        JsonObject q6AnswerObject = new JsonObject();
        q6AnswerObject.addProperty("valueInteger", toInteger(this.questionnaire_q6_answer));
        q6AnswerArray.add(q6AnswerObject);
        q6Object.add("answer", q6AnswerArray);
        itemArray.add(q6Object);

        JsonObject q7Object = new JsonObject();
        q7Object.addProperty("linkId", "Q7");
        q7Object.addProperty("text", "I & R Satisfaction Survey Date");
        JsonArray q7AnswerArray = new JsonArray();
        JsonObject q7AnswerObject = new JsonObject();
        q7AnswerObject.addProperty("valueDate", convertToDate(this.questionnaire_q7_answer));
        q7AnswerArray.add(q7AnswerObject);
        q7Object.add("answer", q7AnswerArray);
        itemArray.add(q7Object);

        JsonObject q8Object = new JsonObject();
        q8Object.addProperty("linkId", "Q8");
        q8Object.addProperty("text", "I & R Satisfaction Survey Score");
        JsonArray q8AnswerArray = new JsonArray();
        JsonObject q8AnswerObject = new JsonObject();
        q8AnswerObject.addProperty("valueInteger", toInteger(this.questionnaire_q8_answer));
        q8AnswerArray.add(q8AnswerObject);
        q8Object.add("answer", q8AnswerArray);
        itemArray.add(q8Object);

        JsonObject q9Object = new JsonObject();
        q9Object.addProperty("linkId", "Q9");
        q9Object.addProperty("text", "2S Satisfaction Survey Date");
        JsonArray q9AnswerArray = new JsonArray();
        JsonObject q9AnswerObject = new JsonObject();
        q9AnswerObject.addProperty("valueDate", convertToDate(this.questionnaire_q9_answer));
        q9AnswerArray.add(q9AnswerObject);
        q9Object.add("answer", q9AnswerArray);
        itemArray.add(q9Object);

        JsonObject q10Object = new JsonObject();
        q10Object.addProperty("linkId", "Q10");
        q10Object.addProperty("text", "2S Satisfaction Survey Score");
        JsonArray q10AnswerArray = new JsonArray();
        JsonObject q10AnswerObject = new JsonObject();
        q10AnswerObject.addProperty("valueInteger", toInteger(this.questionnaire_q10_answer));
        q10AnswerArray.add(q10AnswerObject);
        q10Object.add("answer", q10AnswerArray);
        itemArray.add(q10Object);

        questionnaireObject.add("item", itemArray);

        return questionnaireObject;
    }

}
