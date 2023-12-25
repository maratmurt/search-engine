package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    @Query(value = "select p.id from PageEntity p where p.site.id = :siteId")
    public List<Integer> findAllIdsBySiteId(@Param("siteId") int siteId);
    public void deleteById(int id);
}
