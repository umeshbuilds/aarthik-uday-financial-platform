package com.aarthikuday.repository;

import com.aarthikuday.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByUserUsername(String username);
    Optional<Client> findByUserId(Long userId);
}
