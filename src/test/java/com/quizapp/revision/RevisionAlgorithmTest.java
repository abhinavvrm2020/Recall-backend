package com.quizapp.revision;

import static org.junit.jupiter.api.Assertions.*;

import com.quizapp.question.Question;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RevisionAlgorithmTest {

    private final RevisionAlgorithm algorithm = new RevisionAlgorithm();

    @Test
    void wrongAndSlowBecomeCandidates() {
        Question q1 = question(1L, 60_000);
        Question q2 = question(2L, 60_000);
        Question q3 = question(3L, 60_000);

        var wrong = new RevisionAlgorithm.ScoredAnswer(1L, false, 10_000);
        var slowCorrect = new RevisionAlgorithm.ScoredAnswer(2L, true, 50_000); // > 75% of 60s
        var fastCorrect = new RevisionAlgorithm.ScoredAnswer(3L, true, 10_000);

        var candidates = algorithm.candidates(
                List.of(wrong, slowCorrect, fastCorrect),
                Map.of(1L, q1, 2L, q2, 3L, q3));

        assertEquals(2, candidates.size());
        assertEquals("WRONG", candidates.get(0).reason());
        assertEquals(3, candidates.get(0).remainingReviews());
        assertEquals("SLOW", candidates.get(1).reason());
        assertEquals(2, candidates.get(1).remainingReviews());
    }

    private static Question question(Long id, int allotted) {
        Question q = new Question();
        // id is generated; set via reflection-free stub using map keys only — allotted matters
        q.setAllottedTimeMs(allotted);
        q.setCorrectOption("a");
        q.setSubjectId(1L);
        q.setQuestionJson("{}");
        return q;
    }
}
