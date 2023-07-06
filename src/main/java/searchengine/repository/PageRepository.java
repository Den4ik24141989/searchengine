package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.PageModel;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<PageModel, Integer> {
    @Query(value = "SELECT * FROM page WHERE path=:path", nativeQuery = true)
    PageModel findByPath(@Param("path") String path);

    @Query(value = "SELECT * FROM page WHERE site_id=:site_id", nativeQuery = true)
    List <PageModel> findAllBySiteId(@Param("site_id") int siteId);
    @Query(value = "SELECT COUNT(*) FROM page WHERE code=200", nativeQuery = true)
    int getCountRecords();
    @Query(value = "SELECT COUNT(*) FROM page WHERE code=200 AND site_id=:site_id", nativeQuery = true)
    int getCountRecordsSiteId(@Param("site_id") int siteId);
}
