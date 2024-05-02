package org.vito.server.dto;


public record GenerationProgressDTO(
        Integer currentTitle,
        Integer totalTitles
) {
}
