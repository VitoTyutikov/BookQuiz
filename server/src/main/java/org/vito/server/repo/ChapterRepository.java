package org.vito.server.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.vito.server.entity.Chapter;

import java.util.Optional;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    Optional<Chapter> findByChapterId(Long chapterId);
    Optional<Chapter> findByChapterTitle(String chapterTitle);
}
