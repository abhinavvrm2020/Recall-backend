package com.quizapp.revision;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevisionRepository extends JpaRepository<Revision, Long> {
    List<Revision> findByUserIdAndStatusInOrderByCreatedAtDesc(Long userId, List<String> statuses);
    Optional<Revision> findFirstByUserIdAndSubjectIdAndStatus(Long userId, Long subjectId, String status);
}
