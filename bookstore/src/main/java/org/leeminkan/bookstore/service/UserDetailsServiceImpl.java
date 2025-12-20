package org.leeminkan.bookstore.service;

import org.leeminkan.bookstore.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Look up the user by username using our custom repository method
        return userRepository.findByUsername(username)
                // If the user is found, convert our custom User entity into the required Spring UserDetails object
                .map(user -> org.springframework.security.core.userdetails.User.builder()
                        .username(user.getUsername())
                        .password(user.getPassword()) // Spring Security will check this password
                        .authorities("ROLE_" + user.getRole()) // We must use .authorities() here
                         .build())
                // If the Optional is empty (user not found), throw the required exception
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}