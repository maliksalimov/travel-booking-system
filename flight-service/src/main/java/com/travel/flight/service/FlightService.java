package com.travel.flight.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.flight.model.Flight;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class FlightService {
    @Value("${booking.api.key}")
    private String apiKey;

    @Value("${booking.api.host}")
    private String apiHost;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cache resolved IATA codes so repeated searches don't re-derive them
    private final Map<String, String> airportIdCache = new ConcurrentHashMap<>();

    public List<Flight> searchFlights(String origin, String destination, String date) {
        long totalStart = System.currentTimeMillis();
        try {
            String fromId = toAirportId(origin);
            String toId = toAirportId(destination);
            System.out.printf("[flights] resolved IDs: from=%s to=%s%n", fromId, toId);

            HttpUrl url = Objects.requireNonNull(HttpUrl.parse("https://" + apiHost + "/api/v1/flights/searchFlights"))
                    .newBuilder()
                    .addQueryParameter("fromId", fromId)
                    .addQueryParameter("toId", toId)
                    .addQueryParameter("departDate", date)
                    .addQueryParameter("adults", "1")
                    .addQueryParameter("children", "0")
                    .addQueryParameter("cabinClass", "ECONOMY")
                    .addQueryParameter("currency_code", "USD")
                    .build();

            System.out.println("[flights] search URL: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("x-rapidapi-key", apiKey)
                    .addHeader("x-rapidapi-host", apiHost)
                    .build();

            long t1 = System.currentTimeMillis();
            try (Response response = client.newCall(request).execute()) {
                System.out.printf("[flights] search call: %dms — HTTP %d%n",
                        System.currentTimeMillis() - t1, response.code());

                String responseBody = Objects.requireNonNull(response.body()).string();
                System.out.println("[flights] response (first 500): " +
                        responseBody.substring(0, Math.min(responseBody.length(), 500)));

                if (!response.isSuccessful()) {
                    System.err.println("[flights] ERROR HTTP " + response.code());
                    return getMockFlights(origin, destination);
                }

                JsonNode root = objectMapper.readTree(responseBody);

                // Real response uses data.flightOffers — NOT data.itineraries
                if (!root.has("data") || !root.path("data").has("flightOffers")) {
                    System.out.println("[flights] WARNING: data.flightOffers missing in response — using mock data");
                    System.out.println("[flights] data keys: " + root.path("data").fieldNames());
                    return getMockFlights(origin, destination);
                }

                JsonNode flightOffers = root.path("data").path("flightOffers");
                List<Flight> flights = new ArrayList<>();

                flightOffers.forEach(offer -> {
                    try {
                        JsonNode segments = offer.path("segments");
                        JsonNode firstSeg = segments.get(0);
                        JsonNode lastSeg = segments.get(segments.size() - 1);

                        String originCode = firstSeg.path("departureAirport").path("code").asText();
                        String destCode = lastSeg.path("arrivalAirport").path("code").asText();
                        String departure = firstSeg.path("departureTime").asText();
                        String arrival = lastSeg.path("arrivalTime").asText();

                        // Airline from first leg of first segment
                        String airline = firstSeg.path("legs").get(0)
                                .path("carriersData").get(0)
                                .path("name").asText();

                        JsonNode priceTotal = offer.path("priceBreakdown").path("total");
                        long units = priceTotal.path("units").asLong();
                        long nanos = priceTotal.path("nanos").asLong();
                        double priceRaw = units + nanos / 1_000_000_000.0;
                        String currency = priceTotal.path("currencyCode").asText("USD");
                        String priceFormatted = String.format("%s %.2f", currency, priceRaw);

                        String flightId = offer.path("flightKey").asText(originCode + "-" + destCode);

                        flights.add(new Flight(
                                flightId, originCode, destCode,
                                departure, arrival, airline,
                                priceRaw, priceFormatted
                        ));
                    } catch (Exception e) {
                        System.err.println("[flights] skipping offer: " + e.getMessage());
                    }
                });

                System.out.printf("[flights] total: %dms — %d results%n",
                        System.currentTimeMillis() - totalStart, flights.size());
                return flights.isEmpty() ? getMockFlights(origin, destination) : flights;
            }

        } catch (Exception e) {
            System.err.printf("[flights] EXCEPTION after %dms: %s%n",
                    System.currentTimeMillis() - totalStart, e.getMessage());
            e.printStackTrace();
            return getMockFlights(origin, destination);
        }
    }

    // booking-com15 /searchAirport endpoint does not exist, so city→IATA is resolved locally.
    private static final Map<String, String> CITY_TO_IATA = Map.ofEntries(
        Map.entry("baku",          "GYD"),
        Map.entry("istanbul",      "IST"),
        Map.entry("ankara",        "ESB"),
        Map.entry("london",        "LHR"),
        Map.entry("paris",         "CDG"),
        Map.entry("madrid",        "MAD"),
        Map.entry("barcelona",     "BCN"),
        Map.entry("rome",          "FCO"),
        Map.entry("milan",         "MXP"),
        Map.entry("berlin",        "BER"),
        Map.entry("frankfurt",     "FRA"),
        Map.entry("munich",        "MUC"),
        Map.entry("amsterdam",     "AMS"),
        Map.entry("brussels",      "BRU"),
        Map.entry("vienna",        "VIE"),
        Map.entry("zurich",        "ZRH"),
        Map.entry("lisbon",        "LIS"),
        Map.entry("athens",        "ATH"),
        Map.entry("prague",        "PRG"),
        Map.entry("budapest",      "BUD"),
        Map.entry("warsaw",        "WAW"),
        Map.entry("stockholm",     "ARN"),
        Map.entry("oslo",          "OSL"),
        Map.entry("copenhagen",    "CPH"),
        Map.entry("helsinki",      "HEL"),
        Map.entry("dublin",        "DUB"),
        Map.entry("kyiv",          "KBP"),
        Map.entry("kiev",          "KBP"),
        Map.entry("moscow",        "SVO"),
        Map.entry("tbilisi",       "TBS"),
        Map.entry("yerevan",       "EVN"),
        Map.entry("dubai",         "DXB"),
        Map.entry("abu dhabi",     "AUH"),
        Map.entry("doha",          "DOH"),
        Map.entry("riyadh",        "RUH"),
        Map.entry("cairo",         "CAI"),
        Map.entry("new york",      "JFK"),
        Map.entry("los angeles",   "LAX"),
        Map.entry("chicago",       "ORD"),
        Map.entry("miami",         "MIA"),
        Map.entry("atlanta",       "ATL"),
        Map.entry("boston",        "BOS"),
        Map.entry("san francisco", "SFO"),
        Map.entry("seattle",       "SEA"),
        Map.entry("toronto",       "YYZ"),
        Map.entry("vancouver",     "YVR"),
        Map.entry("mexico city",   "MEX"),
        Map.entry("sao paulo",     "GRU"),
        Map.entry("buenos aires",  "EZE"),
        Map.entry("tokyo",         "NRT"),
        Map.entry("beijing",       "PEK"),
        Map.entry("shanghai",      "PVG"),
        Map.entry("hong kong",     "HKG"),
        Map.entry("singapore",     "SIN"),
        Map.entry("bangkok",       "BKK"),
        Map.entry("kuala lumpur",  "KUL"),
        Map.entry("delhi",         "DEL"),
        Map.entry("mumbai",        "BOM"),
        Map.entry("sydney",        "SYD"),
        Map.entry("melbourne",     "MEL"),
        Map.entry("seoul",         "ICN"),
        Map.entry("johannesburg",  "JNB"),
        Map.entry("nairobi",       "NBO"),
        Map.entry("lagos",         "LOS")
    );

    /**
     * Converts user input to the airport ID format booking-com15 requires (e.g. GYD.AIRPORT).
     * Accepts: IATA codes ("GYD", "MAD"), city names ("Baku", "Madrid"),
     * or already-formatted IDs ("GYD.AIRPORT").
     */
    private String toAirportId(String input) {
        if (input == null || input.isBlank()) return input;
        String key = input.trim().toLowerCase();
        String cached = airportIdCache.get(key);
        if (cached != null) return cached;

        String id;
        if (key.contains(".")) {
            // Already in API format: "GYD.AIRPORT"
            id = input.trim().toUpperCase();
        } else {
            String iata = CITY_TO_IATA.get(key);
            if (iata != null) {
                // Matched a city name: "baku" → "GYD"
                id = iata + ".AIRPORT";
            } else {
                // Assume the user typed an IATA code directly: "GYD" → "GYD.AIRPORT"
                id = input.trim().toUpperCase() + ".AIRPORT";
            }
        }

        airportIdCache.put(key, id);
        System.out.println("[flights] airport ID: \"" + input + "\" → " + id);
        return id;
    }

    private List<Flight> getMockFlights(String origin, String destination) {
        return List.of(
                new Flight("MOCK001", origin, destination, "2026-05-15T08:00", "2026-05-15T11:30", "United Airlines", 450.0, "$450"),
                new Flight("MOCK002", origin, destination, "2026-05-15T14:00", "2026-05-15T17:30", "Delta", 380.0, "$380"),
                new Flight("MOCK003", origin, destination, "2026-05-15T20:00", "2026-05-15T23:30", "American", 520.0, "$520")
        );
    }
}
