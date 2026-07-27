package com.aarthikuday.controller;

import com.aarthikuday.dto.InvestmentRequest;
import com.aarthikuday.model.Investment;
import com.aarthikuday.model.Transaction;
import com.aarthikuday.repository.InvestmentRepository;
import com.aarthikuday.repository.TransactionRepository;
import com.aarthikuday.service.InvestmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class InvestmentController {

    private final InvestmentService investmentService;
    private final InvestmentRepository investmentRepository;
    private final TransactionRepository transactionRepository;

    public InvestmentController(InvestmentService investmentService, 
                                InvestmentRepository investmentRepository,
                                TransactionRepository transactionRepository) {
        this.investmentService = investmentService;
        this.investmentRepository = investmentRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/investments")
    public ResponseEntity<List<Investment>> getInvestments(@RequestParam(required = false) Long clientId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

        if (isAdmin) {
            if (clientId != null) {
                return ResponseEntity.ok(investmentService.getInvestmentsByClientId(clientId));
            }
            return ResponseEntity.ok(investmentRepository.findAll());
        } else {
            // Client gets their own holdings
            return ResponseEntity.ok(investmentService.getInvestmentsByUsername(auth.getName()));
        }
    }

    @PostMapping("/admin/investments")
    public ResponseEntity<Investment> addInvestment(@Valid @RequestBody InvestmentRequest request) {
        Investment investment = investmentService.addInvestment(
                request.getClientId(),
                request.getPartner(),
                request.getType(),
                request.getSchemeName(),
                request.getUnits(),
                request.getBuyPrice()
        );
        return ResponseEntity.ok(investment);
    }

    @PostMapping("/admin/investments/simulate-live")
    public ResponseEntity<Map<String, String>> triggerLiveSync() {
        investmentService.simulateLiveMarketUpdates();
        return ResponseEntity.ok(Map.of("message", "Live price sync completed successfully.", "status", "SUCCESS"));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getClientTransactions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(transactionRepository.findByClientUserUsernameOrderByTransactionDateDesc(auth.getName()));
    }
}
