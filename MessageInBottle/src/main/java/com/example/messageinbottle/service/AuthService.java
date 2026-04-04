package com.example.messageinbottle.service;

import com.example.messageinbottle.dto.AuthResponse;
import com.example.messageinbottle.dto.LoginRequest;
import com.example.messageinbottle.dto.RegisterRequest;
import com.example.messageinbottle.entity.User;
import com.example.messageinbottle.entity.Wallet;
import com.example.messageinbottle.repository.UserRepository;
import com.example.messageinbottle.repository.WalletRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    public AuthService(UserRepository userRepository, WalletRepository walletRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }

        long now = System.currentTimeMillis();

        User user = new User();
        user.setUsername(request.getUsername());
        user.setNickname(request.getNickname());
        user.setPassword(request.getPassword());
        user.setCreatedAt(now);
        user = userRepository.save(user);

        Wallet wallet = new Wallet();
        wallet.setUserId(user.getId());
        wallet.setBalance(0D);
        wallet.setUpdatedAt(now);
        walletRepository.save(wallet);

        return new AuthResponse(user.getId(), user.getUsername(), user.getNickname(), user.getCreatedAt());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameAndPassword(request.getUsername(), request.getPassword())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));

        return new AuthResponse(user.getId(), user.getUsername(), user.getNickname(), user.getCreatedAt());
    }
}

