package com.cmc.comma.domain.checklist.service;

import com.cmc.comma.domain.checklist.dto.response.ChecklistResponse;
import com.cmc.comma.domain.checklist.dto.response.ChecklistResponse.Option;
import com.cmc.comma.domain.checklist.dto.response.ChecklistResponse.Question;
import com.cmc.comma.domain.checklist.entity.Mood;
import com.cmc.comma.domain.checklist.entity.TimeBudget;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ChecklistService {

    private static final String Q1_TITLE = "지금 기분이 어때요?";
    private static final String Q2_TITLE = "어느정도 시간이 있어요?";

    public ChecklistResponse getChecklist() {
        List<Option> moodOptions = Arrays.stream(Mood.values())
                .map(m -> new Option(m.name(), m.getLabel(), null))
                .toList();
        List<Option> timeOptions = Arrays.stream(TimeBudget.values())
                .map(t -> new Option(t.name(), t.getLabel(), t.getDescription()))
                .toList();

        return new ChecklistResponse(List.of(
                new Question(1, Q1_TITLE, moodOptions),
                new Question(2, Q2_TITLE, timeOptions)
        ));
    }
}