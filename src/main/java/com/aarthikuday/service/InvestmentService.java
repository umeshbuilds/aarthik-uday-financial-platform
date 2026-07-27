package com.aarthikuday.service;

import com.aarthikuday.model.Client;
import com.aarthikuday.model.Investment;
import com.aarthikuday.model.Transaction;
import com.aarthikuday.repository.ClientRepository;
import com.aarthikuday.repository.InvestmentRepository;
import com.aarthikuday.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;

@Service
public class InvestmentService {

    private static final Logger log = LoggerFactory.getLogger(InvestmentService.class);

    private final InvestmentRepository investmentRepository;
    private final ClientRepository clientRepository;
    private final TransactionRepository transactionRepository;
    private final ClientService clientService;
    private final Random random = new Random();

    public InvestmentService(InvestmentRepository investmentRepository, ClientRepository clientRepository,
                             TransactionRepository transactionRepository, ClientService clientService) {
        this.investmentRepository = investmentRepository;
        this.clientRepository = clientRepository;
        this.transactionRepository = transactionRepository;
        this.clientService = clientService;
    }

    public List<Investment> getInvestmentsByClientId(Long clientId) {
        return investmentRepository.findByClientId(clientId);
    }

    public List<Investment> getInvestmentsByUsername(String username) {
        return investmentRepository.findByClientUserUsername(username);
    }

    @Transactional
    public Investment addInvestment(Long clientId, String partner, String type, String schemeName,
                                    BigDecimal units, BigDecimal buyPrice) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found with ID: " + clientId));

        Investment investment = new Investment(client, partner, type, schemeName, units, buyPrice);
        investment = investmentRepository.save(investment);

        // Record a transaction for this purchase
        Transaction tx = new Transaction(client, investment, "BUY", investment.getInvestedAmount(), "SUCCESS");
        transactionRepository.save(tx);

        // Update Client profile totals
        clientService.updateClientPortfolioTotals(clientId);

        return investment;
    }

    @Transactional
    public void simulateLiveMarketUpdates() {
        List<Investment> investments = investmentRepository.findAll();
        if (investments.isEmpty()) {
            return;
        }

        log.debug("Simulating live market update for {} investments...", investments.size());

        for (Investment inv : investments) {
            BigDecimal currentPrice = inv.getCurrentPrice();

            // Generate a small fluctuation percentage (-1.5% to +2.0%)
            // Stocks fluctuate a bit more than mutual funds or insurance
            double maxFluctuation = inv.getType().equalsIgnoreCase("STOCKS") ? 0.03 : 0.01;
            double percentChange = (random.nextDouble() * 2 * maxFluctuation) - maxFluctuation;
            
            // Add a slight upward bias (0.15%) to simulate standard market growth over time
            percentChange += 0.0015; 

            BigDecimal multiplier = BigDecimal.valueOf(1.0 + percentChange);
            BigDecimal newPrice = currentPrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

            // Ensure price doesn't fall below a logical threshold (e.g. 5.0)
            if (newPrice.compareTo(BigDecimal.valueOf(5)) < 0) {
                newPrice = BigDecimal.valueOf(5.00);
            }

            inv.setCurrentPrice(newPrice);
            inv.recalculateTotals();
            investmentRepository.save(inv);
        }

        // Update portfolio aggregates for all active clients
        List<Client> clients = clientRepository.findAll();
        for (Client c : clients) {
            clientService.updateClientPortfolioTotals(c.getId());
        }

        log.debug("Simulation completed successfully.");
    }
}
