package com.dnrcpw.cpwmobilepdf.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.dnrcpw.cpwmobilepdf.R;
import com.dnrcpw.cpwmobilepdf.data.DBHandler;
import com.dnrcpw.cpwmobilepdf.data.ToastUtils;
import com.dnrcpw.cpwmobilepdf.model.Track;
import com.dnrcpw.cpwmobilepdf.model.Tracks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditTrackActivity extends AppCompatActivity{
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor(); // for database calls
    EditText editTxt;
    TextView timeStamp;
    ImageView trackImg;
    RadioGroup trackColorGrp;
    RadioButton cyanBtn, redBtn, blueBtn;
    private Tracks tracks;
    String mapName;
    String path;
    String bounds;
    String viewPort;
    int id;
    Track track;
    //boolean landscape;
    boolean changed=false;
    String prevName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Read the waypoint id that was clicked on and the map name
        Intent i = this.getIntent();
        if (i.getExtras() == null){
            Toast.makeText(EditTrackActivity.this, "Failed to get map values. Try again.",Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        id = i.getExtras().getInt("CLICKED");
        mapName = i.getExtras().getString("NAME");
        path = i.getExtras().getString("PATH");
        bounds = i.getExtras().getString("BOUNDS");
        //String mediaBox = i.getExtras().getString("MEDIABOX");
        viewPort = i.getExtras().getString("VIEWPORT");
        //landscape = i.getExtras().getBoolean("LANDSCAPE");

        // if the map was locked in landscape, show this also in landscape
        /*if (landscape){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        }*/
        dbExecutor.execute(() -> {
            DBHandler db = DBHandler.getInstance(EditTrackActivity.this);
            try {
                tracks = db.getTracks(mapName);
            } catch (SQLException exc) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    ToastUtils.showExtendedToast(EditTrackActivity.this, "Failed to read tracks from database. " + exc.getMessage());
                });

            } catch (Exception exc) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    ToastUtils.showExtendedToast(EditTrackActivity.this, "Failed to read tracks from database, other exception. " + exc.getMessage());
                });

            }
            // Switch to main thread to push the data to your UI
            runOnUiThread(() -> {
                try {
                    track = tracks.get(id);
                } catch (Exception e) {
                    ToastUtils.showExtendedToast(EditTrackActivity.this, "That track was not found in the database. Error: " + e.getMessage());
                    return;
                }

                setContentView(R.layout.activity_track);
                prevName = track.getDesc();
                editTxt = findViewById(R.id.trackName);
                editTxt.setText(track.getDesc());
                trackImg = findViewById(R.id.track_img);
                timeStamp = findViewById(R.id.trackTime);
                timeStamp.setText(track.getTime());
                trackColorGrp = findViewById(R.id.trackColor);
                String trackColor = track.getColorName();
                cyanBtn = findViewById(R.id.cyanTrack);
                redBtn = findViewById(R.id.redTrack);
                blueBtn = findViewById(R.id.blueTrack);
                if (trackColor.equals("cyan")) {
                    cyanBtn.setChecked(true);
                    trackImg.setImageResource(R.drawable.ic_cyan_track);
                } else if (trackColor.equals("red")) {
                    redBtn.setChecked(true);
                    trackImg.setImageResource(R.drawable.ic_red_track);
                } else {
                    blueBtn.setChecked(true);
                    trackImg.setImageResource(R.drawable.ic_blue_track);
                }
                ImageButton clearBtn = findViewById(R.id.clear_track);

                // Clear track name
                clearBtn.setOnClickListener(view -> editTxt.setText(""));

                // Listeners for track color radio buttons
                trackColorGrp.setOnCheckedChangeListener((radioGroup, checkedId) -> {
                    if (checkedId == R.id.cyanTrack) {
                        track.setColorName("cyan");
                        trackImg.setImageResource(R.drawable.ic_cyan_track);
                        changed = true;
                    } else if (checkedId == R.id.redTrack) {
                        track.setColorName("red");
                        trackImg.setImageResource(R.drawable.ic_red_track);
                        changed = true;
                    } else if (checkedId == R.id.blueTrack) {
                        track.setColorName("blue");
                        trackImg.setImageResource(R.drawable.ic_blue_track);
                        changed = true;
                    }
                });
            });
        });
    }

    // Remove track dialog
    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    //'DELETE' button clicked, remove map from imported maps
                    dbExecutor.execute(() -> {
                        DBHandler.getInstance(EditTrackActivity.this).deleteTrack(track);
                    });

                    // Return to PDFActivity
                    finish();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //'CANCEL' button clicked, do nothing
                    break;
            }
        }
    };

    // not saved
    DialogInterface.OnClickListener dialogClickListener2 = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    //'SAVE' button clicked, save then exit
                    String name = editTxt.getText().toString();
                    if (name.isEmpty()) {
                        Toast.makeText(EditTrackActivity.this, "Cannot rename to blank!", Toast.LENGTH_LONG).show();
                    } else {
                        track.setDesc(name);
                        dbExecutor.execute(() -> {
                            DBHandler.getInstance(EditTrackActivity.this).updateTrack(track);
                        });
                        // Return to PDFActivity
                        finish();
                    }
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //'BACK' button clicked, do nothing
                    finish();
                    break;
            }
        }
    };

    // EDIT MENU
    // -------------
    //   ... Menu
    // -------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.edit_menu, menu);
        //menu.findItem(R.id.action_quality).setChecked(bestQuality);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        if (item.getItemId() == R.id.delete_map) {
            // display alert dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(EditTrackActivity.this);
            builder.setTitle("Delete");
            builder.setMessage("Delete this waypoint?").setPositiveButton("DELETE", dialogClickListener)
                    .setNegativeButton("CANCEL", dialogClickListener).show();
            return true;
        } else if (item.getItemId() == R.id.save) {
            // rename map
            String name = editTxt.getText().toString();
            if (name.isEmpty()) {
                Toast.makeText(EditTrackActivity.this, "Cannot rename to blank!", Toast.LENGTH_LONG).show();
            } else {
                track.setDesc(name);
                dbExecutor.execute(() -> {
                    DBHandler.getInstance(EditTrackActivity.this).updateTrack(track);
                });
                // Return to PDFActivity
                finish();
            }
            return true;
        } else if (item.getItemId() == android.R.id.home){
            // show dialog if not saved
            if (changed || !prevName.equals(editTxt.getText().toString())) {
                AlertDialog.Builder builder = new AlertDialog.Builder(EditTrackActivity.this);
                builder.setTitle("Warning");
                builder.setMessage("Changes are not saved.").setPositiveButton("SAVE", dialogClickListener2)
                        .setNegativeButton("DON'T SAVE", dialogClickListener2).show();
            }
            else {
                finish();
            }
            return true;
        } else {
            //default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume(){
        super.onResume();

    }
    @Override
    protected void onStop(){
        super.onStop();
    }
}
