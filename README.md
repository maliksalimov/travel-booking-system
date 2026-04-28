# Travel Booking System

A production-grade microservices travel booking platform integrating real-time flight and hotel data from the Booking.com API, routed through a unified API gateway and served from a dark-themed web dashboard.

[![Build Status](https://app.travis-ci.com/maliksalimov/travel-booking-system.svg?branch=main)](https://app.travis-ci.com/maliksalimov/travel-booking-system)
[![Java](https://img.shields.io/badge/Java-11-orange?logo=openjdk)](https://adoptium.net/temurin/releases/?version=11)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.3.12-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-Hoxton.SR12-brightgreen?logo=spring)](https://spring.io/projects/spring-cloud)
[![Gradle](https://img.shields.io/badge/Gradle-6.9.4-blue?logo=gradle)](https://gradle.org)
[![License](https://img.shields.io/badge/License-MIT-lightgrey)](LICENSE)

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [API Integration Status](#api-integration-status)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Quick Start — Docker Compose](#quick-start--docker-compose)
- [Manual Setup](#manual-setup)
- [Configuration](#configuration)
- [Frontend Dashboard](#frontend-dashboard)
- [API Reference](#api-reference)
- [Project Structure](#project-structure)
- [Testing](#testing)
- [CI/CD Pipeline](#cicd-pipeline)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

This project implements a travel search backend using the Spring Cloud Netflix stack. A Zuul API gateway acts as the single entry point, routing requests to three domain microservices via Eureka-based service discovery. All services are independently deployable and containerised with Docker.

**What works today:**

| Service | Data Source | Status |
|---|---|---|
| Hotels | Booking.com API (booking-com15.p.rapidapi.com) | ✅ Live data |
| Flights | Booking.com API with IATA code resolution | ✅ Live data |
| Cars | Booking.com API | ⚠️ Upstream API returns server error |

---

## Architecture

```
                Browser / API Client
                        │
                        ▼
          ┌─────────────────────────┐
          │   Frontend Dashboard    │
          │   frontend/index.html   │
          │   Dark-themed SPA       │
          └────────────┬────────────┘
                       │ HTTP → localhost:8080
                       ▼
          ┌─────────────────────────┐
          │      API Gateway        │
          │    Zuul Proxy :8080     │
          │  /flights/** → FLIGHT   │
          │  /hotels/**  → HOTEL    │
          │  /cars/**    → CAR      │
          └────────────┬────────────┘
                       │ resolves via
                       ▼
          ┌─────────────────────────┐
          │   Eureka Discovery      │
          │       :8761             │
          └──────┬──────┬──────┬───┘
                 │      │      │
        ┌────────▼──┐ ┌─▼──────┴──┐ ┌──────────────┐
        │  Flight   │ │   Hotel   │ │  Car Rental  │
        │ Svc :8081 │ │ Svc :8082 │ │  Svc  :8083  │
        └─────┬─────┘ └─────┬─────┘ └──────┬───────┘
              │             │              │
              └─────────────┴──────────────┘
                            │
                            ▼
                  Booking.com RapidAPI
              booking-com15.p.rapidapi.com
```

### Request Flow

```
GET localhost:8080/flights/search?origin=GYD&destination=MAD&date=2026-06-01
  → Zuul matches /flights/**
  → Eureka resolves FLIGHT-SERVICE → localhost:8081
  → FlightService resolves "GYD" → "GYD.AIRPORT"
  → Calls booking-com15.p.rapidapi.com/api/v1/flights/searchFlights
  → Parses data.flightOffers[]
  → Returns JSON array of real flights to client
```

---

## API Integration Status

### Hotels ✅ Live

Calls `/api/v1/hotels/searchDestination` to convert a city name to a numeric `dest_id`, then calls `/api/v1/hotels/searchHotels`. Returns real hotel names, IDs, prices and review scores from Booking.com.

```bash
curl "http://localhost:8080/hotels/search?location=New%20York&arrivalDate=2026-06-01&departureDate=2026-06-05"
```

### Flights ✅ Live

Accepts IATA airport codes (e.g. `GYD`, `MAD`) **or** common city names (e.g. `Baku`, `Madrid`) — a 60-city lookup map resolves city names to IATA codes. Calls `/api/v1/flights/searchFlights` and parses `data.flightOffers[]`.

```bash
curl "http://localhost:8080/flights/search?origin=GYD&destination=MAD&date=2026-06-01"
# or
curl "http://localhost:8080/flights/search?origin=Baku&destination=Madrid&date=2026-06-01"
```

### Cars ⚠️ Unavailable

The `/api/v1/cars/searchCarRentals` endpoint on booking-com15 consistently returns `{"status":false,"message":"Something went wrong"}` for all coordinate inputs and date formats. The service returns an empty list and the frontend displays a clear error message rather than fake data. This is an upstream API issue unrelated to the application code.

---

## Technology Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 11 |
| Framework | Spring Boot | 2.3.12.RELEASE |
| Cloud | Spring Cloud Netflix (Hoxton) | Hoxton.SR12 |
| Service Discovery | Netflix Eureka | — |
| API Gateway | Netflix Zuul | — |
| HTTP Client | OkHttp3 | 4.9.3 |
| JSON | Jackson Databind | (managed) |
| Build | Gradle multi-module | 6.9.4 |
| Containerisation | Docker + Docker Compose | — |
| CI/CD | Travis CI | — |
| Code Coverage | JaCoCo | 0.8.11 |
| Testing | JUnit 5 | — |
| External API | Booking.com via RapidAPI | — |

---

## Prerequisites

### Java 11

Gradle 6.9.4 supports Java 8–16 only. **Java 17+ will not work.**

```bash
# macOS
brew install --cask temurin11

# Verify
java -version
# Expected: openjdk version "11.x.x"
```

If you have multiple JDKs, pin Gradle to Java 11 by creating `gradle.properties` in the project root:

```properties
org.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-11.jdk/Contents/Home
```

Find the correct path: `/usr/libexec/java_home -v 11`

### Docker (for containerised deployment)

```bash
docker --version          # Docker 20+
docker-compose --version  # Compose 1.29+
```

### Port availability

```bash
lsof -i :8761,8080,8081,8082,8083
# No output = all ports free
```

---

## Quick Start — Docker Compose

The fastest way to run the entire stack:

**1. Clone and configure**

```bash
git clone https://github.com/maliksalimov/travel-booking-system.git
cd travel-booking-system
cp .env.example .env
# Edit .env and set your Booking.com RapidAPI key:
#   BOOKING_API_KEY=your_key_here
```

**2. Build JARs**

```bash
./gradlew clean build -x test
```

**3. Start all services**

```bash
docker-compose up --build
```

Services start in dependency order: Eureka → business services → gateway.  
Wait for the gateway to print `Started ApiGatewayApplication` (~60 s total).

**4. Open the dashboard**

Open `frontend/index.html` in your browser — or open `http://localhost:8080` if you serve the frontend.

**5. Stop**

```bash
docker-compose down
```

---

## Manual Setup

Use this approach for development or when Docker is not available.

### 1. Clone and build

```bash
git clone https://github.com/maliksalimov/travel-booking-system.git
cd travel-booking-system
chmod +x gradlew
./gradlew clean build
```

### 2. Set API credentials (optional)

The API key ships as a fallback in `application.yml`. To override it, export the environment variable before starting each service:

```bash
export BOOKING_API_KEY=your_rapidapi_key_here
```

### 3. Start services in order

Open five terminal windows:

```bash
# Terminal 1 — Eureka (start first, wait ~30 s)
cd discovery-service && ../gradlew bootRun

# Terminal 2 — Flight service
cd flight-service && ../gradlew bootRun

# Terminal 3 — Hotel service
cd hotel-service && ../gradlew bootRun

# Terminal 4 — Car rental service
cd car-rental-service && ../gradlew bootRun

# Terminal 5 — API Gateway (start last)
cd api-gateway && ../gradlew bootRun
```

### 4. Verify registration

```bash
curl -s -H "Accept:application/json" http://localhost:8761/eureka/apps \
  | python3 -m json.tool | grep '"app"'
```

Expected:
```
"app": "FLIGHT-SERVICE",
"app": "HOTEL-SERVICE",
"app": "CAR-RENTAL-SERVICE",
"app": "API-GATEWAY",
```

---

## Configuration

### Environment Variables

| Variable | Default (in yml) | Description |
|---|---|---|
| `BOOKING_API_KEY` | *(hardcoded fallback)* | RapidAPI key for booking-com15.p.rapidapi.com |
| `BOOKING_API_HOST` | `booking-com15.p.rapidapi.com` | RapidAPI host header |

Set in a `.env` file (excluded from git) or export before running:

```bash
export BOOKING_API_KEY=your_key_here
export BOOKING_API_HOST=booking-com15.p.rapidapi.com
```

**`.env.example`** — copy this to `.env` and fill in your key:

```bash
BOOKING_API_KEY=your_rapidapi_key_here
```

### application.yml pattern (all three services)

```yaml
booking:
  api:
    key: ${BOOKING_API_KEY:fallback-key}
    host: ${BOOKING_API_HOST:booking-com15.p.rapidapi.com}
```

### Gateway timeouts (api-gateway/src/main/resources/application.yml)

```yaml
ribbon:
  ConnectTimeout: 5000
  ReadTimeout: 20000

hystrix:
  command:
    default:
      execution:
        isolation:
          thread:
            timeoutInMilliseconds: 30000
```

These values accommodate two chained API calls (destination lookup + main search) which can take up to 15 s combined on a cold start.

### Zuul routes

```yaml
zuul:
  routes:
    flights:
      path: /flights/**
      serviceId: FLIGHT-SERVICE
      strip-prefix: false
    hotels:
      path: /hotels/**
      serviceId: HOTEL-SERVICE
      strip-prefix: false
    cars:
      path: /cars/**
      serviceId: CAR-RENTAL-SERVICE
      strip-prefix: false
```

`strip-prefix: false` forwards the full path unchanged so the controllers' `@RequestMapping` annotations match without modification.

---

## Frontend Dashboard

A single-page HTML/JS/CSS application in `frontend/`.

**Open:** `frontend/index.html` directly in a browser. No build step required.

**Tabs:**

| Tab | How to search |
|---|---|
| ✈️ Flights | Enter IATA codes (`GYD`, `MAD`) **or** city names (`Baku`, `Madrid`) + date |
| 🏨 Hotels | Enter any city name (`New York`, `Paris`) + arrival/departure dates |
| 🚗 Cars | Enter a city name + pick-up/drop-off date and time |

The city→IATA lookup covers 60 airports. For unlisted cities enter the 3-letter IATA code directly.

**City → IATA coverage (sample):**

| City | Code | City | Code |
|---|---|---|---|
| Baku | GYD | London | LHR |
| Madrid | MAD | Paris | CDG |
| Istanbul | IST | New York | JFK |
| Dubai | DXB | Los Angeles | LAX |
| Tokyo | NRT | Singapore | SIN |

---

## API Reference

All requests go through the gateway at `http://localhost:8080`.

### `GET /flights/search`

| Parameter | Type | Required | Example |
|---|---|---|---|
| `origin` | string | ✅ | `GYD` or `Baku` |
| `destination` | string | ✅ | `MAD` or `Madrid` |
| `date` | `YYYY-MM-DD` | ✅ | `2026-06-01` |

```bash
curl -s "http://localhost:8080/flights/search?origin=GYD&destination=MAD&date=2026-06-01" \
  | python3 -m json.tool
```

**Response:**
```json
[
  {
    "id": "1_VF578_VF605.GYD20260601",
    "origin": "GYD",
    "destination": "MAD",
    "departureTime": "2026-06-01T16:25:00",
    "arrivalTime": "2026-06-02T16:05:00",
    "airline": "AJET",
    "price": 182.53,
    "currency": "USD 182.53"
  }
]
```

### `GET /hotels/search`

| Parameter | Type | Required | Default | Example |
|---|---|---|---|---|
| `location` | string | ✅ | — | `New York` |
| `arrivalDate` | `YYYY-MM-DD` | ❌ | `2026-05-01` | `2026-06-01` |
| `departureDate` | `YYYY-MM-DD` | ❌ | `2026-05-05` | `2026-06-05` |

```bash
curl -s "http://localhost:8080/hotels/search?location=New%20York&arrivalDate=2026-06-01&departureDate=2026-06-05" \
  | python3 -m json.tool
```

**Response:**
```json
[
  {
    "id": "14142501",
    "name": "The Peninsula New York",
    "location": "New York",
    "pricePerNight": 875.0,
    "rating": 9.2
  }
]
```

### `GET /cars/search`

| Parameter | Type | Required | Example |
|---|---|---|---|
| `location` | string | ✅ | `London` |
| `pickUpLat` | double | ✅ | `51.5074` |
| `pickUpLon` | double | ✅ | `-0.1278` |
| `dropOffLat` | double | ✅ | `51.5074` |
| `dropOffLon` | double | ✅ | `-0.1278` |
| `pickUpTime` | `YYYY-MM-DDTHH:MM:SS` | ✅ | `2026-06-01T10:00:00` |
| `dropOffTime` | `YYYY-MM-DDTHH:MM:SS` | ✅ | `2026-06-05T10:00:00` |

> **Note:** The upstream car rental API is currently returning a server error for all requests. The service returns `[]` and the frontend displays an unavailability message.

---

## Project Structure

```
travel-booking-system/
├── build.gradle                # Root: shared plugins, deps, JaCoCo config
├── settings.gradle             # Declares all 5 sub-modules
├── gradlew / gradlew.bat       # Gradle wrapper
├── docker-compose.yml          # Orchestrates all 5 services
├── .travis.yml                 # CI: build → test → jacocoTestReport → docker build
├── .env.example                # Template for local credentials
├── .gitignore                  # Excludes .env, build/, .gradle/, .idea/
│
├── frontend/
│   ├── index.html              # Single-page dashboard (3 tabs)
│   ├── app.js                  # Fetch calls + display logic + city→coords map
│   └── style.css               # Dark theme, responsive layout
│
├── discovery-service/          # Eureka server :8761
│   ├── Dockerfile
│   ├── build.gradle
│   └── src/main/resources/application.yml
│
├── api-gateway/                # Zuul proxy :8080
│   ├── Dockerfile
│   ├── build.gradle
│   └── src/main/
│       ├── java/.../ApiGatewayApplication.java   (@EnableZuulProxy)
│       ├── java/.../CorsConfig.java
│       └── resources/application.yml             (routes + timeouts)
│
├── flight-service/             # Booking.com flights :8081
│   ├── Dockerfile
│   ├── build.gradle
│   └── src/
│       ├── main/java/.../
│       │   ├── controller/FlightController.java
│       │   ├── model/Flight.java
│       │   └── service/FlightService.java        (CITY_TO_IATA map, flightOffers parser)
│       └── test/java/.../FlightServiceTest.java
│
├── hotel-service/              # Booking.com hotels :8082
│   ├── Dockerfile
│   ├── build.gradle
│   └── src/
│       ├── main/java/.../
│       │   ├── controller/HotelController.java
│       │   ├── model/Hotel.java
│       │   └── service/HotelService.java         (searchDestination → searchHotels)
│       └── test/java/.../HotelServiceTest.java
│
└── car-rental-service/         # Car rentals :8083
    ├── Dockerfile
    ├── build.gradle
    └── src/
        ├── main/java/.../
        │   ├── controller/CarRentalController.java
        │   ├── model/Car.java
        │   └── service/CarRentalService.java      (returns [] on upstream error)
        └── test/java/.../CarRentalServiceTest.java
```

---

## Testing

### Run all tests

```bash
./gradlew test
```

### Run tests with coverage report

```bash
./gradlew test jacocoTestReport
```

HTML reports land in each module's `build/reports/jacoco/test/html/index.html`.

### Run a single module's tests

```bash
./gradlew :hotel-service:test
./gradlew :flight-service:test
./gradlew :car-rental-service:test
```

### Test a service directly (bypassing the gateway)

```bash
# Flight service — direct port
curl "http://localhost:8081/flights/search?origin=GYD&destination=MAD&date=2026-06-01"

# Hotel service — direct port
curl "http://localhost:8082/hotels/search?location=Paris&arrivalDate=2026-06-01&departureDate=2026-06-05"

# Car rental — direct port
curl "http://localhost:8083/cars/search?location=London&pickUpLat=51.5074&pickUpLon=-0.1278&dropOffLat=51.5074&dropOffLon=-0.1278&pickUpTime=2026-06-01T10:00:00&dropOffTime=2026-06-05T10:00:00"
```

If a direct call succeeds but the gateway call fails, the problem is service registration — see [Troubleshooting](#troubleshooting).

---

## CI/CD Pipeline

Travis CI runs on every push:

```yaml
# .travis.yml
install:
  - ./gradlew clean build -x test   # Compile all modules

script:
  - ./gradlew test jacocoTestReport  # Run tests + generate coverage
  - docker-compose build             # Verify all Docker images build

after_success:
  - bash <(curl -s https://codecov.io/bash)  # Upload coverage to Codecov
```

**Pipeline stages:**

```
Push to GitHub
    │
    ▼
Travis CI
    ├─ chmod +x gradlew
    ├─ ./gradlew clean build -x test      (compile)
    ├─ ./gradlew test jacocoTestReport    (test + coverage)
    ├─ docker-compose build               (image build verification)
    └─ Upload to Codecov (on success)
```

---

## Troubleshooting

### Port already in use

```
Web server failed to start. Port XXXX was already in use.
```

```bash
lsof -ti :8080 | xargs kill -9
```

### Java version mismatch

```
Unsupported class file major version 65
```

Version 65 = Java 21, which Gradle 6.9.4 does not support. Install Java 11 and pin it:

```bash
/usr/libexec/java_home -v 11
# Returns e.g. /Library/Java/JavaVirtualMachines/temurin-11.jdk/Contents/Home
```

Add to `gradle.properties`:
```properties
org.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-11.jdk/Contents/Home
```

### Services not appearing in Eureka

**Checklist:**
1. Eureka must be fully started before business services (watch for `Started DiscoveryServiceApplication`)
2. Eureka has a 90-second heartbeat grace period — wait before checking the dashboard
3. All services must point to the same `eureka.client.service-url.defaultZone`
4. With Docker Compose, services use `http://discovery-service:8761/eureka/` (hostname, not `localhost`)

### Gateway returns 504 Gateway Timeout

The Booking.com API makes two sequential calls per request (location lookup + search). If either takes longer than 20 s, Hystrix cuts the circuit. Check service logs for `[flights]` or `[hotels]` timing lines and verify your internet connection.

### Gateway returns 404 but service works on direct port

Zuul routes by Eureka service ID. If the gateway started before a service registered, it won't route to it. Either restart the gateway, or wait up to 30 s for the Eureka registry refresh cycle.

### Flights return MOCK001 / MOCK002 / MOCK003

This means `searchFlights` failed. Common causes:
- Invalid airport ID: enter a 3-letter IATA code (`GYD`) or a city from the [lookup table](#frontend-dashboard)
- Date is in the past or the route has no available flights for that date
- Network blocked or API key invalid — check service logs for `[flights] ERROR HTTP` lines

### Hotels return real data but wrong location

The hotel service resolves city names through `/api/v1/hotels/searchDestination`. If the API returns a region instead of a city, the hotels shown may be in the wider metro area. Use well-known city names (`"New York"`, `"London"`) for best results.

---

## Contributing

1. Fork the repository and create a feature branch from `main`
2. Keep each service's `build.gradle` minimal — shared config belongs in root `build.gradle` under `subprojects {}`
3. New services must be registered in `settings.gradle` and have a Zuul route in `api-gateway/src/main/resources/application.yml`
4. Do not commit API keys — use environment variables with yml fallbacks
5. Test startup order manually before submitting a PR

---

## License

This project is licensed under the MIT License.

---

## Author

**Malik Salimov** — [github.com/maliksalimov](https://github.com/maliksalimov)
