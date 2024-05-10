package org.vito.server.booksplit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;


// Here will be one title and some questions
public record GenerationResponseDTO(
        @JsonProperty("book_id")
        Long bookId,
        String title,
        @JsonProperty("start_page")
        Integer startPage,
        List<QuestionDTO> questions

) {
}
