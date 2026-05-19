package com.dnrcpw.cpwmobilepdf.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.dnrcpw.cpwmobilepdf.R;

import java.io.File;
import java.text.DecimalFormat;

/**
 * Shown on long press of map name in MainActivity.
 * Allows the user to rename or delete the map.
 */
public class EditMapNameActivity extends AppCompatActivity {
    Integer id;
    EditText editTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit_map_name);

        String mapName;
        String str;
        editTxt = findViewById(R.id.rename);
        ImageButton clearBtn = findViewById(R.id.clear_rename);
        final TextView editFileName = findViewById(R.id.editFileName);
        final TextView editFileSize = findViewById(R.id.editFileSize);
        TextView editBounds = findViewById(R.id.editBounds);

        // Clear rename map textview
        clearBtn.setOnClickListener(view -> {
            editTxt.setText("");
            editFileName.setText("");
            editFileSize.setText("");
        });


        // As user types in editTxt update filename
        class DoneOnEditorActionListener implements TextView.OnEditorActionListener {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event){
                if (actionId == EditorInfo.IME_ACTION_DONE){
                    String name = editTxt.getText().toString();
                    if (!name.endsWith(".pdf"))
                       name = name + ".pdf";
                    String txt = getFilesDir()+"/"+name;
                    editFileName.setText(txt);
                    return true;
                }
                return false;
            }
        }
        editTxt.setOnEditorActionListener(new DoneOnEditorActionListener());

        Intent i = this.getIntent();
        try {
            mapName = i.getExtras().getString("NAME");
        } catch (NullPointerException e){
            mapName = "map name";
        }

        try {
            id = i.getExtras().getInt("ID");
        }catch (NullPointerException e){
            Toast.makeText(EditMapNameActivity.this, "ID missing!", Toast.LENGTH_LONG).show();
            return;
        }

        ImageView img = findViewById(R.id.editImage);
        String imgPath =  i.getExtras().getString("IMG");
        if (imgPath != null) {
            File imgFile = new File(imgPath);
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            if (myBitmap != null)
                img.setImageBitmap(myBitmap);
            else
                img.setImageResource(R.drawable.pdf_icon);
        }
        else
            img.setImageResource(R.drawable.pdf_icon);
        editTxt.setText(mapName);
        String path = i.getExtras().getString("PATH");
        if (path == null) {
            Toast.makeText(EditMapNameActivity.this, "File path missing!", Toast.LENGTH_LONG).show();
            return;
        }
        String bounds = i.getExtras().getString("BOUNDS");
        String[] latlong;
        DecimalFormat df = new DecimalFormat("0.00000");
        try{
            latlong = bounds.split(" ");
        }catch (NullPointerException e){
            latlong = new String[2];
            latlong[0]="0";
            latlong[1]="0";
        }
        if (latlong.length == 1){
            latlong = new String[2];
            latlong[0]="0";
            latlong[1]="0";
        }
            // get unique lat long values
            String lat1 = latlong[0];
            String long1 = latlong[1];
            String lat2="0";
            String long2="0";
            for (int j=2; j<latlong.length; j++){
                // even number, latitudes
                if ((j % 2) == 0){
                    if (!lat1.equals(latlong[j])) lat2 = latlong[j];
                }
                // odd number, longitudes
                else {
                    if (!long1.equals(latlong[j])) long2 = latlong[j];
                }
            }
            bounds = df.format(Double.parseDouble(lat1)) + ", " + df.format(Double.parseDouble(long1)) +", "+df.format(Double.parseDouble(lat2)) +", "+df.format(Double.parseDouble(long2));
            str = "This app's folder: "+path;
            editFileName.setText(str);
            editBounds.setText(bounds);

        // get file size
        File file = new File(path);
        df = new DecimalFormat("0.00");
        double size = Double.parseDouble(Long.toString(file.length()));
        String unit = " bytes";
        if (size >= 1024) {
            size = size / 1024;
            unit = "KB";
        }
        if (size >= 1024) {
            size = size / 1024;
            unit = "MB";
        }
        if (size >= 1024) {
            size = size / 1024;
            unit = "GB";
        }
        // truncate KB
        if (unit.equals("KB")) df = new DecimalFormat("0");
        str = df.format(size) + unit;
        editFileSize.setText(str);
    }


    // Remove Imported Map dialog
    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    //'DELETE' button clicked, remove map from imported maps
                    Intent mainIntent = new Intent(EditMapNameActivity.this, MainActivity.class);
                    mainIntent.putExtra("ID",id);
                    mainIntent.putExtra("DELETE","true");
                    mainIntent.putExtra("UPDATES", "true");
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(mainIntent);
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //'CANCEL' button clicked, do nothing
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        if (item.getItemId() == R.id.delete_map) {
            // display alert dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(EditMapNameActivity.this);
            builder.setTitle("Delete");
            builder.setMessage("Delete the imported map?").setPositiveButton("DELETE", dialogClickListener)
                    .setNegativeButton("CANCEL", dialogClickListener).show();
            return true;
        } else if (item.getItemId() == android.R.id.home){
            // back button pressed, return
            Intent mainIntent = new Intent(EditMapNameActivity.this, MainActivity.class);
            mainIntent.putExtra("ID",id);
            mainIntent.putExtra("UPDATES", "true");
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(mainIntent);
            return true;
        } else if (item.getItemId() == R.id.save) {
            // rename map
            String name;
            name = editTxt.getText().toString();
            if (name.equals("")) {
                Toast.makeText(EditMapNameActivity.this, "Cannot rename to blank!", Toast.LENGTH_LONG).show();
                return true;
            }
            Intent mainIntent = new Intent(EditMapNameActivity.this, MainActivity.class);
            mainIntent.putExtra("NAME", name);
            mainIntent.putExtra("ID", id);
            mainIntent.putExtra("RENAME", "true");
            mainIntent.putExtra("UPDATES", "true");
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(mainIntent);
            return true;
        } else {
            //default:
                return super.onOptionsItemSelected(item);
        }
    }
}
