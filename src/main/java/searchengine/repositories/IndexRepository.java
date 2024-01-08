package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {
    @Transactional
    void deleteAllByPage_Site(SiteEntity site);

    @Transactional
    void deleteAllByPage_SiteAndPage_Path(SiteEntity site, String path);

    List<IndexEntity> findAllByPage_Path(String path);

    @Query("SELECT i.page.id FROM IndexEntity i WHERE i.lemma.lemma = :lemma")
    List<Integer> findAllPageIdsByLemmaWord(@Param("lemma") String lemmaWord);

    @Query("SELECT i.page.id FROM IndexEntity i WHERE i.lemma.lemma = :lemma AND i.page.site.id = :siteId")
    List<Integer> findAllPageIdsByLemmaWordAndSiteId(@Param("lemma") String lemmaWord, @Param("siteId") int siteId);

    @Query("SELECT i.page FROM IndexEntity i WHERE i.lemma.lemma = :lemma")
    List<PageEntity> findAllPagesByLemmaWord(@Param ("lemma") String lemmaWord);
}
