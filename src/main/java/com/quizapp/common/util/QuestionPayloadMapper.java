package com.quizapp.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizapp.common.ApiException;
import com.quizapp.question.Question;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class QuestionPayloadMapper {

    private final ObjectMapper objectMapper;

    public QuestionPayloadMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String text(Question question) {
        return node(question).path("question").asText("");
    }

    public Map<String, String> options(Question question) {
        Map<String, String> options = new LinkedHashMap<>();
        node(question).path("options").fields()
                .forEachRemaining(e -> options.put(e.getKey(), e.getValue().asText()));
        return options;
    }

    public boolean isCorrect(Question question, String selectedOption) {
        if (selectedOption == null) {
            return false;
        }
        return question.getCorrectOption().equalsIgnoreCase(selectedOption.trim());
    }

    public boolean isSlow(Question question, int timeTakenMs) {
        return timeTakenMs > (int) (question.getAllottedTimeMs() * 0.75);
    }

    private JsonNode node(Question question) {
        try {
            return objectMapper.readTree(question.getQuestionJson());
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Corrupt question " + question.getId());
        }
    }
}
