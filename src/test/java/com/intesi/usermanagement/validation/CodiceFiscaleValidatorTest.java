package com.intesi.usermanagement.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CodiceFiscaleValidatorTest {

    private CodiceFiscaleValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CodiceFiscaleValidator();
    }

    // -------------------------------------------------------------------------
    // Casi validi
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "valido: \"{0}\"")
    @ValueSource(strings = {
            "RSSMRA85M01H501Z",   // caso standard
            "BNCMRA72A41F205X",   // donna (giorno > 40)
            "RSSMRA85M01H501Z",   // uppercase già verificato
            "rssmra85m01h501z",   // lowercase — il validatore accetta case-insensitive
            "VRDGNN07M03F839P",   // altro CF reale-like
            "MRTNTN80A01H501U",   // nome con consonanti ripetute
            // omocodia: una cifra sostituita con lettera [LMNPQRSTUV]
            "RSSMRAL5M01H501Z",
    })
    void shouldAcceptValidCodiceFiscale(String cf) {
        assertThat(validator.isValid(cf, null)).isTrue();
    }

    // -------------------------------------------------------------------------
    // Null → valido (la gestione null spetta a @NotBlank)
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "null accettato: delegato a @NotBlank")
    @NullSource
    void shouldAcceptNull(String cf) {
        assertThat(validator.isValid(cf, null)).isTrue();
    }

    // -------------------------------------------------------------------------
    // Casi non validi
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "non valido: \"{0}\"")
    @ValueSource(strings = {
            "",                    // stringa vuota
            "   ",                 // solo spazi
            "RSSMRA85M01H501",     // 15 char — troppo corto
            "RSSMRA85M01H501ZZ",   // 17 char — troppo lungo
            "RSSMRA85X01H501Z",    // mese 'X' non valido (non in ABCDEHLMPRST)
            "123MRA85M01H501Z",    // cognome con cifre
            "RSSMRA85M01H5O1Z",    // carattere 'O' nella parte comune (non in LMNPQRSTUV)
            "RSS MRA85M01H501Z",   // spazio interno
    })
    void shouldRejectInvalidCodiceFiscale(String cf) {
        assertThat(validator.isValid(cf, null)).isFalse();
    }
}
