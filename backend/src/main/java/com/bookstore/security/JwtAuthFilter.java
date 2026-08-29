package com.bookstore.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ============================================================
 * JwtAuthFilter — Checks the JWT Token on Every Request
 * ============================================================
 *
 * WHAT IS A FILTER?
 * A filter intercepts every HTTP request BEFORE it reaches the controller.
 * Think of it as a security checkpoint at the entrance.
 *
 * WHAT THIS FILTER DOES:
 * For every incoming request:
 *   1. Look for the "Authorization: Bearer <token>" header
 *   2. If no token → let the request continue (public routes will work,
 *      protected routes will be rejected by SecurityConfig)
 *   3. If token found → validate it
 *   4. If valid → extract the user's email, load their details from DB,
 *      and tell Spring "this request is from THIS authenticated user"
 *   5. If invalid/expired → let the request continue without authentication
 *      (Spring will then reject it for protected routes)
 *
 * WHY "OncePerRequestFilter"?
 * It guarantees the filter runs EXACTLY once per HTTP request,
 * even if Spring's filter chain would otherwise call it multiple times.
 */
@Component
@RequiredArgsConstructor  // Lombok: auto-generates constructor for final fields
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;                     // For token validation
    private final UserDetailsService userDetailsService; // To load user from DB

    /**
     * The main filter logic — runs for every HTTP request.
     *
     * @param request     - the incoming HTTP request
     * @param response    - the outgoing HTTP response
     * @param filterChain - the rest of the filter chain (call this to continue)
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Step 1: Read the Authorization header from the request
        // Expected format: "Bearer eyJhbGciOiJ..."
        final String authHeader = request.getHeader("Authorization");

        // Step 2: If no Authorization header or it doesn't start with "Bearer ",
        // just let the request continue — no authentication set
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 3: Extract just the token part (remove "Bearer " prefix)
        final String token = authHeader.substring(7);

        // Step 4: Validate the token — is signature correct? Is it expired?
        if (!jwtUtil.isValid(token)) {
            // Token is invalid or expired — continue without authentication
            filterChain.doFilter(request, response);
            return;
        }

        // Step 5: Extract the user's email from the token
        final String email = jwtUtil.extractEmail(token);

        // Step 6: If we have an email and no authentication is set yet,
        // load the user and set them as the current authenticated user
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Load full user details from database (roles, etc.)
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // Create an authentication object telling Spring who this user is
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,                  // The user object
                            null,                         // Credentials (not needed after login)
                            userDetails.getAuthorities()  // Their roles/permissions
                    );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Store the authentication in the current request's security context
            // After this, Spring knows who is making this request
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        // Step 7: Continue to the next filter (and eventually the controller)
        filterChain.doFilter(request, response);
    }
}
