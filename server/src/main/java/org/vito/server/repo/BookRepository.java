package org.vito.server.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.vito.server.entity.Book;

import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByBookTitle(String bookTitle);
    Optional<Book> findByBookId(Long bookId);
}
