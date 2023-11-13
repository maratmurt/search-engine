package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.SiteEntity;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    @Query(value = "select p from Page p where p.site = ?1")
    Iterable<Page> findBySite(SiteEntity site);

    boolean existsBySiteAndPath(SiteEntity site, String path);
}
