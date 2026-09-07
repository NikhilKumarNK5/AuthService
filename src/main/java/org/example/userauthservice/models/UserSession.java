package org.example.userauthservice.models;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Data
@Entity
public class UserSession extends BaseModel {
    private String token;
    @ManyToOne
    private User user;
}
