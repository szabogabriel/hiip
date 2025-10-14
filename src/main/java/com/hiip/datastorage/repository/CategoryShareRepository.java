package com.hiip.datastorage.repository;

import com.hiip.datastorage.entity.Category;
import com.hiip.datastorage.entity.CategoryShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryShareRepository extends JpaRepository<CategoryShare, Long> {
    
    /**
     * Find all shares for a specific category
     */
    List<CategoryShare> findByCategory(Category category);
    
    /**
     * Find all categories shared with a specific user
     */
    List<CategoryShare> findBySharedWithUsername(String username);
    
    /**
     * Find a specific share by category and username
     */
    Optional<CategoryShare> findByCategoryAndSharedWithUsername(Category category, String username);
    
    /**
     * Delete all shares for a specific category
     */
    void deleteByCategory(Category category);
    
    /**
     * Delete a specific share
     */
    void deleteByCategoryAndSharedWithUsername(Category category, String username);
    
    /**
     * Check if a category is shared with a user
     */
    boolean existsByCategoryAndSharedWithUsername(Category category, String username);
    
    /**
     * Find categories shared with user that have write permission
     */
    @Query("SELECT cs FROM CategoryShare cs WHERE cs.sharedWithUsername = :username AND cs.canWrite = true")
    List<CategoryShare> findWritableSharesForUser(String username);
}
