package com.example.android.pets.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

/**
 * Created by jhancock2010 on 2/26/18.
 */

public class PetProvider extends ContentProvider {

    private String LOG_TAG = getClass().getSimpleName();

    private PetDbHelper mDbHelper;
    private static UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private static final int PETS=100;
    private static final int PET_ID=101;

    static{
        sUriMatcher.addURI(PetContract.PetEntry.CONTENT_AUTHORITY,
                PetContract.PetEntry.PATH_PETS, PETS);

        sUriMatcher.addURI(PetContract.PetEntry.CONTENT_AUTHORITY,
                PetContract.PetEntry.PATH_PETS+"/#", PET_ID);
    }

    @Override
    public boolean onCreate() {
        Log.d(LOG_TAG, "onCreate start");
        this.mDbHelper = new PetDbHelper(getContext());
        return true;
    }

    private String strArrtoStr(String[] arr){
        if (arr != null && arr.length >0) {
            StringBuilder sb = new StringBuilder();
            for (String s : arr) {
                sb.append(s+",");
            }
            sb.setLength(sb.length()-1);
            return sb.toString();
        } else if (arr == null){
            return null;
        } else {
            return "";
        }
    }
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        Log.d(LOG_TAG, "query start");
        Log.d(LOG_TAG, "uri = " + uri.toString());
        Log.d(LOG_TAG, "projection elements:");
        Log.d(LOG_TAG, strArrtoStr(projection));
        Log.d(LOG_TAG, "selecction = " + selection);
        Log.d(LOG_TAG, "selectionArgs elements: ");
        Log.d(LOG_TAG, strArrtoStr(selectionArgs)+"");
        Log.d(LOG_TAG, "sortOrder = " + sortOrder);


        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        Cursor cursor=null;

        int match = sUriMatcher.match(uri);
        Log.d(LOG_TAG, "value matched from URI is: " + match);

        switch(match){
            case PETS:
                Log.d(LOG_TAG, "matched PETS ID from uri");
                cursor = db.query(PetContract.PetEntry.TABLE_NAME, projection, selection,
                        selectionArgs, null, null, sortOrder);
                break;
            case PET_ID:
                Log.d(LOG_TAG, "matched PET_ID from uri");
                selection = PetContract.PetEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri))};
                cursor = db.query(PetContract.PetEntry.TABLE_NAME, projection, selection,
                        selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("no case for matched id " + match
                        + " from URI " + uri);
        }
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {

        Log.d(LOG_TAG, "getType start");

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return PetContract.PetEntry.CONTENT_LIST_TYPE;
            case PET_ID:
                return PetContract.PetEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    private void validate(ContentValues values) throws IllegalArgumentException {
        Log.d(LOG_TAG, "validate start");
        String name = values.getAsString(PetContract.PetEntry.COLUMN_PET_NAME);

        if (TextUtils.isEmpty(name)){
            Log.d(LOG_TAG, "invalaid value for name " + name);
            throw new IllegalArgumentException("name is null");
        }

        Integer weight = values.getAsInteger(PetContract.PetEntry.COLUMN_PET_WEIGHT);
        if (weight == null || weight <= 0){
            throw new IllegalArgumentException("invalid value for weight  " + weight);
        }

        Integer gender = values.getAsInteger(PetContract.PetEntry.COLUMN_PET_GENDER);
        if (gender == null || ! isValidGender(gender)){
            throw new IllegalArgumentException("invalid value for weight  " + weight);
        }
    }

    private boolean isValidGender(Integer gender) {
        return gender == PetContract.PetEntry.GENDER_FEMALE ||
                gender == PetContract.PetEntry.GENDER_MALE ||
                gender == PetContract.PetEntry.GENDER_UNKNOWN ;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {

        Log.d(LOG_TAG, "insert start");
        Log.d(LOG_TAG, "uri = " + uri);
        Log.d(LOG_TAG, "contentValues = " + contentValues);

        validate(contentValues);

        final int match = sUriMatcher.match(uri);
        Uri result = null;
        switch (match) {
            case PETS:
                result = insertPet(uri, contentValues);
                break;
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
        return result;
    }

    /**
     * Insert a pet into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertPet(Uri uri, ContentValues values) {


        Log.d(LOG_TAG, "insertPet start");
        Log.d(LOG_TAG, "uri = " + uri);
        Log.d(LOG_TAG, "contentValues = " + values);

        // TODO: Insert a new pet into the pets database table with the given ContentValues

        // Once we know the ID of the new row in the table,
        // return the new URI with the ID appended to the end of it
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        long id = db.insert(PetContract.PetEntry.TABLE_NAME, null, values);
        if (id == -1){
            Log.d(LOG_TAG, "insert returned -1");
        }
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {


        Log.d(LOG_TAG, "delete start");

        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                // Delete all rows that match the selection and selection args
                return database.delete(PetContract.PetEntry.TABLE_NAME, selection, selectionArgs);
            case PET_ID:
                // Delete a single row given by the ID in the URI
                selection = PetContract.PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return database.delete(PetContract.PetEntry.TABLE_NAME, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {

        Log.d(LOG_TAG, "where " + selection);

        if (selectionArgs != null){
            for (String s: selectionArgs){
                Log.d(LOG_TAG, "selectionArgs s " + s);
            }
        }

        final int match = sUriMatcher.match(uri);

        int result = 0;

        switch (match) {
            case PETS:
                result = updatePet( values, selection, selectionArgs);
                break;
            case PET_ID:
                // For the PET_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = PetContract.PetEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                result = updatePet( values, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
        return result;
    }

    private int updatePet(ContentValues values, String selection, String[] selectionArgs){
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        validateForUpdate(values, selection, selectionArgs);

        if (values.size() > 0) {
            return db.update(PetContract.PetEntry.TABLE_NAME, values, selection, selectionArgs);
        } else {
            return 0;
        }
    }

    private void validateForUpdate(ContentValues values, String selection, String[] selectionArgs)
    throws IllegalArgumentException{
        if(values.containsKey(PetContract.PetEntry.COLUMN_PET_WEIGHT)){
            int weight = values.getAsInteger(PetContract.PetEntry.COLUMN_PET_WEIGHT);
            if (weight < 0){
                throw new IllegalArgumentException("Weight must be greater then 0");
            }
        }

        if (values.containsKey(PetContract.PetEntry.COLUMN_PET_NAME)){
            String name = values.getAsString(PetContract.PetEntry.COLUMN_PET_NAME);
            if (name == null){
                throw new IllegalArgumentException("Pet must have a name");
            }
        }

        if (values.size() == 0){
            throw new IllegalArgumentException("nothing");
        }
    }

}
