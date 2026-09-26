package com.quizapp.revision;

import com.quizapp.question.Question;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RevisionAlgorithm {

    public static final String CONFIDENCE_SURE = "SURE";
    public static final String CONFIDENCE_GUESS = "GUESS";
    public static final String CONFIDENCE_UNSURE = "UNSURE";

    public record Candidate(Long questionId, String reason, int remainingReviews, Instant nextReviewAt) {}

    public record ScoredAnswer(Long questionId, boolean correct, String confidence) {}

    public List<Candidate> candidates(List<ScoredAnswer> answers, Map<Long, Question> questionsById) {
        List<Candidate> out = new ArrayList<>();
        Instant now = Instant.now();
        for (ScoredAnswer a : answers) {
            if (questionsById.get(a.questionId()) == null) {
                continue;
            }
            String confidence = normalizeConfidence(a.confidence());
            if (a.correct() && CONFIDENCE_SURE.equals(confidence)) {
                continue;
            }
            if (!a.correct() && CONFIDENCE_SURE.equals(confidence)) {
                out.add(new Candidate(a.questionId(), "SURE_WRONG", 3, now));
            } else if (!a.correct()) {
                out.add(new Candidate(
                        a.questionId(), wrongReason(confidence), 2, now));
            } else {
                out.add(new Candidate(
                        a.questionId(), rightReason(confidence), 2, now));
            }
        }
        return out;
    }

    public void applyRevisionOutcome(RevisionQuestion rq, boolean correct, String confidence) {
        String normalized = normalizeConfidence(confidence);
        Instant now = Instant.now();
        if (correct && CONFIDENCE_SURE.equals(normalized)) {
            rq.setRemainingReviews(Math.max(0, rq.getRemainingReviews() - 1));
            rq.setNextReviewAt(now.plus(3, ChronoUnit.DAYS));
            return;
        }
        if (!correct && CONFIDENCE_SURE.equals(normalized)) {
            rq.setReason("SURE_WRONG");
            rq.setRemainingReviews(3);
            rq.setNextReviewAt(now.plus(1, ChronoUnit.DAYS));
            return;
        }
        if (!correct) {
            rq.setReason(wrongReason(normalized));
            rq.setRemainingReviews(2);
            rq.setNextReviewAt(now.plus(1, ChronoUnit.DAYS));
            return;
        }
        rq.setReason(rightReason(normalized));
        int remaining = Math.max(0, rq.getRemainingReviews() - 1);
        rq.setRemainingReviews(remaining);
        if (remaining > 0) {
            rq.setNextReviewAt(now.plus(2, ChronoUnit.DAYS));
        }
    }

    public static String normalizeConfidence(String confidence) {
        if (confidence == null || confidence.isBlank()) {
            return CONFIDENCE_UNSURE;
        }
        return confidence.trim().toUpperCase();
    }

    public static boolean shouldReplaceCandidate(
            String existingReason, int existingRemaining, String candidateReason, int candidateRemaining) {
        int existingPriority = reasonPriority(existingReason);
        int candidatePriority = reasonPriority(candidateReason);
        if (candidatePriority > existingPriority) {
            return true;
        }
        if (candidatePriority < existingPriority) {
            return false;
        }
        return candidateRemaining > existingRemaining;
    }

    public static int reasonPriority(String reason) {
        if (reason == null) {
            return 0;
        }
        return switch (reason) {
            case "SURE_WRONG" -> 4;
            case "GUESS_WRONG", "UNSURE_WRONG", "WRONG" -> 3;
            case "GUESS_RIGHT", "UNSURE_RIGHT" -> 2;
            case "SLOW" -> 1;
            default -> 0;
        };
    }

    private static String wrongReason(String confidence) {
        return CONFIDENCE_GUESS.equals(confidence) ? "GUESS_WRONG" : "UNSURE_WRONG";
    }

    private static String rightReason(String confidence) {
        return CONFIDENCE_GUESS.equals(confidence) ? "GUESS_RIGHT" : "UNSURE_RIGHT";
    }
}
