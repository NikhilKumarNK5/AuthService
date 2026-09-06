package org.example.userauthservice.services;

import org.example.userauthservice.exceptions.PasswordMismatchException;
import org.example.userauthservice.exceptions.UserAlreadyExistsException;
import org.example.userauthservice.exceptions.UserNotRegisteredException;
import org.example.userauthservice.models.Role;
import org.example.userauthservice.models.User;
import org.example.userauthservice.repos.RoleRepo;
import org.example.userauthservice.repos.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AuthService implements IAuthService {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private RoleRepo roleRepo;

    @Override
    public User signUp(String email, String password, String name, String phoneNumber) {

        Optional<User> userOptional = userRepo.findByEmail(email);
        if (userOptional.isPresent()) {
            throw new UserAlreadyExistsException("Please try different emailId");
        }

        userOptional = userRepo.findByPhoneNumber(phoneNumber);
        if (userOptional.isPresent()) {
            throw new UserAlreadyExistsException("Please try different phoneNumber");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(password);  //this should not be passed as raw password
        user.setPhoneNumber(phoneNumber);
        user.setName(name);

        // Optimize this role creation
        Role role;
        Optional<Role> optionalRole = roleRepo.findByValue("NON_ADMIN");
        if (optionalRole.isEmpty()) {
            role = new Role();
            role.setValue("NON_ADMIN");
            roleRepo.save(role);
        } else {
            role = optionalRole.get();
        }

        List<Role> roles = new ArrayList<>();
        roles.add(role);
        user.setRoles(roles);

        return userRepo.save(user);
    }

    @Override
    public User login(String email, String password) {

        Optional<User> userOptional = userRepo.findByEmail(email);
        if (userOptional.isEmpty()) {
            throw new UserNotRegisteredException("Please signup first");
        }

        User user = userOptional.get();
        if(!user.getPassword().equals(password)) {
            throw new PasswordMismatchException("Please use correct credentials");
        }

        return user;
    }
}
