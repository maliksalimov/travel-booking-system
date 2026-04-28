package com.travel.carrental;

import com.travel.carrental.model.Car;
import com.travel.carrental.service.CarRentalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CarRentalServiceTest {

    @Autowired
    private CarRentalService carRentalService;

    @Test
    void testSearchCarsReturnsList() {
        List<Car> cars = carRentalService.searchCars("Los Angeles");

        assertNotNull(cars);
        assertFalse(cars.isEmpty());
        assertEquals(3, cars.size());
    }

    @Test
    void testCarLocationMatches() {
        String location = "Los Angeles";
        List<Car> cars = carRentalService.searchCars(location);

        for(Car car : cars) {
            assertEquals(location, car.getLocation());
        }
    }

    @Test
    void testCarPricePositive() {
        List<Car> cars = carRentalService.searchCars("Los Angeles");
        for(Car car : cars){
            assertTrue(car.getPricePerDay() > 0);
        }
    }
}
