package com.hiip.datastorage.repository;

import com.hiip.datastorage.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    /**
     * Find a category by its complete path
     */
    Optional<Category> findByPath(String path);
    
    /**
     * Find all root categories (categories without a parent)
     */
    @Query("SELECT c FROM Category c WHERE c.parent IS NULL ORDER BY c.name")
    List<Category> findRootCategories();
    
    /**
     * Find categories accessible by a user (global, owned by user, or shared with user)
     */
    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN c.sharedWith s " +
           "WHERE c.isGlobal = true OR c.createdBy = :username OR " +
           "(s.sharedWithUsername = :username AND s.canRead = true) " +
           "ORDER BY c.path")
    List<Category> findAccessibleByUser(String username);
    
    /**
     * Find categories where user has write permission
     */
    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN c.sharedWith s " +
           "WHERE c.createdBy = :username OR " +
           "(s.sharedWithUsername = :username AND s.canWrite = true) " +
           "ORDER BY c.path")
    List<Category> findWritableByUser(String username);
    
    /**
     * Find categories created by a specific user
     */
    List<Category> findByCreatedByOrderByPath(String createdBy);
    
    /**
     * Find global categories
     */
    List<Category> findByIsGlobalTrueOrderByPath();
    
    /**
     * Find all categories that start with a given path prefix
     */
    @Query("SELECT c FROM Category c WHERE c.path LIKE :pathPrefix% ORDER BY c.path")
    List<Category> findByPathStartingWith(String pathPrefix);
    
    /**
     * Find direct children of a category
     */
    List<Category> findByParentOrderByName(Category parent);
    
    /**
     * Check if a category exists by path
     */
    boolean existsByPath(String path);
}
