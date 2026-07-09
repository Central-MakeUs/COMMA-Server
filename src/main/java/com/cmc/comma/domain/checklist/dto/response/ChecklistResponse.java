package com.cmc.comma.domain.checklist.dto.response;

import java.util.List;

public record ChecklistResponse(List<Question> questions) {

    public record Question(int order, String title, List<Option> options) {}

    public record Option(String code, String label, String description) {}
}