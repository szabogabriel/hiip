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
    
    // Search by category path pattern (with wildcards)
    @Query("SELECT d FROM DataStorage d WHERE d.category.path LIKE :pathPattern AND d.owner = :owner AND d.hidden = false")
    List<DataStorage> findByCategoryPathLikeAndOwnerAndHiddenFalse(
        @Param("pathPattern") String pathPattern, 
        @Param("owner") String owner);
    
    // Search by tags and category path pattern (with wildcards)
    @Query("SELECT DISTINCT d FROM DataStorage d JOIN d.tags t WHERE t IN :tags AND d.category.path LIKE :pathPattern AND d.owner = :owner AND d.hidden = false")
    List<DataStorage> findByTagsAndCategoryPathLikeAndOwnerAndHiddenFalse(
        @Param("tags") List<String> tags,
        @Param("pathPattern") String pathPattern, 
        @Param("owner") String owner);
    
    // ========== Queries including shared data ==========
    
    /**
     * Find all data entries accessible by user (owned or shared via category)
     */
    @Query("SELECT DISTINCT d FROM DataStorage d " +
           "LEFT JOIN d.category c " +
           "LEFT JOIN c.sharedWith cs " +
           "WHERE (d.owner = :username OR (cs.sharedWithUsername = :username AND cs.canRead = true) OR c.isGlobal = true) " +
           "AND d.hidden = false")
    List<DataStorage> findAccessibleByUser(@Param("username") String username);
    
    /**
     * Find data entries accessible by user and matching tags
     */
    @Query("SELECT DISTINCT d FROM DataStorage d " +
           "JOIN d.tags t " +
           "LEFT JOIN d.category c " +
           "LEFT JOIN c.sharedWith cs " +
           "WHERE t IN :tags " +
           "AND (d.owner = :username OR (cs.sharedWithUsername = :username AND cs.canRead = true) OR c.isGlobal = true) " +
           "AND d.hidden = false")
    List<DataStorage> findByTagsAccessibleByUser(@Param("tags") List<String> tags, @Param("username") String username);
    
    /**
     * Find data entries accessible by user in specific category
     */
    @Query("SELECT DISTINCT d FROM DataStorage d " +
           "LEFT JOIN d.category c " +
           "LEFT JOIN c.sharedWith cs " +
           "WHERE d.category = :category " +
           "AND (d.owner = :username OR (cs.sharedWithUsername = :username AND cs.canRead = true) OR c.isGlobal = true) " +
           "AND d.hidden = false")
    List<DataStorage> findByCategoryAccessibleByUser(@Param("category") Category category, @Param("username") String username);
    
    /**
     * Find data entries accessible by user matching both tags and category
     */
    @Query("SELECT DISTINCT d FROM DataStorage d " +
           "JOIN d.tags t " +
           "LEFT JOIN d.category c " +
           "LEFT JOIN c.sharedWith cs " +
           "WHERE t IN :tags " +
           "AND d.category = :category " +
           "AND (d.owner = :username OR (cs.sharedWithUsername = :username AND cs.canRead = true) OR c.isGlobal = true) " +
           "AND d.hidden = false")
    List<DataStorage> findByTagsAndCategoryAccessibleByUser(
        @Param("tags") List<String> tags, 
        @Param("category") Category category, 
        @Param("username") String username);
    
    /**
     * Find data entries accessible by user matching category path pattern
     */
    @Query("SELECT DISTINCT d FROM DataStorage d " +
           "LEFT JOIN d.category c " +
           "LEFT JOIN c.sharedWith cs " +
           "WHERE c.path LIKE :pathPattern " +
           "AND (d.owner = :username OR (cs.sharedWithUsername = :username AND cs.canRead = true) OR c.isGlobal = true) " +
           "AND d.hidden = false")
    List<DataStorage> findByCategoryPathLikeAccessibleByUser(@Param("pathPattern") String pathPattern, @Param("username") String username);
    
    /**
     * Find data entries accessible by user matching both tags and category path pattern
     */
    @Query("SELECT DISTINCT d FROM DataStorage d " +
           "JOIN d.tags t " +
           "LEFT JOIN d.category c " +
           "LEFT JOIN c.sharedWith cs " +
           "WHERE t IN :tags " +
           "AND c.path LIKE :pathPattern " +
           "AND (d.owner = :username OR (cs.sharedWithUsername = :username AND cs.canRead = true) OR c.isGlobal = true) " +
           "AND d.hidden = false")
    List<DataStorage> findByTagsAndCategoryPathLikeAccessibleByUser(
        @Param("tags") List<String> tags,
        @Param("pathPattern") String pathPattern, 
        @Param("username") String username);
}
