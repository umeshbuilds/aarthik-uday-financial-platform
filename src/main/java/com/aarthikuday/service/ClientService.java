package com.aarthikuday.service;

import com.aarthikuday.model.Client;
import com.aarthikuday.model.Investment;
import com.aarthikuday.model.User;
import com.aarthikuday.repository.ClientRepository;
import com.aarthikuday.repository.InvestmentRepository;
import com.aarthikuday.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final InvestmentRepository investmentRepository;
    private final PasswordEncoder passwordEncoder;

    public ClientService(ClientRepository clientRepository, UserRepository userRepository,
                         InvestmentRepository investmentRepository, PasswordEncoder passwordEncoder) {
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.investmentRepository = investmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    public Client getClientById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found with ID: " + id));
    }

    public Client getClientByUsername(String username) {
        return clientRepository.findByUserUsername(username)
                .orElseThrow(() -> new RuntimeException("Client not found for username: " + username));
    }

    @Transactional
    public Client createClient(String username, String password, String fullName, String email,
                               String phoneNumber, String panNumber, String address) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists: " + email);
        }

        // Create User entity
        User user = new User(
                username,
                passwordEncoder.encode(password),
                "ROLE_CLIENT",
                fullName,
                email
        );
        user.setSrlNo(userRepository.findMaxSrlNo() + 1);
        user = userRepository.save(user);

        // Create Client entity linked to the user
        Client client = new Client(user, phoneNumber, panNumber, address);
        return clientRepository.save(client);
    }

    @Transactional
    public void updateClientPortfolioTotals(Long clientId) {
        Client client = getClientById(clientId);
        List<Investment> investments = investmentRepository.findByClientId(clientId);

        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal currentValue = BigDecimal.ZERO;

        for (Investment inv : investments) {
            totalInvested = totalInvested.add(inv.getInvestedAmount());
            currentValue = currentValue.add(inv.getCurrentValue());
        }

        client.setTotalInvested(totalInvested);
        client.setCurrentValue(currentValue);
        client.setTotalProfit(currentValue.subtract(totalInvested));

        clientRepository.save(client);
    }
}
