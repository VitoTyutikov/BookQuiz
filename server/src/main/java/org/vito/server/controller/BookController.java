package org.vito.server.controller;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.vito.server.dto.PythonUploadResponse;
import org.vito.server.dto.UploadResponse;
import org.vito.server.entity.BookEntity;
import org.vito.server.kafka.KafkaProducer;
import org.vito.server.service.BookService;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/books")
public class BookController {
    private final String pythonURL;

    private final BookService bookService;

    private final KafkaProducer kafkaProducer;

    public BookController(BookService bookService,
                          @Value("${server.python-api.url}") String pythonURL,
                          KafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
        this.pythonURL = pythonURL;
        this.bookService = bookService;
    }

    @RequestMapping(value = "/book/file/upload", method = RequestMethod.POST)
    public ResponseEntity<UploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        if (!Objects.equals(file.getContentType(), "application/pdf")) {
            return ResponseEntity.badRequest().body(new UploadResponse(null, null, "Only pdf files are allowed"));
        }

        if (file.getSize() > 105 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(new UploadResponse(null, null, "File is too big"));
        }
        try {
            var book = new BookEntity();
            book.setBookTitle(file.getOriginalFilename());
            var savedBook = bookService.save(book);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });
            body.add("bookId", String.valueOf(savedBook.getBookId()));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<PythonUploadResponse> response = restTemplate.postForEntity(pythonURL, requestEntity, PythonUploadResponse.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new Exception(response.getBody().getError());
            }
            return ResponseEntity.ok(new UploadResponse(savedBook.getBookId(), savedBook.getBookTitle(), null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new UploadResponse(null, null, e.getMessage()));
        }
    }

    @RequestMapping(value = "/book/generate", method = RequestMethod.POST)
    public void generate(@RequestParam("bookId") Long bookId, @RequestParam("filename") String filename) {
        kafkaProducer.sendMessage(bookId + "_" + filename);
        System.out.println("Generation request sent for: " + bookId + "_" + filename);
    }

}
