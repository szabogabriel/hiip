package com.hiip.datastorage.service.authentication;

import com.hiip.datastorage.entity.User;
import com.hiip.datastorage.repository.UserRepository;
import com.hiip.datastorage.security.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameAndIsActiveTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        // Build authorities list based on user's admin status
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (user.getIsAdmin()) {
            authorities.add(new SimpleGrantedAuthority(UserRole.ADMIN.getAuthority()));
        }
        authorities.add(new SimpleGrantedAuthority(UserRole.USER.getAuthority()));
        
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .build();
    }
}
