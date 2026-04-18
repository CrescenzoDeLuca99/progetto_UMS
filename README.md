# User Management Service

Servizio REST per la gestione anagrafica degli utenti, sviluppato con Spring Boot 3 e architettura esagonale. Espone API per creazione, modifica, gestione dello stato e cancellazione logica degli utenti, con autenticazione JWT tramite Keycloak e pubblicazione di eventi su Kafka.

---

## Requisiti

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) — è l'unico prerequisito. Java e Maven non sono necessari sulla macchina host.

---

## Avvio

```bash
# 1. Clona il repository
git clone https://github.com/CrescenzoDeLuca99/progetto_UMS.git
cd Esercizio_Intesi

# 2. Avvia lo stack completo
docker compose up --build -d
```

Al primo avvio Docker compila l'immagine dell'applicativo (~3–5 min per il download delle dipendenze Maven). Gli avvii successivi partono in ~2 min, dominati dall'inizializzazione di Keycloak.

### Servizi esposti

| Servizio | URL |
|---|---|
| API + Swagger UI | http://localhost:8080/swagger-ui.html |
| Keycloak (console admin) | http://localhost:8180 — `admin / admin` |
| Kafka UI | http://localhost:8090 |
| PostgreSQL | `localhost:5432` — `usermgmt / usermgmt` |
| Redis | `localhost:6379` |

### Fermare lo stack

```bash
docker compose down          # ferma i container, mantiene i volumi
docker compose down -v       # ferma i container e cancella i dati
```

---

## Testare le API

Lo stack include tre utenti di test pre-configurati in Keycloak:

| Username | Password | Ruolo |
|---|---|---|
| `admin-user` | `admin` | `OWNER` |
| `operator-user` | `operator` | `OPERATOR` |
| `dev-user` | `developer` | `DEVELOPER` |

### Tramite Swagger UI

1. Apri http://localhost:8080/swagger-ui.html
2. Clicca il pulsante **Authorize** in alto a destra
3. Inserisci `username` e `password` di uno degli utenti di test
4. Clicca **Authorize** — Swagger ottiene il token da Keycloak automaticamente
5. Chiudi il dialog e usa gli endpoint direttamente dalla UI

### Matrice dei permessi

| Operazione | OWNER | OPERATOR | MAINTAINER | DEVELOPER | REPORTER |
|---|:---:|:---:|:---:|:---:|:---:|
| Lettura | ✅ | ✅ | ✅ | ✅ | ✅ |
| Creazione / Modifica | ✅ | ✅ | ✅ | ❌ | ❌ |
| Abilita / Disabilita | ✅ | ✅ | ❌ | ❌ | ❌ |
| Cancellazione | ✅ | ❌ | ❌ | ❌ | ❌ |

---

## Sviluppo locale (senza Docker per l'app)

Per sviluppare con hot-reload è possibile avviare solo l'infrastruttura e far girare l'app sul proprio JDK:

```bash
# avvia solo l'infrastruttura
docker compose up -d postgres kafka redis keycloak keycloak-init

# avvia l'app con Maven (richiede Java 21)
./mvnw spring-boot:run
```

L'app usa il profilo `dev` che punta a `localhost` per tutti i servizi.

### Eseguire i test

```bash
./mvnw verify
```

I test di integrazione usano Testcontainers: avviano automaticamente container dedicati per Postgres, Kafka e Redis, separati da quelli di sviluppo. Docker Desktop deve essere in esecuzione.

---

## Scelte tecniche

### Architettura esagonale (Ports & Adapters)

Il progetto adotta l'architettura esagonale per isolare il core applicativo dall'infrastruttura.

```
api/controller         →  inbound adapter  (HTTP)
application/port/in    →  porte in-bound   (UserCommandUseCase, UserQueryUseCase)
application/service    →  core applicativo (UserService)
application/port/out   →  porte out-bound  (UserPersistencePort, RolePersistencePort, UserEventPort)
infrastructure/dao     →  outbound adapter (DB + cache)
infrastructure/messaging →  outbound adapter (Kafka)
```

`UserService` dipende esclusivamente da interfacce. Il controller non conosce `UserService` ma solo i contratti delle porte. Questa struttura rende ogni layer sostituibile e testabile in isolamento.

### Soft-delete

Gli utenti non vengono mai cancellati fisicamente. La cancellazione imposta `status = DELETED`; tutte le query operative escludono questo status. Il record rimane in database per esigenze di audit.

### Caching con Redis

I ruoli sono cacheable con TTL di 24h (cambiano solo per manutenzione). Gli utenti hanno TTL di 10 min. La cache viene aggiornata proattivamente ad ogni salvataggio (`@CachePut`) per evitare letture stantie.

La self-injection via `@Lazy` in `UserDaoImpl` e `RoleDaoImpl` è necessaria affinché Spring AOP intercetti le chiamate a `@Cacheable` — le chiamate interne allo stesso bean bypassano il proxy.

### Autenticazione JWT + Keycloak

L'applicativo è un OAuth2 Resource Server: valida i token JWT firmati da Keycloak senza gestire sessioni (STATELESS). I ruoli vengono estratti dal claim `realm_access.roles` del token e convertiti in authority Spring Security con prefisso `ROLE_`.

### Kafka — eventi di dominio

Ogni operazione di scrittura pubblica un evento asincrono sul topic `user-events` (CREATED, UPDATED, DISABLED, ENABLED, DELETED). La pubblicazione è fire-and-forget: il thread chiamante non viene bloccato; gli errori sono loggati a livello ERROR.

### Test con Testcontainers

I test di integrazione usano container reali per tutta l'infrastruttura (Postgres, Kafka, Redis). I test di sicurezza generano token JWT firmati con una chiave RSA in-memory, replicando esattamente il comportamento di produzione senza dipendere da Keycloak.

### Dockerfile multi-stage

La build è divisa in tre stage: compilazione Maven, estrazione dei layer Spring Boot, runtime con JRE Alpine. L'immagine finale pesa ~80 MB. Il layered JAR massimizza il cache hit in CI: modificando solo il codice applicativo, Docker ricopia solo l'ultimo layer senza riscaricare le dipendenze.

La JVM è configurata con `UseContainerSupport` e `MaxRAMPercentage=75` per rispettare i memory limit del container senza andare in OOM kill.

---

## Struttura del progetto

```
src/main/java/com/intesi/usermanagement/
├── api/                    # Controller e GlobalExceptionHandler
├── application/
│   ├── port/in/            # Porte in-bound (use case interfaces)
│   ├── port/out/           # Porte out-bound (persistence e messaging interfaces)
│   └── service/            # UserService
├── domain/
│   ├── model/              # User, Role
│   └── enums/              # UserStatus, RoleName, UserEventType
├── infrastructure/
│   ├── dao/impl/           # UserDaoImpl, RoleDaoImpl (cache + DB)
│   ├── messaging/          # UserEventPublisher (Kafka)
│   ├── repository/         # Spring Data JPA
│   ├── config/             # Cache, OpenAPI, DataInitializer
│   └── security/           # JWT, RBAC
├── dto/                    # Request e Response
├── mapper/                 # MapStruct
├── validation/             # @ValidCodiceFiscale
└── exception/              # Eccezioni di dominio
```
