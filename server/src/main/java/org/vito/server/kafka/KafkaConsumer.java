package org.vito.server.kafka;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.vito.server.dto.GenerationProgressDTO;
import org.vito.server.dto.GenerationResponseDTO;
import org.vito.server.service.BookService;

@Component
@RequiredArgsConstructor
public class KafkaConsumer {

    private final BookService bookService;

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "generation_progress", groupId = "progress")
    public void listenGenerationProgress(String generationProgress) {
        //TODO: send to android
        try {
            GenerationProgressDTO progress = objectMapper.readValue(generationProgress, GenerationProgressDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Received Message in topic generation_progress. Progress:  " + generationProgress);
    }

    @KafkaListener(topics = "generation_response", groupId = "response")
    public void listenGenerationResponse(String generationResponse) {
        //TODO: save to database
        GenerationResponseDTO response;
        try {
            response = objectMapper.readValue(generationResponse, GenerationResponseDTO.class);
            bookService.addChapterQuestions(response);
        } catch (JsonProcessingException e) {
            System.out.println("Failed to parse: \n" + generationResponse + "\n");
            throw new RuntimeException(e);
        }

        System.out.println("Received Message in topic generation_response for book_id = " + response.bookId());
    }


}
