package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {
    @Transactional
    void deleteAllByPage_Site(SiteEntity site);

    List<IndexEntity> findAllByPage_Path(String path);

    @Query("SELECT i.page FROM IndexEntity i WHERE i.lemma.lemma = :lemma")
    List<PageEntity> findAllPagesByLemmaWord(@Param ("lemma") String lemmaWord);

    @Query("SELECT i.rank FROM IndexEntity i WHERE i.page.id = :pageId AND i.lemma.lemma = :lemmaWord")
    double getRankByPageIdAndLemmaWord(@Param("pageId") int pageId, @Param("lemmaWord") String lemmaWord);

    List<IndexEntity> findAllByPage(PageEntity page);
}
