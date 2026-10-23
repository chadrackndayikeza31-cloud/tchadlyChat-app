package com.chadrack.tchadlychat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "tchadlychat.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_USERS = "users";

    private static final String COLUMN_ID = "id";
    private static final String COLUMN_PSEUDO = "pseudo";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_PASSWORD = "password";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String createUsersTable =
                "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_PSEUDO + " TEXT UNIQUE NOT NULL, " +
                COLUMN_EMAIL + " TEXT UNIQUE NOT NULL, " +
                COLUMN_PASSWORD + " TEXT NOT NULL" +
                ")";

        db.execSQL(createUsersTable);
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    public boolean ajouterUtilisateur(
            String pseudo,
            String email,
            String password) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(COLUMN_PSEUDO, pseudo);
        values.put(COLUMN_EMAIL, email);

        // Ntitubika password muri SQLite.
        values.put(COLUMN_PASSWORD, "");

        long resultat = db.insert(TABLE_USERS, null, values);

        db.close();

        return resultat != -1;
    }

    public boolean utilisateurExiste(String pseudo) {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{COLUMN_ID},
                COLUMN_PSEUDO + " = ?",
                new String[]{pseudo},
                null,
                null,
                null
        );

        boolean existe = cursor.moveToFirst();

        cursor.close();
        db.close();

        return existe;
    }

    public boolean emailExiste(String email) {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{COLUMN_ID},
                COLUMN_EMAIL + " = ?",
                new String[]{email},
                null,
                null,
                null
        );

        boolean existe = cursor.moveToFirst();

        cursor.close();
        db.close();

        return existe;
    }

    public boolean verifierConnexion(
            String pseudo,
            String password) {

        // Iyi method ntigikoreshwa kuri Firebase login.
        return false;
    }

    public String getEmailParPseudo(String pseudo) {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{COLUMN_EMAIL},
                COLUMN_PSEUDO + " = ?",
                new String[]{pseudo},
                null,
                null,
                null
        );

        String email = null;

        if (cursor.moveToFirst()) {
            email = cursor.getString(
                    cursor.getColumnIndexOrThrow(COLUMN_EMAIL)
            );
        }

        cursor.close();
        db.close();

        return email;
    }
}