package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {
    IndexEntity findByLemmaAndPage(LemmaEntity lemma, PageEntity page);

    //TODO delete all indices by site id
    @Query("delete from IndexEntity i where i.page.site.id = :siteId")
    void deleteAllBySiteId(int siteId);
}
