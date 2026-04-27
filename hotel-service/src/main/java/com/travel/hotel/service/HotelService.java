package com.travel.hotel.service;

import com.travel.hotel.model.Hotel;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class HotelService {

    public List<Hotel> searchHotels(String location) {
        return List.of(
                new Hotel("H001", "Grand Plaza Hotel", location, 180.0, 4.5),
                new Hotel("H002", "Sunset Resort", location, 250.0, 4.8),
                new Hotel("H003", "Budget Inn", location, 90.0, 3.9)
        );
    }
}