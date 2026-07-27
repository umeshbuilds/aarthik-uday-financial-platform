package com.aarthikuday.config;

import com.aarthikuday.model.Client;
import com.aarthikuday.model.User;
import com.aarthikuday.repository.UserRepository;
import com.aarthikuday.service.ClientService;
import com.aarthikuday.service.InvestmentService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ClientService clientService;
    private final InvestmentService investmentService;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(UserRepository userRepository, ClientService clientService,
                          InvestmentService investmentService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.clientService = clientService;
        this.investmentService = investmentService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            return; // DB already seeded
        }

        // 1. Seed Admin Owner Account
        User admin = new User(
                "admin",
                passwordEncoder.encode("admin123"),
                "ROLE_ADMIN",
                "Umesh Menariya",
                "admin@aarthikuday.com"
        );
        admin.setSrlNo(userRepository.findMaxSrlNo() + 1);
        userRepository.save(admin);

        // 2. Seed Client 1: Rahul Sharma (Balanced SIP & Stocks)
        Client client1 = clientService.createClient(
                "rahul",
                "rahul123",
                "Rahul Sharma",
                "rahul@gmail.com",
                "9876543210",
                "ABCDE1234F",
                "102, Skyline Towers, Mumbai"
        );
        // Add Holdings
        investmentService.addInvestment(client1.getId(), "NJ_WEALTH", "SIP", "Nippon India Small Cap Fund", BigDecimal.valueOf(125.45), BigDecimal.valueOf(120.50));
        investmentService.addInvestment(client1.getId(), "NJ_WEALTH", "SIP", "HDFC Balanced Advantage Fund", BigDecimal.valueOf(480.12), BigDecimal.valueOf(250.00));
        investmentService.addInvestment(client1.getId(), "IIFL", "STOCKS", "Reliance Industries Ltd", BigDecimal.valueOf(40.00), BigDecimal.valueOf(2450.00));
        investmentService.addInvestment(client1.getId(), "IIFL", "STOCKS", "Tata Motors Ltd", BigDecimal.valueOf(60.00), BigDecimal.valueOf(620.00));

        // 3. Seed Client 2: Priya Patel (Mutual Fund Focus & SWP)
        Client client2 = clientService.createClient(
                "priya",
                "priya123",
                "Priya Patel",
                "priya@yahoo.com",
                "8765432109",
                "FGHIJ5678K",
                "405, Green Meadows, Ahmedabad"
        );
        investmentService.addInvestment(client2.getId(), "NJ_WEALTH", "SIP", "Parag Parikh Flexi Cap Fund", BigDecimal.valueOf(1800.50), BigDecimal.valueOf(55.20));
        investmentService.addInvestment(client2.getId(), "NJ_WEALTH", "SWP", "SBI Equity Hybrid Fund (SWP)", BigDecimal.valueOf(2500.00), BigDecimal.valueOf(210.00));
        investmentService.addInvestment(client2.getId(), "NJ_WEALTH", "INSURANCE", "LIC Tech Term Insurance", BigDecimal.valueOf(1.00), BigDecimal.valueOf(25000.00));
        investmentService.addInvestment(client2.getId(), "IIFL", "STOCKS", "Infosys Ltd", BigDecimal.valueOf(50.00), BigDecimal.valueOf(1480.00));

        // 4. Seed Client 3: Amit Verma (Stock Specialist)
        Client client3 = clientService.createClient(
                "amit",
                "amit123",
                "Amit Verma",
                "amit@outlook.com",
                "7654321098",
                "LMNOP9012Q",
                "Apartment 3C, Pearl Heights, Delhi"
        );
        investmentService.addInvestment(client3.getId(), "IIFL", "STOCKS", "HDFC Bank Ltd", BigDecimal.valueOf(120.00), BigDecimal.valueOf(1550.00));
        investmentService.addInvestment(client3.getId(), "IIFL", "STOCKS", "ITC Ltd", BigDecimal.valueOf(400.00), BigDecimal.valueOf(430.00));
        investmentService.addInvestment(client3.getId(), "IIFL", "STOCKS", "Tata Consultancy Services Ltd", BigDecimal.valueOf(35.00), BigDecimal.valueOf(3400.00));
        investmentService.addInvestment(client3.getId(), "NJ_WEALTH", "SIP", "Mirae Asset Large Cap Fund", BigDecimal.valueOf(350.00), BigDecimal.valueOf(115.00));

        // 5. Seed Client 4: Sneha Reddy (SIP and Insurance Conservative Portfolio)
        Client client4 = clientService.createClient(
                "sneha",
                "sneha123",
                "Sneha Reddy",
                "sneha@gmail.com",
                "6543210987",
                "RSTUV3456W",
                "Villa 12, Royal Palms, Hyderabad"
        );
        investmentService.addInvestment(client4.getId(), "NJ_WEALTH", "SIP", "ICICI Prudential Bluechip Fund", BigDecimal.valueOf(850.00), BigDecimal.valueOf(82.40));
        investmentService.addInvestment(client4.getId(), "NJ_WEALTH", "INSURANCE", "HDFC Life Click 2 Protect", BigDecimal.valueOf(1.00), BigDecimal.valueOf(18500.00));

        // 6. Seed Client 5: Vikram Singh (High Net Worth Portfolio)
        Client client5 = clientService.createClient(
                "vikram",
                "vikram123",
                "Vikram Singh",
                "vikram@co.in",
                "9812345670",
                "XYZAB7890C",
                "Flat 10B, High Rise Residency, Bangalore"
        );
        investmentService.addInvestment(client5.getId(), "NJ_WEALTH", "SIP", "Quant Small Cap Fund", BigDecimal.valueOf(2500.00), BigDecimal.valueOf(185.00));
        investmentService.addInvestment(client5.getId(), "NJ_WEALTH", "SWP", "ICICI Equity & Debt Fund (SWP)", BigDecimal.valueOf(4000.00), BigDecimal.valueOf(290.00));
        investmentService.addInvestment(client5.getId(), "IIFL", "STOCKS", "Larsen & Toubro Ltd", BigDecimal.valueOf(150.00), BigDecimal.valueOf(2800.00));
        investmentService.addInvestment(client5.getId(), "IIFL", "STOCKS", "Maruti Suzuki India Ltd", BigDecimal.valueOf(25.00), BigDecimal.valueOf(9800.00));
        investmentService.addInvestment(client5.getId(), "IIFL", "STOCKS", "Reliance Industries Ltd", BigDecimal.valueOf(100.00), BigDecimal.valueOf(2450.00));
    }
}
