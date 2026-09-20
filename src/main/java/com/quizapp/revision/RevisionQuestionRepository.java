package com.quizapp.revision;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RevisionQuestionRepository extends JpaRepository<RevisionQuestion, Long> {

    List<RevisionQuestion> findByRevisionId(Long revisionId);

    Optional<RevisionQuestion> findByRevisionIdAndQuestionId(Long revisionId, Long questionId);

    @Query("""
        select rq from RevisionQuestion rq
        where rq.revisionId = :revisionId
          and rq.remainingReviews > 0
          and rq.nextReviewAt <= :now
        order by rq.nextReviewAt
        """)
    List<RevisionQuestion> findDue(Long revisionId, Instant now);

    long countByRevisionIdAndRemainingReviewsGreaterThan(Long revisionId, int remaining);
}
