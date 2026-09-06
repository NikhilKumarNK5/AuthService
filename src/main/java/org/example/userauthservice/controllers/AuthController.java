package org.example.userauthservice.controllers;

import org.example.userauthservice.dtos.LoginRequestDto;
import org.example.userauthservice.dtos.SignupRequestDto;
import org.example.userauthservice.dtos.UserDto;
import org.example.userauthservice.models.Role;
import org.example.userauthservice.models.User;
import org.example.userauthservice.services.IAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private IAuthService authService;

    // signup or register
    @PostMapping("/signup")
    public ResponseEntity<UserDto> signup(@RequestBody SignupRequestDto signupRequestDto) {
        User user = authService.signUp(
                signupRequestDto.getEmail(),
                signupRequestDto.getPassword(),
                signupRequestDto.getName(),
                signupRequestDto.getPhoneNumber());

        return new ResponseEntity<>(from(user), HttpStatus.CREATED);
    }

    // login
    @PostMapping("/login")
    public ResponseEntity<UserDto> login(@RequestBody LoginRequestDto loginRequestDto) {
        User user = authService.login(
                loginRequestDto.getEmail(),
                loginRequestDto.getPassword()
        );

        return new ResponseEntity<>(from(user), HttpStatus.OK);
    }

    private UserDto from(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setName(user.getName());
        userDto.setEmail(user.getEmail());
        List<String> roleValues = new ArrayList<>();
        for (Role role : user.getRoles()) {
            roleValues.add(role.getValue());
        }
        userDto.setRoles(roleValues);
        return userDto;
    }
}
