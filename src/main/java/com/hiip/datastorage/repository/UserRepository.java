package com.hiip.datastorage.repository;

import com.hiip.datastorage.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameAndIsActiveTrue(String username);
    Optional<User> findByEmail(String email);
    
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isActive = :isActive WHERE u.username = :username")
    void setIsActiveByUsername(@Param("username") String username, @Param("isActive") Boolean isActive);
}
