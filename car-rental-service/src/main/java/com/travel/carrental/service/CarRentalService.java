package com.travel.carrental.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.carrental.model.Car;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class CarRentalService {

    @Value("${booking.api.key}")
    private String apiKey;

    @Value("${booking.api.host}")
    private String apiHost;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Car> searchCars(String pickUpLat, String pickUpLon, String dropOffLat, String dropOffLon,
                                String pickUpTime, String dropOffTime) {
        long totalStart = System.currentTimeMillis();
        try {
            // API requires pick_up_date and pick_up_time as separate params.
            // Input arrives as "2026-05-01T10:00:00" — split on T.
            String[] pickUpParts = splitDateTime(pickUpTime);
            String[] dropOffParts = splitDateTime(dropOffTime);

            System.out.printf("[cars] pick_up_date=%s pick_up_time=%s drop_off_date=%s drop_off_time=%s%n",
                    pickUpParts[0], pickUpParts[1], dropOffParts[0], dropOffParts[1]);

            HttpUrl url = Objects.requireNonNull(HttpUrl.parse("https://" + apiHost + "/api/v1/cars/searchCarRentals"))
                    .newBuilder()
                    .addQueryParameter("pick_up_latitude", pickUpLat)
                    .addQueryParameter("pick_up_longitude", pickUpLon)
                    .addQueryParameter("drop_off_latitude", dropOffLat)
                    .addQueryParameter("drop_off_longitude", dropOffLon)
                    .addQueryParameter("pick_up_date", pickUpParts[0])
                    .addQueryParameter("pick_up_time", pickUpParts[1])
                    .addQueryParameter("drop_off_date", dropOffParts[0])
                    .addQueryParameter("drop_off_time", dropOffParts[1])
                    .addQueryParameter("driver_age", "30")
                    .addQueryParameter("currency_code", "USD")
                    .build();

            System.out.println("[cars] search URL: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("x-rapidapi-key", apiKey)
                    .addHeader("x-rapidapi-host", apiHost)
                    .build();

            long t1 = System.currentTimeMillis();
            try (Response response = client.newCall(request).execute()) {
                System.out.printf("[cars] search call: %dms — HTTP %d%n",
                        System.currentTimeMillis() - t1, response.code());

                String responseBody = Objects.requireNonNull(response.body()).string();
                System.out.println("[cars] full response: " + responseBody);

                if (!response.isSuccessful()) {
                    System.err.println("[cars] ERROR HTTP " + response.code());
                    return List.of();
                }

                JsonNode root = objectMapper.readTree(responseBody);

                if (!root.path("status").asBoolean(true)) {
                    String msg = root.path("message").toString();
                    System.err.println("[cars] API returned status:false — message: " + msg);
                    // "Something went wrong" is a persistent server-side error from booking-com15
                    // for all coordinate inputs. Return empty list so the frontend can show a
                    // proper "unavailable" message rather than fake mock data.
                    return List.of();
                }

                JsonNode data = root.path("data");

                // Handle both array and object with search_results
                JsonNode results;
                if (data.isArray()) {
                    results = data;
                } else if (data.has("search_results")) {
                    results = data.path("search_results");
                } else {
                    System.out.println("[cars] WARNING: unexpected data structure — keys: " + data.fieldNames());
                    return List.of();
                }

                List<Car> cars = new ArrayList<>();
                results.forEach(r -> {
                    try {
                        JsonNode vehicle = r.path("vehicle_info");
                        cars.add(new Car(
                                r.path("result_key").asText(),
                                vehicle.path("v_name").asText(),
                                pickUpLat + "," + pickUpLon,
                                r.path("pricing_info").path("drive_away_price").asDouble(),
                                vehicle.path("sipp_name").asText()
                        ));
                    } catch (Exception e) {
                        System.err.println("[cars] skipping result: " + e.getMessage());
                    }
                });

                System.out.printf("[cars] total: %dms — %d results%n",
                        System.currentTimeMillis() - totalStart, cars.size());
                return cars;
            }

        } catch (Exception e) {
            System.err.printf("[cars] EXCEPTION after %dms: %s%n",
                    System.currentTimeMillis() - totalStart, e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    /** Splits "2026-05-01T10:00:00" into ["2026-05-01", "10:00"]. Falls back gracefully. */
    private String[] splitDateTime(String datetime) {
        if (datetime == null || datetime.isBlank()) {
            return new String[]{"2026-05-01", "10:00"};
        }
        int tIdx = datetime.indexOf('T');
        if (tIdx < 0) tIdx = datetime.indexOf(' ');
        if (tIdx < 0) {
            return new String[]{datetime, "10:00"};
        }
        String datePart = datetime.substring(0, tIdx);
        String timePart = datetime.substring(tIdx + 1);
        // Trim to HH:MM (API doesn't want seconds)
        if (timePart.length() > 5) timePart = timePart.substring(0, 5);
        return new String[]{datePart, timePart};
    }

    private List<Car> getMockCars(String location) {
        return List.of(
                new Car("C001", "Toyota Camry", location, 55.0, "Sedan"),
                new Car("C002", "Ford Explorer", location, 85.0, "SUV"),
                new Car("C003", "Tesla Model 3", location, 120.0, "Electric")
        );
    }
}
