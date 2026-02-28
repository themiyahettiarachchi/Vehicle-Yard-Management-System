package com.vms.repository;

import com.vms.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Integer> {
    List<Sale> findBySaleType(String saleType);

    @Query("SELECT COALESCE(SUM(s.salePrice), 0) FROM Sale s")
    BigDecimal getTotalRevenue();

    List<Sale> findAllByOrderBySaleDateDesc();
}
