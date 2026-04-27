package com.travel.flight.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.flight.model.Flight;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FlightService {
    @Value("${rapidapi.flight.key}")
    private String apiKey;

    @Value("${rapidapi.flight.host}")
    private String apiHost;

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Flight> searchFlights(String origin, String destination, String date) {
        try {
            String url = String.format(
                    "https://%s/api/v1/flights/searchFlights?originSkyId=%s&destinationSkyId=%s&date=%s",
                    apiHost, origin, destination, date
            );

            System.out.println("=== REQUEST URL ===");
            System.out.println(url);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("x-rapidapi-key", apiKey)
                    .addHeader("x-rapidapi-host", apiHost)
                    .build();

            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();

            System.out.println("=== API RESPONSE ===");
            System.out.println(responseBody);
            System.out.println("====================");

            JsonNode root = objectMapper.readTree(responseBody);

            if (!root.has("data") || !root.path("data").has("itineraries")) {
                System.out.println("WARNING: No itineraries in response, using mock data");
                return getMockFlights(origin, destination);
            }

            JsonNode itineraries = root.path("data").path("itineraries");
            List<Flight> flights = new ArrayList<>();

            itineraries.forEach(itinerary -> {
                JsonNode leg = itinerary.path("legs").get(0);
                Flight flight = new Flight(
                        leg.path("id").asText(),
                        leg.path("origin").path("displayCode").asText(),
                        leg.path("destination").path("displayCode").asText(),
                        leg.path("departure").asText(),
                        leg.path("arrival").asText(),
                        leg.path("carriers").path("marketing").get(0).path("name").asText(),
                        itinerary.path("price").path("raw").asDouble(),
                        itinerary.path("price").path("formatted").asText()
                );
                flights.add(flight);
            });

            return flights;

        } catch (Exception e) {
            System.err.println("ERROR calling API: " + e.getMessage());
            e.printStackTrace();
            return getMockFlights(origin, destination);
        }
    }

    private List<Flight> getMockFlights(String origin, String destination) {
        return List.of(
                new Flight("MOCK001", origin, destination, "2026-05-15T08:00", "2026-05-15T11:30", "United Airlines", 450.0, "$450"),
                new Flight("MOCK002", origin, destination, "2026-05-15T14:00", "2026-05-15T17:30", "Delta", 380.0, "$380"),
                new Flight("MOCK003", origin, destination, "2026-05-15T20:00", "2026-05-15T23:30", "American", 520.0, "$520")
        );
    }
}