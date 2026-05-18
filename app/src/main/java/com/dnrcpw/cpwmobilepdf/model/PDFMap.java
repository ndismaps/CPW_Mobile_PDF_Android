package com.dnrcpw.cpwmobilepdf.model;

import android.content.Context;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;

import com.dnrcpw.cpwmobilepdf.R;
import com.dnrcpw.cpwmobilepdf.data.DBHandler;
import com.itextpdf.text.pdf.PdfArray;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfNumber;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfString;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by tammy on 12/11/2017.
 *  Imported Maps
 */

public class PDFMap {
    private String path, bounds, mediabox, viewport, name, fileSize, distToMap;
    private String orientation;
    private int id;
    private Double miles;
    private String thumbnail; // image filename

    public PDFMap(){
    }

    public PDFMap(String path, String bounds, String mediabox, String viewport, String thumbnail, String name, String fileSize, String distToMap, String orient){
        this.path = path;
        this.bounds = bounds;
        this.mediabox = mediabox;
        this.viewport = viewport;
        this.thumbnail = thumbnail;
        this.name = name;
        this.fileSize = fileSize;
        this.distToMap = distToMap;
        this.orientation = orient;
        if (distToMap.equals("onmap") || distToMap.equals(""))
            this.miles = 0.0;
        else
            this.miles = Double.parseDouble(distToMap);
    }

