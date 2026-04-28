const API_BASE = 'http://localhost:8080';

// Tab switching
document.querySelectorAll('.tab').forEach(tab => {
    tab.addEventListener('click', () => {
        const targetTab = tab.dataset.tab;

        document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
        document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));

        tab.classList.add('active');
        document.getElementById(targetTab).classList.add('active');
    });
});

// Flight search
document.getElementById('flight-form').addEventListener('submit', async (e) => {
    e.preventDefault();

    const origin = document.getElementById('origin').value.trim().toUpperCase();
    const destination = document.getElementById('destination').value.trim().toUpperCase();
    const date = document.getElementById('date').value;
    const resultsDiv = document.getElementById('flight-results');
    resultsDiv.innerHTML = '<p class="loading">Searching flights...</p>';

    try {
        const response = await fetch(`${API_BASE}/flights/search?origin=${encodeURIComponent(origin)}&destination=${encodeURIComponent(destination)}&date=${date}`);
        const flights = await response.json();
        displayFlights(flights);
    } catch (error) {
        console.error('Error fetching flights:', error);
        resultsDiv.innerHTML = '<p class="error">Failed to fetch flights. Make sure API Gateway is running on port 8080.</p>';
    }
});

// Hotel search
document.getElementById('hotel-form').addEventListener('submit', async (e) => {
    e.preventDefault();

    const location = document.getElementById('hotel-location').value;
    const arrivalDate = document.getElementById('hotel-arrival').value;
    const departureDate = document.getElementById('hotel-departure').value;
    const resultsDiv = document.getElementById('hotel-results');
    resultsDiv.innerHTML = '<p class="loading">🏨 Searching hotels...</p>';

    try {
        const response = await fetch(`${API_BASE}/hotels/search?location=${location}&arrivalDate=${arrivalDate}&departureDate=${departureDate}`);
        const hotels = await response.json();
        displayHotels(hotels);
    } catch (error) {
        console.error('Error:', error);
        resultsDiv.innerHTML = '<p class="error">❌ Failed to fetch hotels.</p>';
    }
});

// Car search
document.getElementById('car-form').addEventListener('submit', async (e) => {
    e.preventDefault();

    const location = document.getElementById('car-location').value;
    const pickupDate = document.getElementById('car-pickup-date').value;
    const pickupTime = document.getElementById('car-pickup-time').value;   // HH:MM
    const dropoffDate = document.getElementById('car-dropoff-date').value;
    const dropoffTime = document.getElementById('car-dropoff-time').value; // HH:MM
    const resultsDiv = document.getElementById('car-results');
    resultsDiv.innerHTML = '<p class="loading">Searching cars...</p>';

    // Car rental API requires coordinates. Map common city names to lat/lon.
    const coordsMap = {
        'los angeles': { lat: 34.0522,  lon: -118.2437 },
        'new york':    { lat: 40.7128,  lon: -74.0060  },
        'london':      { lat: 51.5074,  lon: -0.1278   },
        'paris':       { lat: 48.8566,  lon: 2.3522    },
        'madrid':      { lat: 40.4168,  lon: -3.7038   },
        'baku':        { lat: 40.4093,  lon: 49.8671   },
        'dubai':       { lat: 25.2048,  lon: 55.2708   },
        'istanbul':    { lat: 41.0082,  lon: 28.9784   },
        'berlin':      { lat: 52.5200,  lon: 13.4050   },
        'rome':        { lat: 41.9028,  lon: 12.4964   },
        'amsterdam':   { lat: 52.3676,  lon: 4.9041    },
        'barcelona':   { lat: 41.3851,  lon: 2.1734    },
    };
    const key = location.toLowerCase().trim();
    const coords = coordsMap[key] || { lat: 51.4706, lon: -0.4619 }; // default: London Heathrow
    const { lat, lon } = coords;

    // Backend expects combined ISO datetime; it splits date and time internally.
    const pickUpDatetime = `${pickupDate}T${pickupTime}:00`;
    const dropOffDatetime = `${dropoffDate}T${dropoffTime}:00`;

    try {
        const response = await fetch(`${API_BASE}/cars/search?location=${encodeURIComponent(location)}&pickUpLat=${lat}&pickUpLon=${lon}&dropOffLat=${lat}&dropOffLon=${lon}&pickUpTime=${encodeURIComponent(pickUpDatetime)}&dropOffTime=${encodeURIComponent(dropOffDatetime)}`);
        const cars = await response.json();
        displayCars(cars);
    } catch (error) {
        console.error('Error:', error);
        resultsDiv.innerHTML = '<p class="error">❌ Failed to fetch cars.</p>';
    }
});

// Display functions
function displayFlights(flights) {
    const container = document.getElementById('flight-results');

    if (flights.length === 0) {
        container.innerHTML = '<p>No flights found.</p>';
        return;
    }

    container.innerHTML = `
        <table>
            <thead>
                <tr>
                    <th>Flight ID</th>
                    <th>Route</th>
                    <th>Departure</th>
                    <th>Arrival</th>
                    <th>Airline</th>
                    <th>Price</th>
                </tr>
            </thead>
            <tbody>
                ${flights.map(f => `
                    <tr>
                        <td>${f.id}</td>
                        <td>${f.origin} → ${f.destination}</td>
                        <td>${f.departureTime}</td>
                        <td>${f.arrivalTime}</td>
                        <td>${f.airline}</td>
                        <td>${f.currency}</td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
}

function displayHotels(hotels) {
    const container = document.getElementById('hotel-results');

    if (hotels.length === 0) {
        container.innerHTML = '<p>No hotels found.</p>';
        return;
    }

    container.innerHTML = `
        <table>
            <thead>
                <tr>
                    <th>Hotel ID</th>
                    <th>Name</th>
                    <th>Location</th>
                    <th>Price/Night</th>
                    <th>Rating</th>
                </tr>
            </thead>
            <tbody>
                ${hotels.map(h => `
                    <tr>
                        <td>${h.id}</td>
                        <td>${h.name}</td>
                        <td>${h.location}</td>
                        <td>$${h.pricePerNight}</td>
                        <td>⭐ ${h.rating}</td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
}

function displayCars(cars) {
    const container = document.getElementById('car-results');

    if (cars.length === 0) {
        container.innerHTML = '<p class="error">Car rental search is currently unavailable (upstream API error). The Booking.com car rental endpoint is returning a server error for all requests. Try again later.</p>';
        return;
    }

    container.innerHTML = `
        <table>
            <thead>
                <tr>
                    <th>Car ID</th>
                    <th>Model</th>
                    <th>Type</th>
                    <th>Location</th>
                    <th>Price/Day</th>
                </tr>
            </thead>
            <tbody>
                ${cars.map(c => `
                    <tr>
                        <td>${c.id}</td>
                        <td>${c.model}</td>
                        <td>${c.type}</td>
                        <td>${c.location}</td>
                        <td>$${c.pricePerDay}</td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
}