package com.omar.azkar.configs.jwt;

import com.omar.azkar.entities.User;
import com.omar.azkar.repos.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepo userRepository;

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        // TODO: return the actual user.
        return null;
    }

    public UserDetails loadUserById(String id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            return new UserPrincipal(user.get());
        }
        return null;
    }
}
