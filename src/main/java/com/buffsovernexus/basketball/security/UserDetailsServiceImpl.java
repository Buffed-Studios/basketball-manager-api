package com.buffsovernexus.basketball.security;

import com.buffsovernexus.basketball.entity.Account;
import com.buffsovernexus.basketball.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found: " + username));

        List<GrantedAuthority> authorities = new ArrayList<>();

        // Base role every authenticated user gets
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        // Superusers bypass all individual access node checks
        if (account.isSuperuser()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SUPERUSER"));
        }

        // Map each access node string directly as a GrantedAuthority
        account.getAccesses().stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        return new User(account.getUsername(), account.getPasswordHash(), authorities);
    }
}
