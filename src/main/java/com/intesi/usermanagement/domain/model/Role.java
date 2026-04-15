package com.intesi.usermanagement.domain.model;

import com.intesi.usermanagement.domain.enums.RoleName;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true, length = 20)
    private RoleName name;

    public Role(RoleName name) {
        this.name = name;
    }
}
