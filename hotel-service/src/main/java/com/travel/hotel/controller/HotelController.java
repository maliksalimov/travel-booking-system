package com.travel.hotel.controller;

import com.travel.hotel.model.Hotel;
import com.travel.hotel.service.HotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/hotels")
public class HotelController {
    private final HotelService hotelService;

    @Autowired
    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<Hotel>> searchHotels(
            @RequestParam String location,
            @RequestParam(defaultValue = "2026-05-01") String arrivalDate,
            @RequestParam(defaultValue = "2026-05-05") String departureDate
    ) {
        return ResponseEntity.ok(hotelService.searchHotels(location, arrivalDate, departureDate));
    }
}