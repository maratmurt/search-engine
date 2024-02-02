package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    @Transactional
    void deleteAllBySite(SiteEntity site);

    @Transactional
    void deleteBySiteAndPath(SiteEntity site, String path);

    int countBySite(SiteEntity site);

    Optional<PageEntity> findBySiteAndPath(SiteEntity site, String path);
}
