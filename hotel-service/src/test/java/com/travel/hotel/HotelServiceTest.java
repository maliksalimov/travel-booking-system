package com.travel.hotel;

import com.travel.hotel.model.Hotel;
import com.travel.hotel.service.HotelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class HotelServiceTest {

    @Autowired
    private HotelService hotelService;

    @Test
    void testSearchHotelReturnList(){
        List<Hotel> hotels = hotelService.searchHotels("Delhi");

        assertNotNull(hotels);
        assertFalse(hotels.isEmpty());
        assertEquals(3, hotels.size());
    }

    @Test
    void testHotelLocationMatches(){
        String location = "Paris";

        List<Hotel> hotels = hotelService.searchHotels(location);

        for(Hotel hotel : hotels) {
            assertEquals(location, hotel.getLocation());
        }
    }

    @Test
    void testHotelPricePositive(){
        List<Hotel> hotels = hotelService.searchHotels("Delhi");
        for(Hotel hotel : hotels) {
            assertTrue(hotel.getPricePerNight() > 0);
        }
    }
}
