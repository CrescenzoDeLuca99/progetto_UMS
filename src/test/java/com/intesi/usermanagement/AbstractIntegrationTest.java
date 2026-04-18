package com.intesi.usermanagement;

import com.intesi.usermanagement.config.SecurityTestConfig;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Date;
import java.util.List;
import java.util.Map;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(SecurityTestConfig.class)
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    static final KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @SuppressWarnings("resource")
    static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    static {
        postgres.start();
        kafka.start();
        redis.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }

    /*
      Configura TestRestTemplate con un token ADMIN prima di ogni test.
      I test RBAC possono sovrascrivere l'interceptor con ruoli diversi.
     */
    @BeforeEach
    void configureAuth(@Autowired TestRestTemplate restTemplate) {
        setTokenRoles(restTemplate, List.of("ADMIN"));
    }

    /*
      Sostituisce l'interceptor del RestTemplate con un token per i ruoli indicati.
      Utile nei test RBAC per simulare utenti con permessi ridotti.
     */
    protected static void setTokenRoles(TestRestTemplate restTemplate, List<String> roles) {
        restTemplate.getRestTemplate().setInterceptors(List.of(
                (request, body, execution) -> {
                    request.getHeaders().setBearerAuth(generateToken("test-user", roles));
                    return execution.execute(request, body);
                }
        ));
    }

    /*
      Genera un JWT con i ruoli indicati nel claim realm_access.roles (struttura Keycloak).
     */
    protected static String generateToken(String subject, List<String> roles) {
        try {
            var signer = new RSASSASigner(SecurityTestConfig.TEST_RSA_KEY);
            var claims = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .issuer("test")
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + 3_600_000L))
                    .claim("realm_access", Map.of("roles", roles))
                    .build();
            var jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(),
                    claims);
            jwt.sign(signer);
            return jwt.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Generazione JWT di test fallita", e);
        }
    }
}
