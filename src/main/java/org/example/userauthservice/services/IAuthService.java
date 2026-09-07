package org.example.userauthservice.services;

import org.antlr.v4.runtime.misc.Pair;
import org.example.userauthservice.models.User;

public interface IAuthService {
    User signUp(String email, String password, String name, String phoneNumber);
    Pair<User, String> login(String email, String password);
    Boolean validateToken(String token);
}
