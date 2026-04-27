package com.travel.carrental.service;

import com.travel.carrental.model.Car;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarRentalService {
    public List<Car> searchCars(String location) {
        return List.of(
                new Car("C001", "Toyota Camry", location, 55.0, "Sedan"),
                new Car("C002", "Ford Explorer", location, 85.0, "SUV"),
                new Car("C003", "Tesla Model 3", location, 120.0, "Electric")
        );
    }
}
