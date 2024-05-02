package org.vito.server.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.vito.server.entity.BookEntity;

import java.util.Optional;

public interface BookRepository extends JpaRepository<BookEntity, Long> {
    Optional<BookEntity> findByBookTitle(String bookTitle);
    Optional<BookEntity> findByBookId(Long bookId);
}
