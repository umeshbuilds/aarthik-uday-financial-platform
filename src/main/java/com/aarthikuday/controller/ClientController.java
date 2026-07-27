package com.aarthikuday.controller;

import com.aarthikuday.dto.ClientRequest;
import com.aarthikuday.model.Client;
import com.aarthikuday.model.Investment;
import com.aarthikuday.model.Transaction;
import com.aarthikuday.repository.TransactionRepository;
import com.aarthikuday.service.ClientService;
import com.aarthikuday.service.InvestmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/clients")
public class ClientController {

    private final ClientService clientService;
    private final InvestmentService investmentService;
    private final TransactionRepository transactionRepository;

    public ClientController(ClientService clientService, InvestmentService investmentService,
                            TransactionRepository transactionRepository) {
        this.clientService = clientService;
        this.investmentService = investmentService;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping
    public ResponseEntity<List<Client>> getAllClients() {
        return ResponseEntity.ok(clientService.getAllClients());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Client> getClientById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.getClientById(id));
    }

    @PostMapping
    public ResponseEntity<Client> createClient(@Valid @RequestBody ClientRequest request) {
        Client client = clientService.createClient(
                request.getUsername(),
                request.getPassword(),
                request.getFullName(),
                request.getEmail(),
                request.getPhoneNumber(),
                request.getPanNumber(),
                request.getAddress()
        );
        return ResponseEntity.ok(client);
    }

    @GetMapping("/{id}/investments")
    public ResponseEntity<List<Investment>> getClientInvestments(@PathVariable Long id) {
        return ResponseEntity.ok(investmentService.getInvestmentsByClientId(id));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<Transaction>> getClientTransactions(@PathVariable Long id) {
        return ResponseEntity.ok(transactionRepository.findByClientIdOrderByTransactionDateDesc(id));
    }
}
