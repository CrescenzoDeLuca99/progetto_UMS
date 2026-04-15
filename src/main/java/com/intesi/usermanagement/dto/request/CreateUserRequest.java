package com.intesi.usermanagement.dto.request;

import com.intesi.usermanagement.domain.enums.RoleName;
import com.intesi.usermanagement.validation.ValidCodiceFiscale;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {

    @NotBlank(message = "Username obbligatorio")
    @Size(min = 3, max = 50, message = "Username deve essere tra 3 e 50 caratteri")
    private String username;

    @NotBlank(message = "Email obbligatoria")
    @Email(message = "Email non valida")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "Codice fiscale obbligatorio")
    @ValidCodiceFiscale
    private String codiceFiscale;

    @NotBlank(message = "Nome obbligatorio")
    @Size(max = 100)
    private String nome;

    @NotBlank(message = "Cognome obbligatorio")
    @Size(max = 100)
    private String cognome;

    @NotEmpty(message = "Almeno un ruolo è obbligatorio")
    private Set<RoleName> roles;
}
