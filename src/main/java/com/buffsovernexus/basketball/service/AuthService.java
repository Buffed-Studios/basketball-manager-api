package com.buffsovernexus.basketball.service;

import com.buffsovernexus.basketball.dto.AccountInfoResponse;
import com.buffsovernexus.basketball.dto.AuthResponse;
import com.buffsovernexus.basketball.dto.LoginRequest;
import com.buffsovernexus.basketball.dto.RegisterRequest;
import com.buffsovernexus.basketball.entity.Account;
import com.buffsovernexus.basketball.exception.UsernameAlreadyExistsException;
import com.buffsovernexus.basketball.repository.AccountRepository;
import com.buffsovernexus.basketball.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (accountRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException(request.username());
        }

        Account account = Account.builder()
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();

        accountRepository.save(account);

        String token = jwtUtil.generateToken(account.getUsername());
        return new AuthResponse(token, account.getUsername());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        String token = jwtUtil.generateToken(request.username());
        return new AuthResponse(token, request.username());
    }

    @Transactional(readOnly = true)
    public AccountInfoResponse getMe(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found: " + username));

        return new AccountInfoResponse(account.getId(), account.getUsername(), account.getCreatedAt());
    }
}
