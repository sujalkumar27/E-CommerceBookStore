package com.bookstore.service;

import com.bookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * ============================================================
 * UserDetailsServiceImpl — Loads a User from the Database
 * ============================================================
 *
 * WHAT THIS IS:
 * Spring Security needs a way to load a user from the database
 * when it wants to check credentials (during login) or verify
 * a JWT token (on every protected request).
 *
 * It calls loadUserByUsername(email) and expects back a UserDetails object.
 * Our User entity already implements UserDetails, so we just return it directly.
 *
 * WHERE THIS IS USED:
 * 1. In JwtAuthFilter — to load the User after validating a token
 * 2. In SecurityConfig — wired into the AuthenticationProvider
 *    which uses it to verify passwords during login
 *
 * WHY A SEPARATE CLASS?
 * Spring Security requires a bean that implements UserDetailsService.
 * Putting this logic here keeps our User entity and UserRepository clean.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load a user by their email address (Spring Security calls this "username").
     *
     * @param email - the user's email
     * @return UserDetails (our User entity, which implements UserDetails)
     * @throws UsernameNotFoundException if no user with this email exists
     *         Spring Security catches this and converts it to BadCredentialsException
     *         (so the client gets a generic "invalid credentials" message)
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
