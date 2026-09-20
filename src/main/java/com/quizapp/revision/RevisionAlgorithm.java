package com.quizapp.revision;

import com.quizapp.attempt.UserQuizAttemptQuestion;
import com.quizapp.question.Question;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RevisionAlgorithm {

    public record Candidate(Long questionId, String reason, int remainingReviews, Instant nextReviewAt) {}

    public List<Candidate> candidates(
            List<UserQuizAttemptQuestion> answers, Map<Long, Question> questionsById) {
        List<Candidate> out = new ArrayList<>();
        Instant now = Instant.now();
        for (UserQuizAttemptQuestion a : answers) {
            Question q = questionsById.get(a.getQuestionId());
            if (q == null) continue;
            boolean slow = a.getTimeTakenMs() > (int) (q.getAllottedTimeMs() * 0.75);
            if (!a.isCorrect()) {
                out.add(new Candidate(a.getQuestionId(), "WRONG", 3, now));
            } else if (slow) {
                out.add(new Candidate(a.getQuestionId(), "SLOW", 2, now));
            }
        }
        return out;
    }

    public void applyRevisionOutcome(RevisionQuestion rq, boolean correct, boolean slow) {
        Instant now = Instant.now();
        if (!correct) {
            rq.setReason("WRONG");
            rq.setRemainingReviews(3);
            rq.setNextReviewAt(now.plus(1, ChronoUnit.DAYS));
            return;
        }
        if (slow) {
            rq.setReason("SLOW");
            rq.setRemainingReviews(Math.max(rq.getRemainingReviews(), 1));
            rq.setNextReviewAt(now.plus(2, ChronoUnit.DAYS));
            return;
        }
        int left = rq.getRemainingReviews() - 1;
        rq.setRemainingReviews(Math.max(left, 0));
        rq.setNextReviewAt(now.plus(3, ChronoUnit.DAYS));
    }
}
