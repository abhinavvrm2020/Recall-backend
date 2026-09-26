package com.quizapp.revision;

import static org.junit.jupiter.api.Assertions.*;

import com.quizapp.question.Question;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RevisionAlgorithmTest {

    private final RevisionAlgorithm algorithm = new RevisionAlgorithm();

    @Test
    void candidatesFollowConfidenceTable() {
        Question q = question(1L);

        var sureWrong = new RevisionAlgorithm.ScoredAnswer(1L, false, "SURE");
        var guessRight = new RevisionAlgorithm.ScoredAnswer(2L, true, "GUESS");
        var sureRight = new RevisionAlgorithm.ScoredAnswer(3L, true, "SURE");
        var nullConfidenceWrong = new RevisionAlgorithm.ScoredAnswer(4L, false, null);

        var candidates = algorithm.candidates(
                List.of(sureWrong, guessRight, sureRight, nullConfidenceWrong),
                Map.of(1L, q, 2L, q, 3L, q, 4L, q));

        assertEquals(3, candidates.size());
        assertEquals("SURE_WRONG", candidates.get(0).reason());
        assertEquals(3, candidates.get(0).remainingReviews());
        assertEquals("GUESS_RIGHT", candidates.get(1).reason());
        assertEquals(2, candidates.get(1).remainingReviews());
        assertEquals("UNSURE_WRONG", candidates.get(2).reason());
        assertEquals(2, candidates.get(2).remainingReviews());
    }

    @Test
    void applyRevisionOutcomeSureRightDecrementsRemaining() {
        RevisionQuestion rq = new RevisionQuestion();
        rq.setRemainingReviews(2);
        Instant before = Instant.parse("2026-01-01T00:00:00Z");
        rq.setNextReviewAt(before);

        algorithm.applyRevisionOutcome(rq, true, "SURE");

        assertEquals(1, rq.getRemainingReviews());
        assertTrue(rq.getNextReviewAt().isAfter(before));
    }

    @Test
    void applyRevisionOutcomeWeakRightLeavesNextReviewWhenRemainingZero() {
        RevisionQuestion rq = new RevisionQuestion();
        rq.setRemainingReviews(1);
        Instant before = Instant.parse("2026-01-01T00:00:00Z");
        rq.setNextReviewAt(before);

        algorithm.applyRevisionOutcome(rq, true, "GUESS");

        assertEquals(0, rq.getRemainingReviews());
        assertEquals(before, rq.getNextReviewAt());
        assertEquals("GUESS_RIGHT", rq.getReason());
    }

    @Test
    void shouldReplaceCandidatePrefersSureWrong() {
        assertTrue(RevisionAlgorithm.shouldReplaceCandidate("GUESS_WRONG", 2, "SURE_WRONG", 3));
        assertFalse(RevisionAlgorithm.shouldReplaceCandidate("SURE_WRONG", 3, "UNSURE_WRONG", 2));
    }

    private static Question question(Long id) {
        Question q = new Question();
        q.setAllottedTimeMs(60_000);
        q.setCorrectOption("a");
        q.setSubjectId(1L);
        q.setQuestionJson("{}");
        return q;
    }
}
