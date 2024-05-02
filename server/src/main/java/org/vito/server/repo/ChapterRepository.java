package org.vito.server.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.vito.server.entity.ChapterEntity;

import java.util.Optional;

public interface ChapterRepository extends JpaRepository<ChapterEntity, Long> {

    Optional<ChapterEntity> findByChapterId(Long chapterId);
    Optional<ChapterEntity> findByChapterTitle(String chapterTitle);
}
