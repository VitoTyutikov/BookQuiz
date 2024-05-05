package org.vito.server.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vito.server.entity.Answer;

import java.util.Optional;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    Optional<Answer> findByAnswerText(String answerText);

//    Optional<Answer> findByQuestionId(Long questionId);

    Optional<Answer> findByAnswerId(Long answerId);

}
