package com.gjx.security;


import com.gjx.entity.User;
import com.gjx.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private IUserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("loadUserByUsername: " + username);
        User user = userService.findByUsername(username);
        System.out.println("Found user: " + user);
        if (user == null) {
            System.out.println("User not found: " + username);
            throw new UsernameNotFoundException("用户不存在");
        }
        System.out.println("Creating UserDetails for: " + username + ", password: " + user.getPassword() + ", role: " + user.getRole());
        return org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password(user.getPassword())
                .authorities(user.getRole())
                .build();
    }
}