package com.example.jorge.blue.utils;

/**
 * Created by JORGE on 4/6/18.
 */

public class Utilities {
    //Constantes campos tabla medicion

    public static final String TABLA_MEDICION = "medicion";
    public static final String CAMPO_TIMESTAMP = "timestamp";
    public static final String CAMPO_TYPE = "type";
    public static final String CAMPO_VALUE = "value";
    public static final String CAMPO_UNIT = "unit";
    public static final String CAMPO_LOCATION = "location";





    public final static String CREAR_TABLA_MEDICION = "CREATE TABLE " + TABLA_MEDICION +
            "("+ CAMPO_TIMESTAMP +" TEXT, " + CAMPO_TYPE + " TEXT, " + CAMPO_VALUE +
            " REAL, " + CAMPO_UNIT + " TEXT, " + CAMPO_LOCATION + " TEXT)";



}
