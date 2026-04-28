package com.travel.hotel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.hotel.model.Hotel;
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
public class HotelService {

    @Value("${booking.api.key}")
    private String apiKey;

    @Value("${booking.api.host}")
    private String apiHost;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> destIdCache = new ConcurrentHashMap<>();

    public List<Hotel> searchHotels(String location, String arrivalDate, String departureDate) {
        long totalStart = System.currentTimeMillis();
        try {
            long t1 = System.currentTimeMillis();
            String destId = resolveDestinationId(location);
            System.out.printf("[hotels] destination lookup: %dms — dest_id=%s%n",
                    System.currentTimeMillis() - t1, destId);

            HttpUrl url = Objects.requireNonNull(HttpUrl.parse("https://" + apiHost + "/api/v1/hotels/searchHotels"))
                    .newBuilder()
                    .addQueryParameter("dest_id", destId)
                    .addQueryParameter("search_type", "CITY")
                    .addQueryParameter("arrival_date", arrivalDate)
                    .addQueryParameter("departure_date", departureDate)
                    .addQueryParameter("adults", "1")
                    .addQueryParameter("room_qty", "1")
                    .addQueryParameter("currency_code", "USD")
                    .build();

            System.out.println("[hotels] search URL: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("x-rapidapi-key", apiKey)
                    .addHeader("x-rapidapi-host", apiHost)
                    .build();

            long t2 = System.currentTimeMillis();
            try (Response response = client.newCall(request).execute()) {
                System.out.printf("[hotels] search call: %dms — HTTP %d%n",
                        System.currentTimeMillis() - t2, response.code());

                String responseBody = Objects.requireNonNull(response.body()).string();
                System.out.println("[hotels] response (first 2000): " +
                        responseBody.substring(0, Math.min(responseBody.length(), 2000)));

                if (!response.isSuccessful()) {
                    System.err.println("[hotels] ERROR HTTP " + response.code() + ": " + responseBody);
                    return getMockHotels(location);
                }

                JsonNode root = objectMapper.readTree(responseBody);
                if (!root.has("data") || !root.path("data").has("hotels")) {
                    System.out.println("[hotels] WARNING: no hotels in response — using mock data");
                    return getMockHotels(location);
                }

                JsonNode hotels = root.path("data").path("hotels");
                List<Hotel> results = new ArrayList<>();
                hotels.forEach(h -> {
                    try {
                        JsonNode prop = h.path("property");
                        results.add(new Hotel(
                                prop.path("id").asText(),
                                prop.path("name").asText(),
                                location,
                                prop.path("priceBreakdown").path("grossPrice").path("value").asDouble(),
                                prop.path("reviewScore").asDouble()
                        ));
                    } catch (Exception e) {
                        System.err.println("[hotels] skipping hotel: " + e.getMessage());
                    }
                });

                System.out.printf("[hotels] total: %dms — %d results%n",
                        System.currentTimeMillis() - totalStart, results.size());
                return results.isEmpty() ? getMockHotels(location) : results;
            }

        } catch (Exception e) {
            System.err.printf("[hotels] EXCEPTION after %dms: %s%n",
                    System.currentTimeMillis() - totalStart, e.getMessage());
            e.printStackTrace();
            return getMockHotels(location);
        }
    }

    private String resolveDestinationId(String location) {
        String cached = destIdCache.get(location.toLowerCase());
        if (cached != null) {
            System.out.println("[hotels] dest cache hit: " + location + " → " + cached);
            return cached;
        }
        try {
            HttpUrl url = Objects.requireNonNull(HttpUrl.parse("https://" + apiHost + "/api/v1/hotels/searchDestination"))
                    .newBuilder()
                    .addQueryParameter("query", location)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("x-rapidapi-key", apiKey)
                    .addHeader("x-rapidapi-host", apiHost)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String body = Objects.requireNonNull(response.body()).string();
                System.out.println("[hotels] dest lookup [" + location + "] HTTP " + response.code() +
                        ": " + body.substring(0, Math.min(body.length(), 1000)));
                JsonNode root = objectMapper.readTree(body);
                JsonNode data = root.path("data");
                if (data.isArray() && data.size() > 0) {
                    for (JsonNode item : data) {
                        if ("CITY".equals(item.path("dest_type").asText())) {
                            String id = item.path("dest_id").asText();
                            destIdCache.put(location.toLowerCase(), id);
                            return id;
                        }
                    }
                    String id = data.get(0).path("dest_id").asText();
                    destIdCache.put(location.toLowerCase(), id);
                    return id;
                }
            }
        } catch (Exception e) {
            System.err.println("[hotels] dest lookup failed for '" + location + "': " + e.getMessage());
        }
        return location;
    }

    private List<Hotel> getMockHotels(String location) {
        return List.of(
                new Hotel("H001", "Grand Plaza Hotel", location, 180.0, 4.5),
                new Hotel("H002", "Sunset Resort", location, 250.0, 4.8),
                new Hotel("H003", "Budget Inn", location, 90.0, 3.9)
        );
    }
}
