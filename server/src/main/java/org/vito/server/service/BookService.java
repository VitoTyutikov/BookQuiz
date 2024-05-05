package org.vito.server.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vito.server.dto.GenerationResponseDTO;
import org.vito.server.entity.Answer;
import org.vito.server.entity.Book;
import org.vito.server.entity.Chapter;
import org.vito.server.entity.Question;
import org.vito.server.repo.AnswerRepository;
import org.vito.server.repo.BookRepository;
import org.vito.server.repo.ChapterRepository;
import org.vito.server.repo.QuestionRepository;

@Service
public class BookService {
    private final AnswerRepository answerRepository;
    private final BookRepository bookRepository;
    private final ChapterRepository chapterRepository;
    private final QuestionRepository questionRepository;

    public BookService(AnswerRepository answerRepository, BookRepository bookRepository, ChapterRepository chapterRepository, QuestionRepository questionRepository) {
        this.answerRepository = answerRepository;
        this.bookRepository = bookRepository;
        this.chapterRepository = chapterRepository;
        this.questionRepository = questionRepository;
    }

    public Book createBook(String bookTitle) {//TODO: Maybe need change. Or from kafka just add
        var book = new Book();
        book.setBookTitle(bookTitle);
        return bookRepository.save(book);
    }

    public Book getBookById(Long bookId) {
        return bookRepository.findById(bookId).orElse(null);
    }

    public Book getBookByTitle(String bookTitle) {
        return bookRepository.findByBookTitle(bookTitle).orElse(null);
    }


    // function thich called on every message from kafka in topic generation_progress
    @Transactional
    public void addChapterQuestions(GenerationResponseDTO generationResponseDTO) {
        Book book = bookRepository.findById(generationResponseDTO.bookId())
                .orElseThrow(() -> new RuntimeException("Book not found"));

        Chapter chapter = new Chapter();
        chapter.setChapterTitle(generationResponseDTO.title());
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


}
