package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    @Transactional
    void deleteAllBySite(SiteEntity site);

    Optional<LemmaEntity> findByLemmaAndSite_Id(String lemmaWord, int siteId);

    int countBySite(SiteEntity site);

    Optional<List<LemmaEntity>> findAllByLemma(String lemmaWord);
}