    public PDFMap importMap(Context c) {
        // preform background computation to read lat long, page size, boundaries from pdf
        File file;
        PDFMap pdfMap = this;
        String filePath = pdfMap.getPath();
        try {
            // Get PDF
            file = new File(filePath);

            // Use iText 5 to read margins, page size, and lat long. iText 7 works with Android 26 and above. iText 5 works with older versions.
            // PDF ISO standard.

            // iText does this for us:
            // Go to the end of the pdf file, back up 1mb and search for startxref. Read the offset to the first xref table.
            // startxref
            // offset number to first xref table
            //%%EOF
            // Read xref table containing offsets to each object in the file. This is the format:
            // xref
            // 0 104
            // 0000000000 65535 f
            // 0000000010 00000 n
            // 0000973188 00000 n
            // ...
            // trailer
            // <<
            // /Size 104
            // /Info 103 0 R
            // /Root 102 0 R         Pointer to object 102. Points to /Pages --> /Kids --> /Contents /BBox /MediaBox
            // /Prev 2874274         May contain /Prev if there is another xref table.
            // >>
            // Read the trailer. /Root points to an object number, "5 0 R"  is the object ID, 0 is the generation number, R = Reference to object.
            // /Root 5 0 R --> .../Pages 1 0 R --> /Kids[15 0 R  16 0 R] --> /Contents.../MediaBox[...].../BBox[...].../Measure 79 0 R --> /Bounds[ ].../GPTS[lat long]/GCS 80 0 R --> GPS projection, ,UNIT["Degree",0.01745]
            // trailer
            //<</Size 82/Root 5 0 R/Info 3 0 R/ID[<481274B989C1D7419BA9E71CBA227123><D6AEE54D32AC354E980F653350D6C962>]/Prev 2874274>>

            PdfReader reader = new PdfReader(filePath);

            //int numPages = reader.getNumberOfPages();

            PdfDictionary page = reader.getPageN(1);
            if (page == null) {
                reader.close();
                pdfMap.setName("Import Failed");
                return pdfMap; //("Import Failed");
            }
            PdfArray vp = page.getAsArray(PdfName.VP);
            // If this is PDF ISO standard file and not a GeoPDF check the version number
            if (vp != null){
                if (Character.getNumericValue(reader.getPdfVersion()) < 6) {
                    // Version less than PDF 1.6
                    reader.close();
                    pdfMap.setName("Import Failed - Not Georeferenced");
                    return pdfMap; //("Not Georeferenced");
                }
            }

            //--------------------------
            // Get MediaBox page size
            //--------------------------
            mediabox = page.getAsArray(PdfName.MEDIABOX).toString(); // works [ 0 0 792 1224]
            if (mediabox.equals("")) {
                reader.close();
                pdfMap.setName("Import Failed");
                return pdfMap; //("Import Failed");
            }
            mediabox = mediabox.substring(1,mediabox.length()-1).trim();
            mediabox = mediabox.replaceAll(",","");

            // ---------------------------------------------------------------------------
            // Get viewport, margins for ISO Standard if vp does not exist it is a GeoPDF
            //----------------------------------------------------------------------------
            if (vp != null) {
                // Find largest image it should be the map
                int id = 0;
                double max = 0.0;
                for (int i = 0; i < vp.size(); i++) {
                    PdfArray b = vp.getAsDict(i).getAsArray(PdfName.BBOX);
                    String parse = b.toString().trim();
                    parse = parse.substring(1, parse.length() - 1);
                    parse = parse.replaceAll(",", "");
                    int pos = parse.indexOf(" ");
                    // double bBoxX1 = Double.parseDouble(parse.substring(0, pos));
                    // FIND bBoxY1
                    parse = parse.substring(pos + 1); // strip off 'bBoxX1 '
                    pos = parse.indexOf(" ");
                    double bBoxY1 = Double.parseDouble(parse.substring(0, pos));
                    // FIND bBoxX2
                    parse = parse.substring(pos + 1); // strip off 'bBoxY1 '
                    pos = parse.indexOf(" ");
                    //double bBoxX2 = Double.parseDouble(parse.substring(0, pos));
                    // FIND bBoxY2
                    parse = parse.substring(pos + 1); // strip off 'bBoxX2 '
                    pos = parse.indexOf(" ");
                    double bBoxY2 = Double.parseDouble(parse.substring(pos + 1));
                    double h;
                    if (bBoxY1 > bBoxY2) h = bBoxY1 - bBoxY2;
                    else h = bBoxY2 - bBoxY1;
                    if (max < h) {
                        max = h;
                        id = i;
                    }
                }
                PdfDictionary vpDict = vp.getAsDict(id);
                if (vpDict == null) {
                    reader.close();
                    pdfMap.setName("Import Failed");
                    return pdfMap; //("Import Failed");
                }
                PdfArray bbox = vpDict.getAsArray(PdfName.BBOX);
                if (bbox == null) {
                    reader.close();
                    pdfMap.setName("Import Failed");
                    return pdfMap; //("Import Failed");
                }
                viewport = bbox.toString().trim();
                viewport = viewport.substring(1, viewport.length() - 1);
                viewport = viewport.replaceAll(",", "");

                String[] bboxArr = viewport.split(" ");
                //---------------------------------------------
                // Get Lat Long from /Measure dictionary /GPTS
                //---------------------------------------------
                PdfDictionary measure = vpDict.getAsDict(PdfName.MEASURE);
                // Measure subtype=GEO
                // get unit box correction for BBox if [0,1, 0,0, 1,0, 1,1] needs no correction
                /*String unitBox = measure.get(PdfName.BOUNDS).toString();
                unitBox = unitBox.trim();
                unitBox = unitBox.substring(1,unitBox.length() -1);
                unitBox = unitBox.replaceAll(",", ""); // remove commas
                String [] units = unitBox.split(" ");


                // adjust bbox by unitBox
                // top-left
                Double bboxX1 = Double.valueOf(bboxArr[0]) + (Double.valueOf(bboxArr[2]) - Double.valueOf(bboxArr[0])) * (Double.valueOf(units[6]) - 1);
                Double bboxY1 = Double.valueOf(bboxArr[1]) + (Double.valueOf(bboxArr[3]) - Double.valueOf(bboxArr[1])) * Double.valueOf(units[5]);
                // bottom-left map boundary * unit square
                Double bboxX2 = Double.valueOf(bboxArr[2]) + (Double.valueOf(bboxArr[2]) - Double.valueOf(bboxArr[0])) * Double.valueOf(units[2]);
                Double bboxY2 = Double.valueOf(bboxArr[3]) + (Double.valueOf(bboxArr[3]) - Double.valueOf(bboxArr[1])) * (Double.valueOf(units[1]) - 1);*/

                // using this one
                /*
                double bboxX1 = Double.parseDouble(bboxArr[0]) + ((Double.parseDouble(units[0]) - 0) * Double.parseDouble(bboxArr[0]));
                double bboxY1 = Double.parseDouble(bboxArr[1]) + ((Double.parseDouble(units[1]) - 1) * Double.parseDouble(bboxArr[1]));
                // bottom-right map boundary * unit square
                double bboxX2 = Double.parseDouble(bboxArr[2]) + ((Double.parseDouble(units[4]) - 1) * Double.parseDouble(bboxArr[2]));
                double bboxY2 = Double.parseDouble(bboxArr[3]) + ((Double.parseDouble(units[5]) - 0) * Double.parseDouble(bboxArr[3]));
                */

                // No adjustment with unit box

                double bboxX1 = Double.parseDouble(bboxArr[0]);
                double bboxY1 = Double.parseDouble(bboxArr[1]);
                // bottom-right map boundary * unit square
                double bboxX2 = Double.parseDouble(bboxArr[2]);
                double bboxY2 = Double.parseDouble(bboxArr[3]);



                //Log.d("viewport", viewport);

                // Adjust view port by unit box???????
                viewport = bboxX1+" "+bboxY1+" "+bboxX2+" "+bboxY2;
                //Log.d("adjusted viewport", viewport);

                bounds = measure.get(PdfName.GPTS).toString();
                if (bounds.equals("")) {
                    reader.close();
                    pdfMap.setName("Import Failed");
                    return pdfMap; //("Import Failed");
                }
                bounds = bounds.trim();
                bounds = bounds.substring(1, bounds.length() - 1);
                bounds = bounds.replaceAll(",", "");
                //String[] latlong;
                //latlong = bounds.split(" ");
                // Test - not working! adjusted with the unit square - gives the correct lat long for BBox
                // bottom-left lat/long (given long + (height or width in decimal degrees) * unit square value
               /* double lat1 = Double.parseDouble(latlong[0]) + (Double.parseDouble(latlong[2]) - Double.parseDouble(latlong[0])) * Double.parseDouble(units[0]);
                double long1 = Double.parseDouble(latlong[1]) + (Double.parseDouble(latlong[7]) - Double.parseDouble(latlong[1])) * (Double.parseDouble(units[1]) - 1);
                // top-left lat/long (given lat + (height or width in decimal degrees) * unit square value
                double lat2 = Double.parseDouble(latlong[2]) + (Double.parseDouble(latlong[2]) - Double.parseDouble(latlong[0])) * Double.parseDouble(units[2]);
                double long2 = Double.parseDouble(latlong[3]) + (Double.parseDouble(latlong[5]) - Double.parseDouble(latlong[3])) * Double.parseDouble(units[3]);

                // top-right lat/long (given lat + (height or width in decimal degrees) * unit square value
                double lat3 = Double.parseDouble(latlong[4]) + (Double.parseDouble(latlong[4]) - Double.parseDouble(latlong[6])) * Double.parseDouble(units[4]);
                double long3 = Double.parseDouble(latlong[5]) + (Double.parseDouble(latlong[5]) - Double.parseDouble(latlong[3])) * Double.parseDouble(units[5]);
                // bottom-right lat/long (given lat + (height or width in decimal degrees) * unit square value
                double lat4 = Double.parseDouble(latlong[6]) + (Double.parseDouble(latlong[4]) - Double.parseDouble(latlong[4])) * (Double.parseDouble(units[6]) - 1);
                double long4 = Double.parseDouble(latlong[7]) + (Double.parseDouble(latlong[1]) - Double.parseDouble(latlong[5])) * Double.parseDouble(units[7]);
                Log.d("adjusted lat/long", lat1+", "+long1+ "  "+lat2+", "+long2+"  "+lat3+", "+long3+"  "+lat4+", "+long4);

                */
            }
            // View port not found geoPDF
            else{
                // /MediaBox [0,0,1638,2088]
                // /LGIDict array of dictionaries
                //      /CTM [a (scale), b, c, d (scale), H (horizontal map distance in meters), V (vertical map distance in meters)]
                //      /Neatline  array of page margins in points 1/72 of an inch [h2, v1, h1, v1, h1, v2, h2, v2, h2, v1]
                //      /Display dictionary of /Projection (optional but will have /Projection instead)
                //          /Projection dictionary
                //              /ProjectionType "UT" UTM works need zone
                //              /Zone "12" or "13"
                //              /Units "m"
                //      /Projection dictionary of /Projection (optional but will have /Display instead)
                //          /Projection dictionary
                //              /ProjectionType "UT" UTM works need zone
                //              /Zone "12" or "13"
                //              /Units "m"
                PdfName lgiDict = new PdfName("LGIDict");
                PdfName ctm = new PdfName("CTM");
                //PdfName desc = new PdfName("Description");
                PdfName display = new PdfName("Display");
                PdfName projection = new PdfName("Projection");
                PdfName projectionType = new PdfName("ProjectionType");
                PdfName PdfNameZone = new PdfName("Zone");
                PdfName PdfNameUnits = new PdfName("Units");
                //PdfName lgitinfo = new PdfName("LGIT:Info");
                PdfName neatline = new PdfName("Neatline");
                PdfArray lgiDictArray = page.getAsArray(lgiDict);
                if (lgiDictArray == null) {
                    reader.close();
                    pdfMap.setName("Import Failed - not georeferenced? No LGIDict dictionary");
                    return pdfMap; //"Import Failed - not georeferenced? No LGIDict dictionary";
                }
                int max=0;
                int id=0;
                PdfDictionary lgiDictionary;
                // Select LGIDict dictionary with largest vertical area
                for (int i=0; i<lgiDictArray.size(); i++) {
                    lgiDictionary = lgiDictArray.getAsDict(i);
                    PdfArray neatArray = lgiDictionary.getAsArray(neatline);
                    int v1 = Math.round(Float.parseFloat(neatArray.getAsString(1).toString()));
                    int v2 = Math.round(Float.parseFloat(neatArray.getAsString(3).toString()));
                    if (v1 == v2) v2 = Math.round(Float.parseFloat(neatArray.getAsString(5).toString()));
                    if (v1 < v2){
                        int tmp;
                        tmp = v1;
                        v1 = v2;
                        v2 = tmp;
                    }
                    int thisMax = v1 - v2;
                    if (thisMax > max){
                        max = thisMax;
                        id = i;
                    }
                }
                lgiDictionary = lgiDictArray.getAsDict(id);

                PdfDictionary displayDict = lgiDictionary.getAsDict(display);
                PdfString projType;
                PdfDictionary projDict;
                int zone=13;
                PdfString units;
                // Working for projection type UTM, units meters
                if (displayDict == null){
                    projDict = lgiDictionary.getAsDict(projection);
                    projType = projDict.getAsString(projectionType);
                    if (!projType.toString().toLowerCase(Locale.US).equals("ut")){
                        reader.close();
                        pdfMap.setName("Import Failed - unknown projection: "+projType.toString());
                        return pdfMap; //"Import Failed - unknown projection: "+projType.toString();
                    }
                    units = projDict.getAsString(PdfNameUnits);
                    if (!units.toString().toLowerCase(Locale.US).equals("m")) {
                        reader.close();
                        pdfMap.setName("Import Failed - unknown unit: "+units.toString());
                        return pdfMap; //"Import Failed - unknown unit: "+units.toString();
                    }
                    PdfNumber z = projDict.getAsNumber(PdfNameZone);
                    if (z != null)
                        zone = Integer.parseInt(z.toString());
                }
                else{
                    projType = displayDict.getAsString(projectionType);
                    if (!projType.toString().toLowerCase(Locale.US).equals("ut")) {
                        reader.close();
                        pdfMap.name = "Import Failed - unknown projection: "+projType.toString();
                        return pdfMap; //"Import Failed - unknown projection: "+projType.toString();
                    }
                    units = displayDict.getAsString(PdfNameUnits);
                    if (!units.toString().toLowerCase(Locale.US).equals("m")) {
                        reader.close();
                        pdfMap.name = "Import Failed - unknown unit: "+units.toString();
                        return pdfMap; //"Import Failed - unknown unit: "+units.toString();
                    }
                    PdfNumber z = displayDict.getAsNumber(PdfNameZone);
                    if (z != null)
                        zone = Integer.parseInt(z.toString());
                }

                // Get View Port
                // This works for projType == UT (for UTM) units == m
                PdfArray neatArray = lgiDictionary.getAsArray(neatline);
                int h1 = Math.round(Float.parseFloat(neatArray.getAsString(0).toString()));
                int v1 = Math.round(Float.parseFloat(neatArray.getAsString(1).toString()));
                int h2 = Math.round(Float.parseFloat(neatArray.getAsString(2).toString()));
                int v2 = Math.round(Float.parseFloat(neatArray.getAsString(3).toString()));
                if (h1 == h2) h2 = Math.round(Float.parseFloat(neatArray.getAsString(4).toString()));
                if (v1 == v2) v2 = Math.round(Float.parseFloat(neatArray.getAsString(5).toString()));
                if (h2 < h1){
                    int tmp;
                    tmp = h1;
                    h1 = h2;
                    h2 = tmp;
                }
                if (v1 < v2){
                    int tmp;
                    tmp = v1;
                    v1 = v2;
                    v2 = tmp;
                }
                viewport = h1 + " " + v1 + " " + h2 + " " + v2;

                // Get Latitude/Longitude Bounds = lat1 long1 lat2 long1 lat2 long2 lat1 long2
                PdfArray ctmArray = lgiDictionary.getAsArray(ctm);
                double a = Double.parseDouble(ctmArray.getAsString(0).toString()); // scale (x2 - x1) / (h2 - h1)
                double H = Double.parseDouble(ctmArray.getAsString(4).toString()); // H = x1 - a * h1
                double V = Double.parseDouble(ctmArray.getAsString(5).toString()); // V = y1 - a * v1
                double x1 = H + a * h1; // meters
                double y1 = V + a * v1; // meters
                double x2 = H + a * h2; // meters
                double y2 = V + a * v2; // meters
                double [] latlong1 = UTMtoLL(y1,x1,zone);
                double [] latlong2 = UTMtoLL(y2,x2,zone);
                bounds = String.format("%s %s %s %s %s %s", latlong2[1], latlong1[0], latlong1[1], latlong1[0], latlong1[1], latlong2[0]);
            }
            reader.close();
        }catch(Exception ex){
            pdfMap.name = "Import Failed";
            return pdfMap;
        }

        // ---------------------
        //  get thumbnail image
        // --------------------
        byte[] thumbnail;
        int pageNum = 0;
        PdfiumCore pdfiumCore = new PdfiumCore(c);
        try {
            ParcelFileDescriptor fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfDocument pdfDocument = pdfiumCore.newDocument(fd);
            pdfiumCore.openPage(pdfDocument, pageNum);

            int width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNum);
            int height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNum);

