package com.aarthikuday.repository;

import com.aarthikuday.model.Investment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvestmentRepository extends JpaRepository<Investment, Long> {
    List<Investment> findByClientId(Long clientId);
    List<Investment> findByClientUserUsername(String username);
    List<Investment> findByPartner(String partner);
}
