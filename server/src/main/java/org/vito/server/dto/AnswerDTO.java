package org.vito.server.dto;


import com.fasterxml.jackson.annotation.JsonProperty;

//@AllArgsConstructor
//@NoArgsConstructor
//@Getter
//@Setter
public record AnswerDTO(
        String answer,

        @JsonProperty("is_correct")
        Boolean isCorrect
) {
}
