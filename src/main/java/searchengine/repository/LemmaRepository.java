package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaModel;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaModel, Integer> {
    @Query(value = "SELECT * FROM lemma WHERE lemma=:lemma AND site_id=:site_id", nativeQuery = true)
    LemmaModel findAllByLemmaAndSiteId(@Param("lemma") String lemma, @Param("site_id") int siteId);

    @Query(value = "SELECT * FROM lemma WHERE lemma=:lemma AND site_id=:site_id", nativeQuery = true)
    List <LemmaModel> findAllByLemmaAndSite(@Param("lemma") String lemma, @Param("site_id") int siteId);

    @Query(value = "SELECT * FROM lemma WHERE id=:id", nativeQuery = true)
    LemmaModel findById(@Param("id") int id);

    @Query(value = "SELECT * FROM lemma WHERE site_id=:site_id", nativeQuery = true)
    List <LemmaModel> findAllBySiteId(@Param("site_id") int siteId);

    @Query(value = "SELECT * FROM lemma WHERE lemma=:lemma", nativeQuery = true)
    List<LemmaModel> findAllByLemma(@Param("lemma") String lemma);
}
