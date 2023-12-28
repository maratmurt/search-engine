package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    void deleteAllBySite(SiteEntity site);

    LemmaEntity findByLemmaAndSite_Id(String word, int siteId);

    LemmaEntity findByLemma(String word);
}
