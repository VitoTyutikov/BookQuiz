package org.vito.server.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.vito.server.entity.QuestionEntity;

import java.util.Optional;

public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {
    Optional<QuestionEntity> findByQuestionId(Long questionId);

    Optional<QuestionEntity> findByQuestionText(String questionText);
}
