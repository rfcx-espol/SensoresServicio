package com.example.jorge.blue.entidades;

/**
 * Created by JORGE on 4/6/18.
 */

public class medicion {
    // Todos los Campos
    private int timestamp;
    private String type;
    private float value;
    private String unit;
    private String location;


    public medicion(int timestamp, String type, float value, String unit, String location) {
        this.timestamp = timestamp;
        this.type = type;
        this.value = value;
        this.unit = unit;
        this.location = location;
    }

    public medicion() {
        this.timestamp = 0;
        this.type = null;
        this.value = 0;
        this.unit = null;
        this.location = null;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
