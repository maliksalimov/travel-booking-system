package com.travel.carrental.model;

public class Car {
    private String id;
    private String model;
    private String location;
    private Double pricePerDay;
    private String type;

    public Car() {
    }

    public Car(String id, String model, String location, Double pricePerDay, String type) {
        this.id = id;
        this.model = model;
        this.location = location;
        this.pricePerDay = pricePerDay;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Double getPricePerDay() {
        return pricePerDay;
    }

    public void setPricePerDay(Double pricePerDay) {
        this.pricePerDay = pricePerDay;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
