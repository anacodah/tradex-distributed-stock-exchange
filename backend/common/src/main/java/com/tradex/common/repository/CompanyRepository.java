package com.tradex.common.repository;

import com.tradex.common.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findBySymbol(String symbol);

    List<Company> findBySectorOrderBySymbolAsc(String sector);

    @Query("SELECT DISTINCT c.sector FROM Company c WHERE c.sector IS NOT NULL ORDER BY c.sector ASC")
    List<String> findAllSectors();

    @Query("SELECT c FROM Company c WHERE LOWER(c.symbol) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY c.symbol ASC")
    List<Company> searchBySymbolOrName(@Param("query") String query);
}
