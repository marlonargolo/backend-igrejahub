package com.igrejahub.security.service;

import com.igrejahub.security.UserPrincipal;
import com.igrejahub.users.entity.User;
import com.igrejahub.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("🔐 Loading user by email: {}", email);
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> {
                log.error("❌ User not found with email: {}", email);
                return new UsernameNotFoundException("Usuário não encontrado com email: " + email);
            });
        
        log.info("✅ User found: {}", user.getEmail());
        return new UserPrincipal(user);
    }
}
