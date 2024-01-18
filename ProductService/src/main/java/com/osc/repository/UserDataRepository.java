package com.osc.repository;

import com.osc.entity.UserData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDataRepository extends JpaRepository<UserData,String> {

    UserData findByUserId(String userId);

    @Query(value = "SELECT * From UserData where userId =?1",nativeQuery = true)
    UserData findRecentlyViewedDataByUserId(String userId);
}
