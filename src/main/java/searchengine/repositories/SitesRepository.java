package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Site;

import java.util.Optional;

@Repository
public interface SitesRepository extends JpaRepository<Site, Integer> {
    Optional<Site> findByUrl(String url);
}
