package org.example.userauthservice.services;

import org.example.userauthservice.models.User;
import org.example.userauthservice.repos.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepo userRepo;

    public User getUserDetails(Long id) {
        Optional<User> userOptional = userRepo.findById(id);
        if (userOptional.isEmpty()) return null;
        return userOptional.get();
    }
}
