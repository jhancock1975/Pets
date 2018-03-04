/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.pets;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.Cursor;
import android.net.ParseException;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.pets.data.PetContract.PetEntry;

/**
 * Allows user to create a new pet or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor>{

    private static String LOG_TAG="Editor Activity";

    /** EditText field to enter the pet's name */
    private EditText mNameEditText;

    /** EditText field to enter the pet's breed */
    private EditText mBreedEditText;

    /** Seek bar field to enter the pet's weight */
    private SeekBar mWeightseekBar;

    /** Text view that shows weight the user is selecting using the seek bar*/
     private TextView mWeightTextView;

    /** EditText field to enter the pet's gender */
    private Spinner mGenderSpinner;

    private boolean mIsNewPet;

    /** Identifier for the pet data loader */
    private static final int EXISTING_PET_LOADER = 0;

    /** flag for checking if screen is touched **/
    private boolean mPetHasChanged = false;

    // OnTouchListener that listens for any user touches on a View, implying that they are modifying
    // the view, and we change the mPetHasChanged boolean to true.

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mPetHasChanged = true;
            return false;
        }
    };

    /**
     * Gender of the pet. The possible valid values are in the PetContract.java file:
     * {@link PetEntry#GENDER_UNKNOWN}, {@link PetEntry#GENDER_MALE}, or
     * {@link PetEntry#GENDER_FEMALE}.
     */
    private int mGender = PetEntry.GENDER_UNKNOWN;

    //holds seek bar position in string format
    private String mSeekBarPostionStr;

    private Uri mCurrentPetUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        //find URI from caller
        mCurrentPetUri = getIntent().getData();
        Log.d(LOG_TAG, "onCreate uri = " + mCurrentPetUri);
        if (mCurrentPetUri == null){
            setTitle(getString(R.string.editor_activity_title_new_pet));
            mIsNewPet = true;
            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a pet that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            setTitle(getString(R.string.editor_activity_title_edit_pet));
            mIsNewPet =false;
            getLoaderManager().initLoader(EXISTING_PET_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_pet_name);
        mBreedEditText = (EditText) findViewById(R.id.edit_pet_breed);

        mWeightseekBar = (SeekBar) findViewById(R.id.seek_pet_weight);
        mWeightseekBar .setOnSeekBarChangeListener(seekBarChangeListener);

        mWeightTextView = (TextView) findViewById(R.id.label_weight_units);
        mWeightTextView.setText(getString(R.string.seek_default_weight) + " "
                + getString(R.string.unit_pet_weight));

        mGenderSpinner = (Spinner) findViewById(R.id.spinner_gender);

        mSeekBarPostionStr = getString(R.string.seek_default_weight);;

        setupSpinner();

        mNameEditText.setOnTouchListener(mTouchListener);
        mBreedEditText.setOnTouchListener(mTouchListener);
        mWeightseekBar.setOnTouchListener(mTouchListener);
        mGenderSpinner.setOnTouchListener(mTouchListener);

    }

    SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {


        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            // updated continuously as the user slides the thumb
            mWeightTextView.setText(progress + " kg");
            mSeekBarPostionStr = progress+"";
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // called when the user first touches the SeekBar
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // called after the user finishes moving the SeekBar
        }
    };

    ArrayAdapter mGenderSpinnerAdapter;
    /**
     * Setup the dropdown spinner that allows the user to select the gender of the pet.
     */
    private void setupSpinner() {
        // Create adapter for spinner. The list options are from the String array it will use
        // the spinner will use the default layout
        mGenderSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_gender_options, android.R.layout.simple_spinner_item);

        // Specify dropdown layout style - simple list view with 1 item per line
        mGenderSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        // Apply the adapter to the spinner
        mGenderSpinner.setAdapter(mGenderSpinnerAdapter);

        // Set the integer mSelected to the constant values
        mGenderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    if (selection.equals(getString(R.string.gender_male))) {
                        mGender = PetEntry.GENDER_MALE;
                    } else if (selection.equals(getString(R.string.gender_female))) {
                        mGender = PetEntry.GENDER_FEMALE;
                    } else {
                        mGender = PetEntry.GENDER_UNKNOWN;
                    }
                }
            }

            // Because AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mGender = PetEntry.GENDER_UNKNOWN;
            }
        });
    }



    /**
     * Get user input from editor and save new pet into database.
     */
    private void savePet() {
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        boolean haveDataToInsert = false;

        String nameString = mNameEditText.getText().toString().trim();
        if (!TextUtils.isEmpty(nameString)){
            haveDataToInsert = true;
        }
        String breedString = mBreedEditText.getText().toString().trim();
        if (!TextUtils.isEmpty(breedString)){
            haveDataToInsert = true;
        }

        String weightString = mSeekBarPostionStr;
        Log.d(LOG_TAG, "weightString value = " + weightString);
        int weight = 0;
        try {
            weight = Integer.parseInt(weightString);
            haveDataToInsert = true;
        } catch(ParseException e){
            Log.d(LOG_TAG, "parse exception trying to parse weight value " +
                    "we log here surrounded with asterisks to accentuate white space: ****" +
                weightString + "***");
        }

        if ((mGender != PetEntry.GENDER_UNKNOWN) && (! haveDataToInsert)){
            haveDataToInsert = true;
        }

        // Create a ContentValues object where column names are the keys,
        // and pet attributes from the editor are the values.
        ContentValues values = new ContentValues();
        values.put(PetEntry.COLUMN_PET_NAME, nameString);
        values.put(PetEntry.COLUMN_PET_BREED, breedString);
        values.put(PetEntry.COLUMN_PET_GENDER, mGender);
        values.put(PetEntry.COLUMN_PET_WEIGHT, weight);

        if (haveDataToInsert) {
            if (mIsNewPet) {
                Log.d(LOG_TAG, "adding new pet");
                // Insert a new row for pet in the database, returning the ID of that new row.
                Uri result = getContentResolver().insert(PetEntry.CONTENT_URI, values);
                // Show a toast message depending on whether or not the insertion was successful
                if (result == null) {

                    // If the row ID is -1, then there was an error with insertion.
                    Toast.makeText(this, R.string.toast_str_save_err, Toast.LENGTH_SHORT).show();

                } else {

                    // Otherwise, the insertion was successful and we can display a toast with the row ID.
                    Toast.makeText(this, getString(R.string.toast_str_insert_id_result),
                            Toast.LENGTH_SHORT).show();
                }
            } else {

                long petId = ContentUris.parseId(mCurrentPetUri);

                Log.d(LOG_TAG, "updating pet with id: " + petId);
                Log.d(LOG_TAG, "values size: " + values.size());

                int updateCount = getContentResolver().update(
                        mCurrentPetUri, values, null, null);

                // Show a toast message depending on whether or not the insertion was successful
                if (updateCount < 1) {
                    Toast.makeText(this, getString(R.string.toast_str_no_update),
                            Toast.LENGTH_SHORT).show();
                } else {
                    // Otherwise, the insertion was successful and we can display a toast with the row ID.
                    Toast.makeText(this, updateCount +
                            getString(R.string.toast_str_update_id_result), Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(this, getString(R.string.toast_str_no_data_to_save),
                    Toast.LENGTH_SHORT).show();

        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new pet, hide the "Delete" menu item.
        if (mCurrentPetUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save pet to database
                savePet();
                // Exit activity
                finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the pet hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mPetHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Log.d(LOG_TAG, "onCreateLoader::start");
        return new CursorLoader(this, mCurrentPetUri, new String[] {
                PetEntry._ID, PetEntry.COLUMN_PET_GENDER, PetEntry.COLUMN_PET_BREED,
                PetEntry.COLUMN_PET_WEIGHT, PetEntry.COLUMN_PET_NAME
        }, null, null, null);

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.d(LOG_TAG, "onLoadFinished::start");
        if (cursor.moveToFirst()) {

            mNameEditText.setText(cursor.getString(
                    cursor.getColumnIndexOrThrow(PetEntry.COLUMN_PET_NAME)));

            mBreedEditText.setText(cursor.getString(
                    cursor.getColumnIndexOrThrow(PetEntry.COLUMN_PET_BREED)));

            mGenderSpinner.setSelection(cursor.getInt(
                    cursor.getColumnIndexOrThrow(PetEntry.COLUMN_PET_GENDER)));

            mWeightseekBar.setProgress(cursor.getInt(
                    cursor.getColumnIndexOrThrow(PetEntry.COLUMN_PET_WEIGHT)));
        }
        Log.d(LOG_TAG, "onLoadFinished::end");

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(LOG_TAG, "onLoaderRest::start");
        mNameEditText.getText().clear();

        mBreedEditText.getText().clear();

        mGenderSpinner.setSelection(0);

        mWeightseekBar.setProgress(50);

        Log.d(LOG_TAG, "onLoaderRest::end");
    }


    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        // If the pet hasn't changed, continue with handling back button press
        if (!mPetHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the pet.
                deletePet();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the pet in the database.
     */
    private void deletePet() {
        getContentResolver().delete(mCurrentPetUri, null, null);

        Toast.makeText(this, getString(R.string.toast_str_pet_deleted) +
                ContentUris.parseId(mCurrentPetUri),
                Toast.LENGTH_SHORT).show();

        finish();
    }
}