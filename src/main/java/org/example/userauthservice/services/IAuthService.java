package org.example.userauthservice.services;

import org.example.userauthservice.models.User;

public interface IAuthService {
    User signUp(String email, String password, String name, String phoneNumber);
    User login(String email, String password);
}
