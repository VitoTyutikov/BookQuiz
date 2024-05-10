package org.vito.server.booksplit.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PythonUploadResponse {
    private String message;
    private String filename;
    private String error;

}
