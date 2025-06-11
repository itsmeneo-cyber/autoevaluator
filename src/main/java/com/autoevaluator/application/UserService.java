package com.autoevaluator.application;

import com.autoevaluator.domain.entity.AppUser;
import com.autoevaluator.domain.repositories.AppUserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private AppUserRepository repo;


    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
    public AppUser register(AppUser user) {
        user.setPassword(encoder.encode(user.getPassword()));
        repo.save(user);
        return user;
    }
}