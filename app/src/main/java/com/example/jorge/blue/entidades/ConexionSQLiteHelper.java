package com.example.jorge.blue.entidades;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.jorge.blue.utils.Utilities;

/**
 * Created by JORGE on 4/6/18.
 */

public class ConexionSQLiteHelper extends SQLiteOpenHelper {



    public ConexionSQLiteHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(Utilities.CREAR_TABLA_MEDICION);

    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS medicion");
        onCreate(db);
    }
}
