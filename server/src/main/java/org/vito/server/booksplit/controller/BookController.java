package org.vito.server.booksplit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.vito.server.auth.service.UserService;
import org.vito.server.booksplit.dto.PythonUploadResponse;
import org.vito.server.booksplit.dto.RequestQuestionsFileDTO;
import org.vito.server.booksplit.dto.UploadResponse;
import org.vito.server.booksplit.entity.Book;
import org.vito.server.booksplit.kafka.KafkaProducer;
import org.vito.server.booksplit.service.BookService;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/books")
public class BookController {
    private final String pythonUploadURL;

    private final BookService bookService;

    private final KafkaProducer kafkaProducer;

    private final UserService userService;

    private final RestTemplate restTemplate;

    public String getCurrentUserLogin() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public BookController(BookService bookService,
            UserService userService,
            @Value("${server.python-api.url}") String pythonUploadURL,
            KafkaProducer kafkaProducer,
            RestTemplate restTemplate) {
        this.kafkaProducer = kafkaProducer;
        this.userService = userService;
        this.pythonUploadURL = pythonUploadURL;
        this.bookService = bookService;
        this.restTemplate = restTemplate;
    }

    @RequestMapping(value = "/book/file/upload", method = RequestMethod.POST)
    public ResponseEntity<UploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        var currentUser = userService.findByUsername(getCurrentUserLogin());
        if (!Objects.equals(file.getContentType(), "application/pdf")) {
            return ResponseEntity.badRequest().body(
                    new UploadResponse(null, null, "Only pdf files are allowed"));
        }
        if (file.getSize() > 105 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(
                    new UploadResponse(null, null, "File is too big"));
        }
        // if book with this name exists: return that book
        var bookInRepo = bookService.findBookByTitle(file.getOriginalFilename());
        if (bookInRepo != null) {
            currentUser.getBooks().add(bookInRepo);
            userService.save(currentUser);
            return ResponseEntity.ok(
                    new UploadResponse(bookInRepo.getBookId(), file.getOriginalFilename(), null));
        }
        // else create
        Book savedBook = null;
        try {
            var book = new Book();
            book.setBookTitle(file.getOriginalFilename());
            savedBook = bookService.save(book);

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
            ResponseEntity<PythonUploadResponse> response = restTemplate.postForEntity(
                    pythonUploadURL + "/file/upload", requestEntity, PythonUploadResponse.class);
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new Exception(Objects.requireNonNull(response.getBody()).getError());
            }
            currentUser.getBooks().add(savedBook);
            userService.save(currentUser);
            return ResponseEntity.ok(
                    new UploadResponse(savedBook.getBookId(), file.getOriginalFilename(), null));
        } catch (Exception e) {
            bookService.deleteBookById(savedBook.getBookId());
            return ResponseEntity.internalServerError().body(
                    new UploadResponse(null, null, e.getMessage()));
        }
    }

    @RequestMapping(value = "/book/generate", consumes = "text/plain", method = RequestMethod.POST)
    public void generate(@RequestBody String nameWithId) {
        // System.out.println(nameWithId);
        // var splitBook = Arrays.stream(nameWithId.split("_")).toList();
        long bookId = Long.parseLong(nameWithId.split("_")[0]);
        var book = bookService.findBookById(bookId);
        if (!book.getChapters().isEmpty()) {
            return;
        }
        kafkaProducer.sendMessage(nameWithId);
        System.out.println("Generation request sent for: " + bookId);
    }

    @RequestMapping(value = "/book/{id}", method = RequestMethod.GET)
    public ResponseEntity<Book> getQuestions(@PathVariable("id") Long bookId) {
        var book = bookService.findBookById(bookId);
        return ResponseEntity.ok().body(book);
    }

    @RequestMapping(value = "/book/getfile/word/{id}",
     method = RequestMethod.GET, 
     produces = "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    public ResponseEntity<ByteArrayResource> getQuestionsFile(@PathVariable("id") Long bookId) {
        try {
            Book book = bookService.findBookById(bookId);
            ObjectMapper mapper = new ObjectMapper();
            String bookJson = mapper.writeValueAsString(book);
            RequestQuestionsFileDTO requestQuestionsFileDTO = new RequestQuestionsFileDTO();
            requestQuestionsFileDTO.setBook(bookJson);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<RequestQuestionsFileDTO> requestEntity = new HttpEntity<>(requestQuestionsFileDTO, headers);
            // System.out.println(requestEntity.getBody());
            ResponseEntity<byte[]> response = restTemplate.exchange(pythonUploadURL + "/getfile/word", HttpMethod.POST,
                    requestEntity, byte[].class);

            if (response.getStatusCode() == HttpStatus.OK) {
                ByteArrayResource resource = new ByteArrayResource(Objects.requireNonNull(response.getBody()));
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                        .body(resource);
            } else {
                return ResponseEntity.status(response.getStatusCode()).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @RequestMapping(value = "/all", method = RequestMethod.GET)
    public ResponseEntity<List<Book>> getAll() {
        return ResponseEntity.ok(bookService.findAll());
    }

    // TODO: разделить этот метода на два: 1 - добавление книги к кпользователю по
    // id книги;
    // 2 - получение pdf файла книги по id книги, который закэшировать
    @RequestMapping(value = "/book/add-to-user/{id}", method = RequestMethod.PATCH)
    public ResponseEntity<byte[]> addToUser(@PathVariable("id") Long bookId) {
        var currentUser = userService.findByUsername(getCurrentUserLogin());
        currentUser.getBooks().add(bookService.findBookById(bookId));
        userService.save(currentUser);

        String pythonServiceUrl = pythonUploadURL + "/book/file/" + bookId;
        ResponseEntity<byte[]> response = restTemplate.getForEntity(pythonServiceUrl, byte[].class);

        if (response.getStatusCode() == HttpStatus.OK) {
            HttpHeaders headers = response.getHeaders();
            String contentDisposition = headers.getFirst(HttpHeaders.CONTENT_DISPOSITION);
            String fileName = bookId + "_book" + ".pdf"; // Default filename

            if (contentDisposition != null && contentDisposition.contains("filename*=")) {
                fileName = contentDisposition.split("filename\\*=")[1].split("''")[1];
            }

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            responseHeaders.setContentDispositionFormData("attachment", fileName);
            return new ResponseEntity<>(response.getBody(), responseHeaders, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

}
//