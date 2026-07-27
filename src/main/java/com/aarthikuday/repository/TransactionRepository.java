package com.aarthikuday.repository;

import com.aarthikuday.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByClientId(Long clientId);
    List<Transaction> findByClientUserUsername(String username);
    List<Transaction> findByInvestmentId(Long investmentId);
    List<Transaction> findByClientIdOrderByTransactionDateDesc(Long clientId);
    List<Transaction> findByClientUserUsernameOrderByTransactionDateDesc(String username);
}
