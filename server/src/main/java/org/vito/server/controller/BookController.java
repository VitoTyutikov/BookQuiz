package org.vito.server.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.vito.server.dto.PythonUploadResponse;
import org.vito.server.dto.UploadResponse;
import org.vito.server.entity.Book;
import org.vito.server.kafka.KafkaProducer;
import org.vito.server.service.BookService;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

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
            var book = new Book();
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
            return ResponseEntity.ok(new UploadResponse(savedBook.getBookId(), file.getOriginalFilename(), null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new UploadResponse(null, null, e.getMessage()));
        }
    }

    @RequestMapping(value = "/book/generate",
            consumes = "text/plain",
            method = RequestMethod.POST)
    public void generate(@RequestBody String nameWithId) {
        System.out.println(nameWithId);
        var splitBook = Arrays.stream(nameWithId.split("_")).toList();
        long bookId = Long.parseLong(nameWithId.split("_")[0]);
        var book = bookService.getBookById(bookId);
//        if (book == null) {
//            book = bookService.getBookByTitle(String.join("_", splitBook.subList(1, splitBook.size())));
//            return;
//        }

        if (!book.getChapters().isEmpty()) {
            return;
        }
        kafkaProducer.sendMessage(nameWithId);
        System.out.println("Generation request sent for: " + bookId);
    }


    //TODO: delete it
    @RequestMapping(value = "/book/file/upload/test", method = RequestMethod.POST)
    public ResponseEntity<UploadResponse> uploadFileTest(@RequestParam("file") MultipartFile file) {
        if (!Objects.equals(file.getContentType(), "application/pdf")) {
            return ResponseEntity.badRequest().body(new UploadResponse(null, null, "Only pdf files are allowed"));
        }

        if (file.getSize() > 105 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(new UploadResponse(null, null, "File is too big"));
        }

        return ResponseEntity.ok(new UploadResponse(1L, file.getOriginalFilename(), null));


    }

    //TODO: delete it
    @RequestMapping(value = "/book/generate/test", method = RequestMethod.GET)
    public void generateTest(@RequestParam("name_with_id") String nameWithId) {
//        kafkaProducer.sendMessage(bookId + "_" + filename);
        System.out.println("Generation request sent for: " + nameWithId);
    }


    @RequestMapping(value = "/book/{id}", method = RequestMethod.GET)
    public Book getQuestions(@PathVariable("id") Long bookId) {
        return bookService.getBookById(bookId);
    }


}
