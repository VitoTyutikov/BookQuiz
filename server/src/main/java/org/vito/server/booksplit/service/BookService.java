package org.vito.server.booksplit.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vito.server.booksplit.dto.GenerationResponseDTO;
import org.vito.server.booksplit.entity.Answer;
import org.vito.server.booksplit.entity.Book;
import org.vito.server.booksplit.entity.Chapter;
import org.vito.server.booksplit.entity.Question;
import org.vito.server.booksplit.repo.AnswerRepository;
import org.vito.server.booksplit.repo.BookRepository;
import org.vito.server.booksplit.repo.ChapterRepository;
import org.vito.server.booksplit.repo.QuestionRepository;

import java.util.List;

@Service
public class BookService {
    private final AnswerRepository answerRepository;
    private final BookRepository bookRepository;
    private final ChapterRepository chapterRepository;
    private final QuestionRepository questionRepository;

    public BookService(AnswerRepository answerRepository, BookRepository bookRepository,
            ChapterRepository chapterRepository, QuestionRepository questionRepository) {
        this.answerRepository = answerRepository;
        this.bookRepository = bookRepository;
        this.chapterRepository = chapterRepository;
        this.questionRepository = questionRepository;
    }

    public Book createBook(String bookTitle) {
        var book = new Book();
        book.setBookTitle(bookTitle);
        return bookRepository.save(book);
    }

    public Book findBookById(Long bookId) {
        try {
            return bookRepository.findById(bookId).orElse(null);
        } catch (Exception e) {
            System.out.println("Error finding book by ID: " + bookId + "\n" + e);
            throw e;
        }
    }

    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    public Book findBookByTitle(String bookTitle) {
        return bookRepository.findByBookTitle(bookTitle).orElse(null);
    }

    @Transactional
    public void addChapterQuestions(GenerationResponseDTO generationResponseDTO) {
        Book book = bookRepository.findById(generationResponseDTO.bookId())
                .orElseThrow(() -> new RuntimeException("Book not found"));
        Chapter chapter = new Chapter();
        chapter.setChapterTitle(generationResponseDTO.title());
        if (book.getChapters().isEmpty()) {
            chapter.setStartPage(0);
        } else {
            chapter.setStartPage(generationResponseDTO.startPage());
        }
        chapter.setBook(book);
        chapterRepository.save(chapter);
        generationResponseDTO.questions().forEach(questionDTO -> {
            Question question = new Question();
            question.setQuestionText(questionDTO.question());
            question.setChapter(chapter);
            questionRepository.save(question);

            questionDTO.answers().forEach(answerDTO -> {
                Answer answer = new Answer();
                answer.setAnswerText(answerDTO.answer());
                answer.setIsCorrect(answerDTO.isCorrect());
                answer.setQuestion(question);
                answerRepository.save(answer);
            });
        });
    }

    public Book save(Book book) {
        return bookRepository.save(book);
    }

    public void delete(Book book) {
        bookRepository.delete(book);
    }

    public void deleteBookById(Long bookId) {
        bookRepository.deleteById(bookId);
    }

}
