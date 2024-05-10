package org.vito.server.booksplit.dto;

import java.util.List;


public record QuestionDTO(
        String question,
        List<AnswerDTO> answers

) {
}
