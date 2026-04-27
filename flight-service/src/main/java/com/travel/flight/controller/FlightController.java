package com.travel.flight.controller;

import com.travel.flight.model.Flight;
import com.travel.flight.service.FlightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/flights")
public class FlightController {

    private final FlightService flightService;

    @Autowired
    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<Flight>> searchFlights(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam(defaultValue = "2026-05-01") String date
    ) {

        List<Flight> flights = flightService.searchFlights(origin, destination, date);
        return ResponseEntity.ok(flights);

    }
}
