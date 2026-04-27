# Travel Booking System

A microservices-based travel booking backend that exposes unified search across flights, hotels, and car rentals through a single API gateway.

[![Java](https://img.shields.io/badge/Java-11-orange?logo=openjdk)](https://adoptium.net/temurin/releases/?version=11)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.3.12-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-Hoxton.SR12-brightgreen?logo=spring)](https://spring.io/projects/spring-cloud)
[![Gradle](https://img.shields.io/badge/Gradle-6.9.4-blue?logo=gradle)](https://gradle.org)
[![License](https://img.shields.io/badge/License-MIT-lightgrey)](LICENSE)

```
                        ┌─────────────────────────────┐
                        │   Eureka Discovery Server   │
                        │        localhost:8761       │
                        └──────────────┬──────────────┘
                                       │ registers / discovers
           ┌───────────────────────────┼───────────────────────────┐
           │                           │                           │
  ┌────────▼────────┐        ┌─────────▼────────┐       ┌─────────▼────────┐
  │  Flight Service │        │  Hotel Service   │       │ Car Rental Svc   │
  │  localhost:8081 │        │  localhost:8082  │       │  localhost:8083  │
  └────────▲────────┘        └─────────▲────────┘       └─────────▲────────┘
           │                           │                           │
           └───────────────────────────┼───────────────────────────┘
                                       │ routes via service ID
                              ┌────────▼────────┐
                              │   API Gateway   │
                              │  localhost:8080 │
                              └────────▲────────┘
                                       │
                                    Client
```

---

## Overview

This project implements a lightweight travel booking backend using the Spring Cloud Netflix stack. A single API gateway (Zuul) acts as the entry point for all client requests and routes them to the appropriate downstream service via Eureka-based service discovery — so consumers never need to know individual service ports or addresses.

The architecture deliberately uses Netflix OSS components (Eureka, Zuul) rather than the newer Spring Cloud Gateway to demonstrate the Hoxton-era microservices pattern, which remains widely deployed in enterprise environments. Eureka handles dynamic service registration and health checking; Zuul handles routing and can be extended with filters for auth, rate limiting, and request transformation.

This project is structured as a Gradle multi-module build so all five services share a single root build configuration, keeping dependency versions consistent and simplifying CI pipelines.

---

## Architecture

### Services

| Service             | Port | Role                                          |
|---------------------|------|-----------------------------------------------|
| `discovery-service` | 8761 | Eureka server — service registry              |
| `api-gateway`       | 8080 | Zuul proxy — single entry point for clients   |
| `flight-service`    | 8081 | Flight search (RapidAPI + mock fallback)      |
| `hotel-service`     | 8082 | Hotel search (mock data)                      |
| `car-rental-service`| 8083 | Car rental search (mock data)                 |

### Request Flow

```
$ curl "localhost:8080/flights/search?origin=JFK&destination=LAX&date=2026-05-01"
        │
        ▼
  API Gateway (8080)
  Zuul matches path /flights/**
        │
        ▼
  Eureka (8761)
  Resolves service ID "FLIGHT-SERVICE" → localhost:8081
        │
        ▼
  Flight Service (8081)
  GET /flights/search?origin=JFK&destination=LAX&date=2026-05-01
        │
        ▼
  JSON response back through gateway to client
```

> Zuul is configured with `strip-prefix: false`, meaning the full path including `/flights/` is forwarded to the downstream service unchanged. The flight service's controller is mapped to `/flights/search`, so no path rewriting is needed.

### Technology Stack

| Layer             | Technology                          | Version        |
|-------------------|-------------------------------------|----------------|
| Language          | Java                                | 11             |
| Framework         | Spring Boot                         | 2.3.12.RELEASE |
| Cloud             | Spring Cloud Netflix                | Hoxton.SR12    |
| Service Discovery | Netflix Eureka                      | —              |
| API Gateway       | Netflix Zuul                        | —              |
| HTTP Client       | OkHttp3                             | 4.9.3          |
| JSON              | Jackson Databind                    | (managed)      |
| Build             | Gradle (multi-module)               | 6.9.4          |

---

## Prerequisites

### Java 11 — Required

Gradle 6.9.4 supports Java 8–16 only. **Java 17+ will not work** with this build configuration.

**macOS (Homebrew):**
```bash
$ brew install --cask temurin11
```

**Verify:**
```bash
$ java -version
# Expected: openjdk version "11.x.x" ...
```

**If you have multiple JDKs installed,** point Gradle to Java 11 by creating `gradle.properties` in the project root:

```properties
org.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-11.jdk/Contents/Home
```

Adjust the path to match your installation (`/usr/libexec/java_home -v 11` will print the correct path on macOS).

### Gradle

No separate Gradle installation required. The project includes a Gradle wrapper (`./gradlew`). Make it executable if needed:

```bash
$ chmod +x gradlew
```

### Port Availability

Ensure these ports are free before starting services:

```bash
$ lsof -i :8761,8080,8081,8082,8083
# No output = ports are available
```

---

## Getting Started

### 1. Clone the Repository

```bash
$ git clone <repository-url>
$ cd travel-booking-system
```

### 2. Build the Project

```bash
$ ./gradlew clean build
```

A successful build compiles all five modules and produces executable JARs in each service's `build/libs/` directory.

### 3. Start Services — Order Matters

Services must start in this exact sequence. The discovery server must be fully up before business services register, and the gateway must start last so it can find registered instances.

**Step 1 — Start the discovery server (Eureka):**
```bash
$ cd discovery-service
$ ../gradlew bootRun
```

Wait approximately **30 seconds** for Eureka to finish initializing. You should see:
```
Started DiscoveryServiceApplication in X.XXX seconds
```

Open the Eureka dashboard to confirm it's running: [http://localhost:8761](http://localhost:8761)

**Step 2 — Start business services (new terminal windows, in parallel):**

```bash
# Terminal 2
$ cd flight-service && ../gradlew bootRun

# Terminal 3
$ cd hotel-service && ../gradlew bootRun

# Terminal 4
$ cd car-rental-service && ../gradlew bootRun
```

Wait for each to print `Started ...Application in X.XXX seconds`. Refresh the Eureka dashboard — you should see `FLIGHT-SERVICE`, `HOTEL-SERVICE`, and `CAR-RENTAL-SERVICE` listed under "Instances currently registered with Eureka".

**Step 3 — Start the API gateway:**
```bash
# Terminal 5
$ cd api-gateway && ../gradlew bootRun
```

### 4. Verify Everything is Running

```bash
$ curl -s http://localhost:8761/eureka/apps | grep "<app>"
```

Expected output (order may vary):
```xml
<app>FLIGHT-SERVICE</app>
<app>HOTEL-SERVICE</app>
<app>CAR-RENTAL-SERVICE</app>
<app>API-GATEWAY</app>
```

---

## API Documentation

All requests go through the gateway on port **8080**.

| Service      | Gateway Endpoint          | Method | Required Parameters            |
|--------------|---------------------------|--------|-------------------------------|
| Flights      | `/flights/search`         | GET    | `origin`, `destination`, `date`|
| Hotels       | `/hotels/search`          | GET    | `location`                     |
| Car Rentals  | `/cars/search`            | GET    | `location`                     |

### Flight Search

```bash
$ curl -s "http://localhost:8080/flights/search?origin=JFK&destination=LAX&date=2026-05-15" | python3 -m json.tool
```

**Example response:**
```json
[
  {
    "id": "MOCK001",
    "origin": "JFK",
    "destination": "LAX",
    "departureTime": "2026-05-15T08:00",
    "arrivalTime": "2026-05-15T11:30",
    "airline": "United Airlines",
    "price": 450.0,
    "currency": "$450"
  },
  {
    "id": "MOCK002",
    "origin": "JFK",
    "destination": "LAX",
    "departureTime": "2026-05-15T14:00",
    "arrivalTime": "2026-05-15T17:30",
    "airline": "Delta",
    "price": 380.0,
    "currency": "$380"
  }
]
```

### Hotel Search

```bash
$ curl -s "http://localhost:8080/hotels/search?location=New+York" | python3 -m json.tool
```

**Example response:**
```json
[
  {
    "id": "H001",
    "name": "Grand Plaza Hotel",
    "location": "New York",
    "pricePerNight": 180.0,
    "rating": 4.5
  },
  {
    "id": "H002",
    "name": "Sunset Resort",
    "location": "New York",
    "pricePerNight": 250.0,
    "rating": 4.8
  },
  {
    "id": "H003",
    "name": "Budget Inn",
    "location": "New York",
    "pricePerNight": 90.0,
    "rating": 3.9
  }
]
```

### Car Rental Search

```bash
$ curl -s "http://localhost:8080/cars/search?location=Los+Angeles" | python3 -m json.tool
```

**Example response:**
```json
[
  {
    "id": "C001",
    "model": "Toyota Camry",
    "location": "Los Angeles",
    "pricePerDay": 55.0,
    "type": "Sedan"
  },
  {
    "id": "C002",
    "model": "Ford Explorer",
    "location": "Los Angeles",
    "pricePerDay": 85.0,
    "type": "SUV"
  },
  {
    "id": "C003",
    "model": "Tesla Model 3",
    "location": "Los Angeles",
    "pricePerDay": 120.0,
    "type": "Electric"
  }
]
```

---

## Project Structure

```
travel-booking-system/
├── build.gradle                          # Root build config (shared deps, plugins)
├── settings.gradle                       # Module declarations
├── gradlew / gradlew.bat                 # Gradle wrapper scripts
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties    # Gradle 6.9.4 distribution URL
│
├── discovery-service/                   # Eureka server
│   ├── build.gradle
│   └── src/main/
│       ├── java/.../DiscoveryServiceApplication.java
│       └── resources/application.yml    # port: 8761
│
├── api-gateway/                         # Zuul proxy + routing rules
│   ├── build.gradle
│   └── src/main/
│       ├── java/.../ApiGatewayApplication.java
│       └── resources/application.yml    # port: 8080, zuul routes
│
├── flight-service/                      # Flight search
│   ├── build.gradle
│   └── src/main/
│       ├── java/.../
│       │   ├── FlightServiceApplication.java
│       │   ├── controller/FlightController.java
│       │   ├── model/Flight.java
│       │   └── service/FlightService.java
│       └── resources/application.yml    # port: 8081, rapidapi config
│
├── hotel-service/                       # Hotel search
│   ├── build.gradle
│   └── src/main/
│       ├── java/.../
│       │   ├── HotelServiceApplication.java
│       │   ├── controller/HotelController.java
│       │   ├── model/Hotel.java
│       │   └── service/HotelService.java
│       └── resources/application.yml    # port: 8082
│
└── car-rental-service/                  # Car rental search
    ├── build.gradle
    └── src/main/
        ├── java/.../
        │   ├── CarRentalServiceApplication.java
        │   ├── controller/CarRentalController.java
        │   ├── model/Car.java
        │   └── service/CarRentalService.java
        └── resources/application.yml    # port: 8083
```

---

## Configuration

### Changing Service Ports

Each service has its port defined in `<service>/src/main/resources/application.yml`:

```yaml
server:
  port: 8081   # Change this
```

If you change a business service's port, no other configuration needs updating — Eureka handles discovery dynamically. If you change the Eureka server port (default `8761`), update `eureka.client.service-url.defaultZone` in every other service's `application.yml`.

### Eureka Server URL

All client services point to the discovery server via:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

For a non-local deployment, replace `localhost:8761` with the actual Eureka host.

### Zuul Routing Rules

The gateway routing is defined in `api-gateway/src/main/resources/application.yml`:

```yaml
zuul:
  routes:
    flights:
      path: /flights/**
      serviceId: FLIGHT-SERVICE
      strip-prefix: false
```

- `path`: the pattern Zuul matches on incoming requests
- `serviceId`: the Spring application name of the target service (case-insensitive; Eureka registers services in uppercase)
- `strip-prefix: false`: the matched path prefix is **not** removed before forwarding. With this set to `true`, a request to `/flights/search` would be forwarded as `/search`; with `false` it's forwarded as `/flights/search`, which matches the controller's `@RequestMapping("/flights")` + `@GetMapping("/search")`

---

## Development Notes

### Why Netflix Zuul and Eureka (not Spring Cloud Gateway)?

Zuul 1 and the Netflix discovery stack were deprecated in Spring Cloud 2020.0.x in favor of Spring Cloud Gateway and Spring Cloud LoadBalancer. This project uses Hoxton.SR12 (the last Hoxton release) to keep the full Netflix OSS stack intact for educational purposes — it demonstrates the pattern that the newer stack was designed to replace, which is useful context when working in organizations that haven't migrated.

Migrating to Spring Cloud Gateway would require:
- Upgrading to Spring Boot 2.4+ and Spring Cloud 2020.0.x+
- Replacing the `@EnableZuulProxy` bootstrap with a `RouteLocator` bean or `application.yml` gateway routes
- Replacing Ribbon load balancing with Spring Cloud LoadBalancer

### Flight Service: RapidAPI Integration and Mock Fallback

`FlightService` attempts a live call to the Skyscanner API via [RapidAPI](https://rapidapi.com/apiheya/api/sky-scrapper) on every request. If the API key is invalid, the network is unavailable, or the response structure doesn't contain an `itineraries` node, the service logs a warning and returns a static list of three mock flights. This means the service is always functional regardless of external API availability.

To use a live API key, update `rapidapi.flight.key` in `flight-service/src/main/resources/application.yml`. **Do not commit a valid API key to version control** — move it to an environment variable instead:

```yaml
rapidapi:
  flight:
    key: ${RAPIDAPI_KEY:your-key-here}
```

### Hotel and Car Rental Services

Both services currently return hardcoded mock data. They are structured identically to `flight-service` (controller → service → model) to make replacing the mock with a real API integration straightforward — only the service class needs to change.

---

## Troubleshooting

### Port already in use

```
Web server failed to start. Port XXXX was already in use.
```

Find and kill the process using the port:
```bash
$ lsof -ti :8080 | xargs kill -9
```

### Java version mismatch

```
Unsupported class file major version 65  (or higher)
```

Your active JDK is Java 21+ (version 65 = Java 21, 69 = Java 25). Gradle 6.9.4 requires Java ≤ 16. Install Java 11 and either set `JAVA_HOME` or add `org.gradle.java.home` to `gradle.properties` (see [Prerequisites](#prerequisites)).

### Gradle daemon issues

If builds hang or produce unexpected errors after a JDK change:
```bash
$ ./gradlew --stop     # Stop all running daemons
$ ./gradlew clean build
```

### Services not appearing in Eureka

**Symptom:** Eureka dashboard shows no registered instances, or gateway returns `500 / No instances available`.

**Checklist:**
1. Confirm the discovery service started fully before business services (watch for `Started DiscoveryServiceApplication`)
2. Eureka has a heartbeat grace period — wait up to **90 seconds** after a service starts before it appears in the registry
3. Verify each service's `application.yml` points to the correct Eureka URL (`http://localhost:8761/eureka/`)
4. Check that no firewall or VPN is blocking `localhost` loopback connections

### Gateway returns 404 but service works on its direct port

Zuul routes by service ID. If the gateway starts before the business services register with Eureka, it will not find them. Restart the gateway after all business services are up, or wait for Eureka's registry refresh cycle (~30s) and retry.

---

## Testing

### Test a service directly (bypassing the gateway)

```bash
# Flight service — direct
$ curl "http://localhost:8081/flights/search?origin=JFK&destination=LAX&date=2026-05-15"

# Hotel service — direct
$ curl "http://localhost:8082/hotels/search?location=Paris"

# Car rental service — direct
$ curl "http://localhost:8083/cars/search?location=Miami"
```

### Test through the gateway

```bash
# All three through port 8080
$ curl "http://localhost:8080/flights/search?origin=LHR&destination=CDG&date=2026-06-01"
$ curl "http://localhost:8080/hotels/search?location=London"
$ curl "http://localhost:8080/cars/search?location=London"
```

If the direct-port request succeeds but the gateway request fails, the issue is service registration (see [Troubleshooting](#troubleshooting)).

### Verify Eureka registration

```bash
# List all registered apps as JSON
$ curl -s -H "Accept: application/json" http://localhost:8761/eureka/apps | python3 -m json.tool | grep '"app"'
```

---

## Contributing

1. Fork the repository and create a feature branch from `main`
2. Keep each service's `build.gradle` minimal — shared configuration belongs in the root `build.gradle` under `subprojects {}`
3. If adding a new service, register it in `settings.gradle` and add a Zuul route to `api-gateway/src/main/resources/application.yml`
4. Test startup order manually before submitting a PR

---

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

---

## Acknowledgments

- **Qwasar Silicon Valley** — academic project context and microservices assignment structure
- **Spring Cloud Netflix** — Eureka and Zuul documentation and reference implementations
- **Netflix OSS** — original open-source implementations of Eureka and Zuul
- **RapidAPI / Sky Scrapper** — flight search API used in the flight service integration attempt
