package org.vito.server.booksplit.dto;


public record GenerationProgressDTO(
        Integer currentTitle,
        Integer totalTitles
) {
}
