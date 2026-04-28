package com.travel.carrental.controller;

import com.travel.carrental.model.Car;
import com.travel.carrental.service.CarRentalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cars")
public class CarRentalController {
    private final CarRentalService carRentalService;


    @Autowired
    public CarRentalController(CarRentalService carRentalService) {
        this.carRentalService = carRentalService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<Car>> searchCars(
            @RequestParam String pickUpLat,
            @RequestParam String pickUpLon,
            @RequestParam String dropOffLat,
            @RequestParam String dropOffLon,
            @RequestParam String pickUpTime,
            @RequestParam String dropOffTime) {
        return ResponseEntity.ok(carRentalService.searchCars(pickUpLat, pickUpLon, dropOffLat, dropOffLon, pickUpTime, dropOffTime));
    }
}
