package org.example.userauthservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.antlr.v4.runtime.misc.Pair;
import org.example.userauthservice.clients.KafkaProducerClient;
import org.example.userauthservice.dtos.EmailDto;
import org.example.userauthservice.exceptions.PasswordMismatchException;
import org.example.userauthservice.exceptions.UserAlreadyExistsException;
import org.example.userauthservice.exceptions.UserNotRegisteredException;
import org.example.userauthservice.models.Role;
import org.example.userauthservice.models.Status;
import org.example.userauthservice.models.User;
import org.example.userauthservice.models.UserSession;
import org.example.userauthservice.repos.RoleRepo;
import org.example.userauthservice.repos.SessionRepo;
import org.example.userauthservice.repos.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;

@Service
public class AuthService implements IAuthService {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private RoleRepo roleRepo;

    @Autowired
    private SessionRepo sessionRepo;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private SecretKey secretKey;

    @Autowired
    private KafkaProducerClient kafkaProducerClient;

    @Autowired
    private ObjectMapper objectMapper;

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
//        user.setPassword(password);  //this should not be passed as raw password
        user.setPassword(bCryptPasswordEncoder.encode(password));
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

        // Put message into Kafka
        EmailDto emailDto = new EmailDto();
        emailDto.setTo(email);
        emailDto.setFrom("nikhil.nikki.nk05@gmail.com");
        emailDto.setSubject("Welcome");
        emailDto.setBody("Problem Solver @Salesforce");
        try {
            String message = objectMapper.writeValueAsString(emailDto);
            kafkaProducerClient.sendMessage("signup", message);
        } catch (JsonProcessingException exception)  {
            throw new RuntimeException(exception.getMessage());
        }

        return userRepo.save(user);
    }

    @Override
    public Pair<User, String> login(String email, String password) {

        Optional<User> userOptional = userRepo.findByEmail(email);
        if (userOptional.isEmpty()) {
            throw new UserNotRegisteredException("Please signup first");
        }

        User user = userOptional.get();
//        if(!user.getPassword().equals(password)) {
//            throw new PasswordMismatchException("Please use correct credentials");
//        }

        if(!bCryptPasswordEncoder.matches(password, user.getPassword())) {
            throw  new PasswordMismatchException("Please use correct credentials");
        }

        // Generating JWT
        Map<String, Object> claims = new HashMap<>();
        claims.put("user_id", user.getId());
        claims.put("issuer", "nik");
        Long currentTime = System.currentTimeMillis();
        claims.put("iat", currentTime);
        claims.put("exp", currentTime + 100000);

        List<Role> roles = user.getRoles();
        List<String> roleValues = new ArrayList<>();
        for (Role role : roles) {
            roleValues.add(role.getValue());
        }

        claims.put("user_access", roleValues);

//        MacAlgorithm algorithm = Jwts.SIG.HS256;
//        SecretKey secretKey = algorithm.key().build();

        String token = Jwts.builder().claims(claims).signWith(secretKey).compact();

        UserSession userSession = new UserSession();
        userSession.setUser(user);
        userSession.setToken(token);
        userSession.setStatus(Status.ACTIVE);
        sessionRepo.save(userSession);

        return new Pair<>(user, token);
    }

    public Boolean validateToken(String token) {
        Optional<UserSession> userSessionOptional = sessionRepo.findByToken(token);

        if(userSessionOptional.isEmpty()) {
            return false;
        }

        UserSession userSession = userSessionOptional.get();

        JwtParser jwtParser = Jwts.parser().verifyWith(secretKey).build();
        Claims claims = jwtParser.parseSignedClaims(token).getPayload();

        Long expiry = (Long) claims.get("exp");
        Long currentTime = System.currentTimeMillis();
        System.out.println("expiry: " + expiry);
        System.out.println("currentTime: " + currentTime);

        if(expiry < currentTime) {
            userSession.setStatus(Status.INACTIVE);
            sessionRepo.save(userSession);
            System.out.println("Token has expired");
            return false;
        }

        return true;
    }
}
