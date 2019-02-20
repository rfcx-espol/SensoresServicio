package com.example.jorge.blue.entidades;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.jorge.blue.utils.Utilities;

/**
 * Created by JORGE on 4/6/18.
 * Refactorized by Leonardo Larrea 1/2/18.
 */

public class ConexionSQLiteHelper extends SQLiteOpenHelper {


    public final static String CREATE_MEASURE_TABLE =
            "CREATE TABLE " + Utilities.MEASURE_TABLE +
                    "("+
                    Utilities.FIELD_ID+ " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"+
                    Utilities.FIELD_TIMESTAMP +" TEXT, " + Utilities.FIELD_TYPE + " TEXT, " +
                    Utilities.FIELD_VALUE + " REAL, " +
                    Utilities.FIELD_UNIT + " TEXT, " +
                    Utilities.FIELD_SENSORID + " TEXT, " +
                    Utilities.FIELD_LOCATION + " TEXT"+
                    ")";



    public final static String CREATE_IMAGE_TABLE =
            "CREATE TABLE " + Utilities.IMAGES_TABLE +
                    "("+
                    Utilities.IMAGE_ID+ " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"+
                    Utilities.IMAGE_NAME +" TEXT, "+
                    Utilities.IMAGE_TYPE + " REAL, " +
                    Utilities.IMAGE_TIMESTAMP + " TEXT "+
                    ")";

    public ConexionSQLiteHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_MEASURE_TABLE);
        db.execSQL(CREATE_IMAGE_TABLE);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+Utilities.MEASURE_TABLE);
        db.execSQL("DROP TABLE IF EXISTS "+Utilities.IMAGES_TABLE);
        onCreate(db);
    }
}
