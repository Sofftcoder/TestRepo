package com.osc.repository;

import com.osc.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByEmail(String email);

    @Query(value = "Select Password from User where email = ?1", nativeQuery = true)
    String findPassword(String email);

    @Query(value = "Select name from User where email = ?1", nativeQuery = true)
    String findName(String email);

    @Transactional
    @Modifying
    @Query(value = "Update User SET password = ?2 where email = ?1 ",nativeQuery = true)
    void changePassword(String email,String Password);


}
