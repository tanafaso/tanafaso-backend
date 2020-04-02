package com.omar.azkar.configs.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        String token = getJwtToken(httpServletRequest);
        if(token != null) {

            JWTVerifier verifier = JWT.require(Algorithm.HMAC512(jwtSecret)).build();
            try {
                String userId = verifier.verify(token).getSubject();
                UserDetails userDetails = userDetailsService.loadUserById(userId);
                if (userDetails != null) {
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            } catch (JWTVerificationException exception) {
                // invalid token
            }
        }
        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }

    private String getJwtToken(HttpServletRequest httpServletRequest) {
        String token = httpServletRequest.getHeader("Authorization");
        if(token != null && token.startsWith("Bearer ")) {
            return token.replace("Bearer ", "");
        }
        return null;
    }
}
