package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteModel;

@Repository
public interface SiteRepository extends JpaRepository<SiteModel, Integer> {
    @Query(value = "SELECT * FROM site WHERE url=:rootUrl", nativeQuery = true)
    SiteModel findByUrl(@Param("rootUrl") String rootUrl);
}
