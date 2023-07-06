package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexModel;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexModel, Integer> {
    @Query(value = "SELECT * FROM index_search WHERE page_id=:page", nativeQuery = true)
    List<IndexModel> findAllByPageId(@Param("page") int page);
}
