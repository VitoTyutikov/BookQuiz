package org.vito.server.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vito.server.entity.AnswerEntity;

import java.util.Optional;

@Repository
public interface AnswerRepository extends JpaRepository<AnswerEntity, Long> {

    Optional<AnswerEntity> findByAnswerText(String answerText);

//    Optional<AnswerEntity> findByQuestionId(Long questionId);

    Optional<AnswerEntity> findByAnswerId(Long answerId);

}
