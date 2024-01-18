package com.osc.repository;

import com.osc.entity.Products;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Products,String> {
    Products findByProductId(String key);

    @Query(value = "SELECT * FROM Products WHERE CategoryId = :categoryId ORDER BY ViewCount DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    Products findTopByCategoryId(@Param("categoryId") String categoryId, @Param("limit") int limit, @Param("offset") int offset);

    @Query(value = "SELECT * From Products where CategoryId = ?1 ORDER BY ViewCount DESC",nativeQuery = true)
    List<Products> findByCategoryId(String categoryId);

    @Query(value = "SELECT * FROM Products WHERE CategoryId = ?1", nativeQuery = true)
    List<Products> findByPrice(String categoryId);

}
