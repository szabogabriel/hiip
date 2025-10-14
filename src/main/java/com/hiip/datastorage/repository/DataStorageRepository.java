package com.hiip.datastorage.repository;

import com.hiip.datastorage.entity.Category;
import com.hiip.datastorage.entity.DataStorage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DataStorageRepository extends JpaRepository<DataStorage, Long> {
    
    List<DataStorage> findByOwnerAndHiddenFalse(String owner);
    
    @Query("SELECT DISTINCT d FROM DataStorage d JOIN d.tags t WHERE t IN :tags AND d.owner = :owner AND d.hidden = false")
    List<DataStorage> findByTagsInAndOwnerAndHiddenFalse(@Param("tags") List<String> tags, @Param("owner") String owner);
    
    // Search by category only
    List<DataStorage> findByCategoryAndOwnerAndHiddenFalse(Category category, String owner);
    
    // Search by tags and category
    @Query("SELECT DISTINCT d FROM DataStorage d JOIN d.tags t WHERE t IN :tags AND d.category = :category AND d.owner = :owner AND d.hidden = false")
    List<DataStorage> findByTagsAndCategoryAndOwnerAndHiddenFalse(
        @Param("tags") List<String> tags, 
        @Param("category") Category category, 
        @Param("owner") String owner);
}
