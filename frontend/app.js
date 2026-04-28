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

    const origin = document.getElementById('origin').value;
    const destination = document.getElementById('destination').value;
    const date = document.getElementById('date').value;

    try {
        const response = await fetch(`${API_BASE}/flights/search?origin=${origin}&destination=${destination}&date=${date}`);
        const flights = await response.json();

        displayFlights(flights);
    } catch (error) {
        console.error('Error fetching flights:', error);
        document.getElementById('flight-results').innerHTML = '<p class="error">Failed to fetch flights. Make sure API Gateway is running on port 8080.</p>';
    }
});

// Hotel search
document.getElementById('hotel-form').addEventListener('submit', async (e) => {
    e.preventDefault();

    const location = document.getElementById('hotel-location').value;

    try {
        const response = await fetch(`${API_BASE}/hotels/search?location=${location}`);
        const hotels = await response.json();

        displayHotels(hotels);
    } catch (error) {
        console.error('Error fetching hotels:', error);
        document.getElementById('hotel-results').innerHTML = '<p class="error">Failed to fetch hotels.</p>';
    }
});

// Car search
document.getElementById('car-form').addEventListener('submit', async (e) => {
    e.preventDefault();

    const location = document.getElementById('car-location').value;

    try {
        const response = await fetch(`${API_BASE}/cars/search?location=${location}`);
        const cars = await response.json();

        displayCars(cars);
    } catch (error) {
        console.error('Error fetching cars:', error);
        document.getElementById('car-results').innerHTML = '<p class="error">Failed to fetch cars.</p>';
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
        container.innerHTML = '<p>No cars found.</p>';
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