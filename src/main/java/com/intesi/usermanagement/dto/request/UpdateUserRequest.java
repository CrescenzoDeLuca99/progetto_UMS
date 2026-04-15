package com.intesi.usermanagement.dto.request;

import com.intesi.usermanagement.domain.enums.RoleName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Set;

/*
 * Email e codiceFiscale sono immutabili dopo la creazione: non esposti qui.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

    @NotBlank(message = "Username obbligatorio")
    @Size(min = 3, max = 50, message = "Username deve essere tra 3 e 50 caratteri")
    private String username;

    @NotBlank(message = "Nome obbligatorio")
    @Size(max = 100)
    private String nome;

    @NotBlank(message = "Cognome obbligatorio")
    @Size(max = 100)
    private String cognome;

    @NotEmpty(message = "Almeno un ruolo è obbligatorio")
    private Set<RoleName> roles;
}
