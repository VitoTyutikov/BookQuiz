package org.vito.server.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vito.server.dto.GenerationResponseDTO;
import org.vito.server.entity.AnswerEntity;
import org.vito.server.entity.BookEntity;
import org.vito.server.entity.ChapterEntity;
import org.vito.server.entity.QuestionEntity;
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

    public BookEntity createBook(String bookTitle) {//TODO: Maybe need change. Or from kafka just add
        var book = new BookEntity();
        book.setBookTitle(bookTitle);
        return bookRepository.save(book);
    }

    public BookEntity getBookById(Long bookId) {
        return bookRepository.findById(bookId).orElse(null);
    }

    public BookEntity getBookByTitle(String bookTitle) {
        return bookRepository.findByBookTitle(bookTitle).orElse(null);
    }


    // function thich called on every message from kafka in topic generation_progress
    @Transactional
    public void addChapterQuestions(GenerationResponseDTO generationResponseDTO) {
        BookEntity book = bookRepository.findById(generationResponseDTO.bookId())
                .orElseThrow(() -> new RuntimeException("Book not found"));

        ChapterEntity chapter = new ChapterEntity();
        chapter.setChapterTitle(generationResponseDTO.title());
        chapter.setBook(book);

        chapterRepository.save(chapter);

        generationResponseDTO.questions().forEach(questionDTO -> {
            QuestionEntity question = new QuestionEntity();
            question.setQuestionText(questionDTO.question());
            question.setChapter(chapter);

            questionRepository.save(question);

            questionDTO.answers().forEach(answerDTO -> {
                AnswerEntity answer = new AnswerEntity();
                answer.setAnswerText(answerDTO.answer());
                answer.setIsCorrect(answerDTO.isCorrect());
                answer.setQuestion(question);

                answerRepository.save(answer);
            });
        });
    }


    public BookEntity save(BookEntity book) {
        return bookRepository.save(book);
    }

}
