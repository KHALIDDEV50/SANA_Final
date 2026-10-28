package com.example.sana.Service;

import com.example.sana.Model.User;
import com.example.sana.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MyUserDetailsService implements UserDetailsService, UserDetailsPasswordService {

    private final UserRepository userRepository;

    // Basic Auth sends the email as the username
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findUserByEmail(username);
        if (user == null) {
            throw new UsernameNotFoundException("Wrong email or password");
        }
        return user;
    }

    // Spring calls this after a successful login with an old plain-text password,
    // so the password is saved again as a BCrypt hash
    @Override
    public UserDetails updatePassword(UserDetails userDetails, String newPassword) {
        User user = userRepository.findUserByEmail(userDetails.getUsername());
        user.setPasswordHash(newPassword);
        return userRepository.save(user);
    }
}
