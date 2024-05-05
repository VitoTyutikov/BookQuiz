package org.vito.server.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.vito.server.entity.Question;

import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    Optional<Question> findByQuestionId(Long questionId);

    Optional<Question> findByQuestionText(String questionText);
}
