package com.travel.flight;

import com.travel.flight.model.Flight;
import com.travel.flight.service.FlightService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class FlightServiceTest {

    @Autowired
    private FlightService flightService;

    @Test
    void testSearchFlightsReturnsList() {
        List<Flight> flights = flightService.searchFlights("Delhi", "Mumbai", "2026-05-15");

        assertNotNull(flights);
        assertFalse(flights.isEmpty());
        assertTrue(flights.size() >= 3);
    }

    @Test
    void testFlightOriginAndDestination() {
        List<Flight> flights = flightService.searchFlights("Delhi", "Mumbai", "2026-05-15");
        Flight flight = flights.get(0);

        assertEquals("Delhi", flight.getOrigin());
        assertEquals("Mumbai", flight.getDestination());
    }

    @Test
    void testFlightPriceIsPositive() {
        List<Flight> flights = flightService.searchFlights("Delhi", "Mumbai", "2026-05-15");

        for(Flight flight : flights) {
            assertNotNull(flight.getPrice());
            assertTrue(flight.getPrice() > 0);
        }
    }


}
