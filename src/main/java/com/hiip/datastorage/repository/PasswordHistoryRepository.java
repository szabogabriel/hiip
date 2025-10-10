package com.hiip.datastorage.repository;

import com.hiip.datastorage.entity.PasswordHistory;
import com.hiip.datastorage.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {
    
    /**
     * Find the most recent password history entries for a user, ordered by creation date descending
     */
    @Query("SELECT ph FROM PasswordHistory ph WHERE ph.user = :user ORDER BY ph.createdAt DESC")
    List<PasswordHistory> findByUserOrderByCreatedAtDesc(@Param("user") User user);
    
    /**
     * Find the most recent N password history entries for a user
     */
    @Query("SELECT ph FROM PasswordHistory ph WHERE ph.user = :user ORDER BY ph.createdAt DESC")
    List<PasswordHistory> findTopNByUserOrderByCreatedAtDesc(@Param("user") User user, 
                                                             org.springframework.data.domain.Pageable pageable);
    
    /**
     * Count password history entries for a user
     */
    long countByUser(User user);
    
    /**
     * Delete oldest password history entries for a user, keeping only the most recent N entries
     */
    @Query("DELETE FROM PasswordHistory ph WHERE ph.user = :user AND ph.id NOT IN " +
           "(SELECT ph2.id FROM PasswordHistory ph2 WHERE ph2.user = :user ORDER BY ph2.createdAt DESC LIMIT :keepCount)")
    void deleteOldPasswordHistory(@Param("user") User user, @Param("keepCount") int keepCount);
}