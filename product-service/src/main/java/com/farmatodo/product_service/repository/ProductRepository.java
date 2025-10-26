package com.farmatodo.product_service.repository;

import com.farmatodo.product_service.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.category) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "p.stock > :minStock AND " +
           "p.status = 'ACTIVE'")
    List<Product> searchProducts(@Param("query") String query, @Param("minStock") int minStock);

    /**
     * Find all active products with stock less than the specified threshold
     * @param maxStock Maximum stock level (exclusive)
     * @return List of products with stock < maxStock
     */
    @Query("SELECT p FROM Product p WHERE " +
           "p.stock < :maxStock AND " +
           "p.status = 'ACTIVE' " +
           "ORDER BY p.stock ASC")
    List<Product> findProductsWithLowStock(@Param("maxStock") int maxStock);

    /**
     * Find all active products with stock greater than the specified threshold
     * @param minStock Minimum stock level (exclusive)
     * @return List of products with stock > minStock
     */
    @Query("SELECT p FROM Product p WHERE " +
           "p.stock < :minStock AND " +
           "p.status = 'ACTIVE' " +
           "ORDER BY p.stock DESC")
    List<Product> findProductsWithMinStock(@Param("minStock") int minStock);
}
