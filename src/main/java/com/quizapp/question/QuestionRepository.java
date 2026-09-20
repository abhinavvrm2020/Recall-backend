package com.quizapp.question;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findBySubjectId(Long subjectId);

    @Query("select q from Question q where q.id in :ids")
    List<Question> findAllByIdIn(Collection<Long> ids);

    long countBySubjectId(Long subjectId);
}