            // ARGB_8888 - best quality, high memory usage, higher possibility of OutOfMemoryError
            // RGB_565 - little worse quality, twice less memory usage
            Bitmap bitmap = Bitmap.createBitmap(width, height,
                    Bitmap.Config.RGB_565);
            pdfiumCore.renderPageBitmap(pdfDocument, bitmap, pageNum, 0, 0,
                    width, height);

            // scale it down
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap,100, (int) Math.round(100.0 * (double)height/(double)width),false);
            //if you need to render annotations and form fields, you can use
            //the same method above adding 'true' as last param
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.PNG, 0, stream);
            thumbnail = stream.toByteArray();
            bitmap.recycle(); // free memory
            scaled.recycle();
            pdfiumCore.closeDocument(pdfDocument); // 12-17-21 added close document

            // Get Map Name
            String name = file.getName();

            // Get File Size
            String fileSize;
            double size = file.length() / 1024.0; // Get size and convert bytes into Kb.
            if (size >= 1024) {
                fileSize = String.format(Locale.US,"%.1f %s", (size / 1024.0), c.getResources().getString(R.string.Mb));
            } else {
                fileSize = String.format(Locale.US,"%.0f %s", size, c.getResources().getString(R.string.Kb));
            }

            // IMPORT INTO the DATABASE
            pdfMap.setName(name);
            pdfMap.setFileSize(fileSize);
            //pdfMap.setThumbnail(thumbnail);
            pdfMap.setViewport(viewport);
            pdfMap.setMediabox(mediabox);
            pdfMap.setBounds(bounds);
            pdfMap.setMapOrientation("none");

            // move to GetMoreActivity
                //DBHandler db = new DBHandler(c);
                //Integer index = db.addMap(pdfMap);
                //pdfMap.setId(index);

            // Save thumbnail to a file in app directory (/data/data/com/dnrcpw/cpwmobilepdf/files), save the path to it.
            File img;
            try {
                // Save thumbnail image in local storage where this App is.
                String path = c.getFilesDir().getAbsolutePath();
                img = new File(path + "/CPWthumbnail" + pdfMap.getId() + ".png");
                if (!img.exists()) {
                    boolean fileDoesNotExist = img.createNewFile();
                    if (!fileDoesNotExist)
                        Toast.makeText(c, c.getResources().getString(R.string.problemThumbnailSavingFile), Toast.LENGTH_LONG).show();
                }
                FileOutputStream fos = new FileOutputStream(img);
                fos.write(thumbnail);
                fos.close();
                pdfMap.setThumbnail(path + "/CPWthumbnail" + pdfMap.getId() + ".png");
            } catch (IOException e){
                Toast.makeText(c, c.getResources().getString(R.string.problemThumbnailDiskFull) + e.getMessage(), Toast.LENGTH_LONG).show();
                pdfMap.setThumbnail(null);
            } catch (SecurityException e){
                Toast.makeText(c, c.getResources().getString(R.string.problemThumbnailSecurity) + e.getMessage(), Toast.LENGTH_LONG).show();
                pdfMap.setThumbnail(null);
            } catch (Exception e) {
                Toast.makeText(c, c.getResources().getString(R.string.problemThumbnailSaving) + e.getMessage(), Toast.LENGTH_LONG).show();
                pdfMap.setThumbnail(null);
            }
            // update the thumbnail
            //db.updateMap(pdfMap);
            //db.close();
            fd.close(); // thumbnail file descriptor
        } catch (IOException | SQLException ex) {
            ex.printStackTrace();
            pdfMap.setName("deleting...");
            pdfMap.setName("Import Failed " + ex.getMessage());
            return pdfMap; //"Import Failed " + ex.getMessage();
        }
        return pdfMap; //c.getResources().getString(R.string.importdone); //"Import Done";
    }

    private double[] UTMtoLL(double f, double f1, int j) {
        // Convert UTM to Lat Long return [lat, long]
        // UTM=f,f1 Zone=j Colorado is mostly 13 and a little 12
        double d = 0.99960000000000004;
        double d1 = 6378137;
        double d2 = 0.0066943799999999998;

        double d4 = (1 - Math.sqrt(1-d2))/(1 + Math.sqrt(1 - d2));
        double d15 = f1 - 500000;
        //double d16 = f;
        double d11 = ((j - 1) * 6 - 180) + 3;

        double d3 = d2/(1 - d2);
        double d10 = f / d;
        double d12 = d10 / (d1 * (1 - d2/4 - (3 * d2 *d2)/64 - (5 * Math.pow(d2,3))/256));
        double d14 = d12 + ((3*d4)/2 - (27*Math.pow(d4,3))/32) * Math.sin(2*d12) + ((21*d4*d4)/16 - (55 * Math.pow(d4,4))/32) * Math.sin(4*d12) + ((151 * Math.pow(d4,3))/96) * Math.sin(6*d12);
        //double d13 = d14 * 180 / Math.PI;
        double d13a = 1 - d2 * Math.sin(d14) * Math.sin(d14); // tlb store since it is used twice
        double d5 = d1 / Math.sqrt(d13a);
        double d6 = Math.tan(d14)*Math.tan(d14);
        double d7 = d3 * Math.cos(d14) * Math.cos(d14);
        double d8 = (d1 * (1 - d2))/Math.pow(d13a,1.5);

        double d9 = d15/(d5 * d);
        double d17 = d14 - ((d5 * Math.tan(d14))/d8)*(((d9*d9)/2-(((5 + 3*d6 + 10*d7) - 4*d7*d7-9*d3)*Math.pow(d9,4))/24) + (((61 +90*d6 + 298*d7 + 45*d6*d6) - 252*d3 -3 * d7 *d7) * Math.pow(d9,6))/720);
        d17 = d17 * 180 / Math.PI;
        double d18 = ((d9 - ((1 + 2 * d6 + d7) * Math.pow(d9,3))/6) + (((((5 - 2 * d7) + 28*d6) - 3 * d7 * d7) + 8 * d3 + 24 * d6 * d6) * Math.pow(d9,5))/120)/Math.cos(d14);
        d18 = d11 + d18 * 180 / Math.PI;
        return new double[]{d18,d17};
    }

    //public Boolean getSelected(){return selected;}

    //public void setSelected(Boolean value){selected=value;}

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getBounds() {
        return bounds;
    }

    public void setBounds(String bounds) {
        this.bounds = bounds;
    }

    public String getMediabox() {
        return mediabox;
    }

    public void setMediabox(String mediabox) {
        this.mediabox = mediabox;
    }

    public String getViewport() {
        return viewport;
    }

    public void setViewport(String viewport) {
        this.viewport = viewport;
    }

    public String getName() { return name; }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileSize() { return fileSize; }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public String getDistToMap() { return distToMap; }

    public void setDistToMap(String distToMap) {
        this.distToMap = distToMap;
    }

    public Double getMiles() { return miles; }

    public void setMiles(Double miles) { this.miles = miles; }

    public void setMapOrientation(String orient) { this.orientation = orient; }
    public String getMapOrientation() { return orientation; }

    // Sort arraylist of PDFMaps by pdf file name a-z
    public static Comparator<PDFMap> NameComparator = (m1, m2) -> {
        String name1 = m1.getName().toLowerCase(Locale.US);
        String name2 = m2.getName().toLowerCase(Locale.US);
        // Return ascending order
        return name1.compareTo(name2);
    };

    // Sort arraylist of PDFMaps by pdf file name reverse z-a
    public static Comparator<PDFMap> NameComparatorReverse = (m1, m2) -> {
        String name1 = m1.getName().toLowerCase(Locale.US);
        String name2 = m2.getName().toLowerCase(Locale.US);
        // Return ascending order
        return name2.compareTo(name1);
    };

    // Sort arraylist of PDFMaps by pdf file modification date
    public static Comparator<PDFMap> DateComparator = (m1, m2) -> {
        File file1 = new File(m1.getPath());
        File file2 = new File(m2.getPath());
        long firstDate = file1.lastModified();
        long secondDate = file2.lastModified();
        // Return ascending order
        return Long.compare(secondDate, firstDate);
    };

    // Sort arraylist of PDFMaps by pdf file modification date reversed
    public static Comparator<PDFMap> DateComparatorReverse = (m1, m2) -> {
        File file1 = new File(m1.getPath());
        File file2 = new File(m2.getPath());
        long firstDate = file1.lastModified();
        long secondDate = file2.lastModified();
        // Return ascending order
        return Long.compare(firstDate, secondDate);
    };

    // Sort arraylist of PDFMaps by proximity to the user's current location, closest first
    public static Comparator<PDFMap> ProximityComparator = (m1, m2) -> {
        // Sort closest map at top
        Double miles1 = m1.getMiles();
        Double miles2 = m2.getMiles();
        return miles1.compareTo(miles2);
    };

    // Sort arraylist of PDFMaps by proximity to the user's current location, closest last
    public static Comparator<PDFMap> ProximityComparatorReverse = (m1, m2) -> {
        // Sort closest map at top
        Double miles1 = m1.getMiles();
        Double miles2 = m2.getMiles();
        return miles2.compareTo(miles1);
    };

    // Sort arraylist of PDFMaps by pdf file size largest first
    public static Comparator<PDFMap> SizeComparator = (m1, m2) -> {
        String m1Num = m1.getFileSize();
        if (m1Num.length()<4)return 0; // if map got stuck importing, the file size may be blank
        m1Num = m1Num.substring(0,m1Num.length()-3);
        Float size1 = Float.parseFloat(m1Num);

        if (m1.getFileSize().contains("Mb")){
            size1 = size1 * 1000;
        }
        else if (m1.getFileSize().contains("Gb")){
            size1 = size1 * 1000000;
        }
        String m2Num = m2.getFileSize();
        if (m2Num.length()<4)return 0; // if map got stuck importing, the file size may be blank
        m2Num = m2Num.substring(0,m2Num.length()-3);
        Float size2 = Float.parseFloat(m2Num);
        if (m2.getFileSize().contains("Mb")){
            size2 = size2 * 1000;
        }
        else if (m2.getFileSize().contains("Gb")){
            size2 = size2 * 1000000;
        }
        // Return largest to smallest
        return size2.compareTo(size1);
    };

    // Sort arraylist of PDFMaps by pdf file size, smallest first
    public static Comparator<PDFMap> SizeComparatorReverse = (m1, m2) -> {
        String m1Num = m1.getFileSize();
        if (m1Num.length()<4)return 0; // if map got stuck importing, the file size may be blank
        m1Num = m1Num.substring(0,m1Num.length()-3);
        Float size1 = Float.parseFloat(m1Num);

        if (m1.getFileSize().contains("Mb")){
            size1 = size1 * 1000;
        }
        else if (m1.getFileSize().contains("Gb")){
            size1 = size1 * 1000000;
        }
        String m2Num = m2.getFileSize();
        if (m2Num.length()<4)return 0; // if map got stuck importing, the file size may be blank
        m2Num = m2Num.substring(0,m2Num.length()-3);
        Float size2 = Float.parseFloat(m2Num);
        if (m2.getFileSize().contains("Mb")){
            size2 = size2 * 1000;
        }
        else if (m2.getFileSize().contains("Gb")){
            size2 = size2 * 1000000;
        }
        // Return largest to smallest
        return size1.compareTo(size2);
    };
}