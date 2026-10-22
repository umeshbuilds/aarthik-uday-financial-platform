package com.aarthikuday.service;

import com.aarthikuday.model.Client;
import com.aarthikuday.model.Investment;
import com.aarthikuday.repository.ClientRepository;
import com.aarthikuday.repository.InvestmentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final ClientRepository clientRepository;
    private final InvestmentRepository investmentRepository;

    public DashboardService(ClientRepository clientRepository, InvestmentRepository investmentRepository) {
        this.clientRepository = clientRepository;
        this.investmentRepository = investmentRepository;
    }

    public Map<String, Object> getAdminDashboardStats() {
        List<Client> clients = clientRepository.findAll();
        List<Investment> investments = investmentRepository.findAll();

        BigDecimal totalAum = BigDecimal.ZERO;
        BigDecimal totalInvested = BigDecimal.ZERO;

        for (Client c : clients) {
            totalAum = totalAum.add(c.getCurrentValue());
            totalInvested = totalInvested.add(c.getTotalInvested());
        }

        BigDecimal totalProfit = totalAum.subtract(totalInvested);
        BigDecimal averagePortfolio = clients.isEmpty() 
                ? BigDecimal.ZERO 
                : totalAum.divide(BigDecimal.valueOf(clients.size()), 2, RoundingMode.HALF_UP);

        // Calculate partner-level aggregates (NJ Wealth vs IIFL)
        BigDecimal njWealthValue = BigDecimal.ZERO;
        BigDecimal iiflValue = BigDecimal.ZERO;

        // Calculate asset type aggregates
        BigDecimal sipValue = BigDecimal.ZERO;
        BigDecimal swpValue = BigDecimal.ZERO;
        BigDecimal insuranceValue = BigDecimal.ZERO;
        BigDecimal stocksValue = BigDecimal.ZERO;

        for (Investment inv : investments) {
            BigDecimal val = inv.getCurrentValue();
            if ("NJ_WEALTH".equalsIgnoreCase(inv.getPartner())) {
                njWealthValue = njWealthValue.add(val);
            } else if ("IIFL".equalsIgnoreCase(inv.getPartner())) {
                iiflValue = iiflValue.add(val);
            }

            switch (inv.getType().toUpperCase()) {
                case "SIP":
                    sipValue = sipValue.add(val);
                    break;
                case "SWP":
                    swpValue = swpValue.add(val);
                    break;
                case "INSURANCE":
                    insuranceValue = insuranceValue.add(val);
                    break;
                case "STOCKS":
                    stocksValue = stocksValue.add(val);
                    break;
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalClients", clients.size());
        stats.put("totalAum", totalAum);
        stats.put("totalInvested", totalInvested);
        stats.put("totalProfit", totalProfit);
        stats.put("averagePortfolio", averagePortfolio);
        
        // Partner breakdown
        stats.put("njWealthValue", njWealthValue);
        stats.put("iiflValue", iiflValue);

        // Product allocation
        stats.put("sipValue", sipValue);
        stats.put("swpValue", swpValue);
        stats.put("insuranceValue", insuranceValue);
        stats.put("stocksValue", stocksValue);

        return stats;
    }

    public Map<String, Object> getClientDashboardStats(String username) {
        Client client = clientRepository.findByUserUsername(username).orElse(null);

        if (client == null) {
            Map<String, Object> emptyStats = new HashMap<>();
            emptyStats.put("clientId", 0L);
            emptyStats.put("fullName", username);
            emptyStats.put("totalInvested", BigDecimal.ZERO);
            emptyStats.put("currentValue", BigDecimal.ZERO);
            emptyStats.put("totalProfit", BigDecimal.ZERO);
            emptyStats.put("njWealthValue", BigDecimal.ZERO);
            emptyStats.put("iiflValue", BigDecimal.ZERO);
            emptyStats.put("sipValue", BigDecimal.ZERO);
            emptyStats.put("swpValue", BigDecimal.ZERO);
            emptyStats.put("insuranceValue", BigDecimal.ZERO);
            emptyStats.put("stocksValue", BigDecimal.ZERO);
            return emptyStats;
        }

        List<Investment> investments = investmentRepository.findByClientId(client.getId());

        // Partner-level aggregates
        BigDecimal njWealthValue = BigDecimal.ZERO;
        BigDecimal iiflValue = BigDecimal.ZERO;

        // Product type aggregates
        BigDecimal sipValue = BigDecimal.ZERO;
        BigDecimal swpValue = BigDecimal.ZERO;
        BigDecimal insuranceValue = BigDecimal.ZERO;
        BigDecimal stocksValue = BigDecimal.ZERO;

        for (Investment inv : investments) {
            BigDecimal val = inv.getCurrentValue();
            if ("NJ_WEALTH".equalsIgnoreCase(inv.getPartner())) {
                njWealthValue = njWealthValue.add(val);
            } else if ("IIFL".equalsIgnoreCase(inv.getPartner())) {
                iiflValue = iiflValue.add(val);
            }

            switch (inv.getType().toUpperCase()) {
                case "SIP":
                    sipValue = sipValue.add(val);
                    break;
                case "SWP":
                    swpValue = swpValue.add(val);
                    break;
                case "INSURANCE":
                    insuranceValue = insuranceValue.add(val);
                    break;
                case "STOCKS":
                    stocksValue = stocksValue.add(val);
                    break;
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("clientId", client.getId());
        stats.put("fullName", client.getUser().getFullName());
        stats.put("totalInvested", client.getTotalInvested());
        stats.put("currentValue", client.getCurrentValue());
        stats.put("totalProfit", client.getTotalProfit());

        // Partner breakdown
        stats.put("njWealthValue", njWealthValue);
        stats.put("iiflValue", iiflValue);

        // Product allocation
        stats.put("sipValue", sipValue);
        stats.put("swpValue", swpValue);
        stats.put("insuranceValue", insuranceValue);
        stats.put("stocksValue", stocksValue);

        return stats;
    }
}
