package org.tensorflow.demo;


import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Trace;
import android.provider.MediaStore;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;
import android.widget.TextView;
import android.widget.Toast;


public class UploadActivity extends AppCompatActivity {

    private static final int SELECT_PICTURE = 0;
    private ImageView imageView;
    private Button takePictureButton;
    private Button selectPictureButton;
    private Button recognizePictureButton;
    private Button downloadPictureButton;

    private static int INPUT_SIZE = 800;
    private static final String TF_OD_API_MODEL_FILE =
            "file:///android_asset/faster_rcnn_buttons_graph.pb";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/button_detect_label.txt";
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;


    private Classifier detector = null;
    private Bitmap croppedBitmap = null;
    float minScale;

    String mCurrentPhotoPath;


    public Bitmap scaleCenterCrop(Bitmap source, int newHeight, int newWidth) {

        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        // Compute the scaling factors to fit the new height and width, respectively.
        // To cover the final image, the final scaling will be the bigger
        // of these two.
        float xScale = (float) newWidth / sourceWidth;
        float yScale = (float) newHeight / sourceHeight;
        float scale = Math.max(xScale, yScale);
        minScale = Math.min(xScale, yScale);

        // Now get the size of the source bitmap when scaled
        float scaledWidth = scale * sourceWidth;
        float scaledHeight = scale * sourceHeight;

        Log.d("INFOS-SIZES", "newWidth = " + newWidth + " | newHeight = " + newHeight);
        Log.d("INFOS-SIZES", "sourceWidth = " + sourceWidth + " | sourceHeight = " + sourceHeight);
        Log.d("INFOS-SIZES", "scaledWidth = " + scaledWidth + " | scaledHeight = " + scaledHeight);

        float sourceScaledWidth = sourceWidth * minScale;
        float sourceScaledHeight = sourceHeight * minScale;

        Log.d("INFOS-SIZES", "sourceScaledWidth = " + (int)sourceScaledWidth + " | sourceScaledHeight = " + (int)sourceScaledHeight);

        Bitmap sourceScaled = Bitmap.createScaledBitmap(source, (int)sourceScaledWidth, (int)sourceScaledHeight, false);

        // Finally, we create a new bitmap of the specified size and draw our new,
        // scaled bitmap onto it.

        Bitmap destRight, destLeft, squareBitmap, mergedImages;

        if (sourceHeight < sourceWidth) {
            Log.d("INFOS-IMAGE-NATURE", "sourceHeight < sourceWidth --> Landscape");
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            sourceScaled = Bitmap.createBitmap(sourceScaled, 0, 0, sourceScaled.getWidth(), sourceScaled.getHeight(), matrix, true);
            Log.d("INFOS-ROTATION", "sourceScaledWidth = " + sourceScaled.getWidth() + " | sourceScaledHeight = " + sourceScaled.getHeight());
        } else {
            Log.d("INFOS-Image Nature", "sourceHeight < sourceWidth --> Portrait");
        }

        if((newWidth - sourceScaled.getWidth()) != 0) { // Add padding

            destRight = Bitmap.createBitmap(((newWidth - sourceScaled.getWidth()) / 2), newHeight, Config.ARGB_8888);
            destLeft = Bitmap.createBitmap(((newWidth - sourceScaled.getWidth()) / 2), newHeight, Config.ARGB_8888);

            Log.d("INFOS-PADDING", "destRightWidth = " + destRight.getWidth() + " | destRightHeight = " + destRight.getHeight());
            Log.d("INFOS-PADDING", "destLeftWidth = " + destLeft.getWidth() + " | destLeftHeight = " + destLeft.getHeight());

            sourceScaled = createSingleImageFromMultipleImages(destLeft, createSingleImageFromMultipleImages(sourceScaled, destRight));

        }

        if (sourceHeight < sourceWidth) {

            Matrix matrix = new Matrix();
            matrix.postRotate(270);
            sourceScaled = Bitmap.createBitmap(sourceScaled, 0, 0, sourceScaled.getWidth(), sourceScaled.getHeight(), matrix, true);
        }

        Log.d("INFOS-Merged", "squareBitmapWidth = " + sourceScaled.getWidth() + " | squareBitmapHeight = " + sourceScaled.getHeight());

        return sourceScaled;
    }

    private Bitmap createSingleImageFromMultipleImages(Bitmap firstImage, Bitmap secondImage) {
        Bitmap result = Bitmap.createBitmap(firstImage.getWidth() + secondImage.getWidth(), firstImage.getHeight(), firstImage.getConfig());
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(firstImage, 0f, 0f, null);
        canvas.drawBitmap(secondImage, firstImage.getWidth(), 0f, null);
        return result;
    }



    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload);

        imageView = (ImageView) findViewById(R.id.imageView);
        takePictureButton = (Button) findViewById(R.id.take_image);
        selectPictureButton = (Button) findViewById(R.id.select_image);
        recognizePictureButton = (Button) findViewById(R.id.recognize_image);
        downloadPictureButton = (Button) findViewById(R.id.download_output);

        takePictureButton.setEnabled(false);
        selectPictureButton.setEnabled(false);
        recognizePictureButton.setEnabled(false);
        downloadPictureButton.setEnabled(false);

        try {

            detector = TensorFlowObjectDetectionAPIModel.create(
                    getAssets(), TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, INPUT_SIZE);
            takePictureButton.setEnabled(true);
            selectPictureButton.setEnabled(true);

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            takePictureButton.setEnabled(false);
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE }, 0);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    takePictureButton.setEnabled(true);
            }
        }
    }


    public void takePhoto(View view) {
        //TODO: launch the camera to take image
        Intent takePicture = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePicture.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "org.tensorflow.demo.fileprovider",
                        photoFile);
                takePicture.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePicture, 0);
            }
        }
    }


    public void pickPhoto(View view) {

        //TODO: launch the photo picker
        Intent pickPicture = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickPicture.setType("image/*");
        pickPicture.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(pickPicture , 1);//one can be replaced with any action code

    }

    public void downloadImage(View view) {
        try {
            if (croppedBitmap != null) {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String imageFileName = "JPEG_" + timeStamp + "_";
                FileOutputStream out = new FileOutputStream(getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/" + imageFileName + ".jpg");
                croppedBitmap.compress(CompressFormat.JPEG, 100, out);
                out.flush();
                out.close();
                Toast.makeText(getApplicationContext(),
                        "Success Download: Image Download",
                        Toast.LENGTH_SHORT).show();
            }
        } catch(Exception e) {}
    }

    public void recognizePhoto(View view){

        if (croppedBitmap != null) {

            final Bitmap mutableBitmap = croppedBitmap.copy(Config.ARGB_8888, true);
            final Canvas canvas = new Canvas(mutableBitmap);

            final Paint paint = new Paint();
            paint.setColor(Color.GREEN);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.0f);

            final ProgressDialog progress = new ProgressDialog(this);
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.setTitle("Recognizing");
            progress.setMessage("Wait while recognizing...");
            progress.setIndeterminate(true);
            progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
            progress.show();

            Thread recognitionThread = new Thread() {
                @Override
                public void run() {
                    final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);

                    Log.d("Predictions Size", "The Results.length ==> " + results.size());

                    for (final Classifier.Recognition result : results) {
                        final RectF location = result.getLocation();
                        if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {
                            Log.d("result.getConfidence", "result.getConfidence ==> " + result.getConfidence());
                            canvas.drawRect(location, paint);
                        }
                    }

                    // To dismiss the dialog
                    progress.dismiss();

                    UploadActivity.this.runOnUiThread(new Runnable()
                    {
                        public void run()
                        {
                            imageView.setImageBitmap(mutableBitmap);
                            croppedBitmap = mutableBitmap;
                            recognizePictureButton.setEnabled(false);
                            downloadPictureButton.setEnabled(true);

                            //Do your UI operations like dialog opening or Toast here
                            Toast.makeText(getApplicationContext(),
                                    "Success Detection: Image Detection",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            };
            recognitionThread.start();

        } else {

            Toast.makeText(getApplicationContext(),
                    "Failed Detection: Image Not Found",
                    Toast.LENGTH_SHORT).show();
        }
    }


    private void setCameraPic() throws IOException {

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath);

        ExifInterface exif = new ExifInterface(mCurrentPhotoPath);
        String orientString = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
        int orientation = orientString != null ? Integer.parseInt(orientString) :  ExifInterface.ORIENTATION_NORMAL;

        int rotationAngle = 0;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotationAngle = 90;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotationAngle = 180;
        if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotationAngle = 270;

        Matrix matrix = new Matrix();
        matrix.postRotate(rotationAngle);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        Log.d("INFOS-SET-CAMERA", "rotatedBitmapWidth = " + rotatedBitmap.getWidth() + " | rotatedBitmapHeight = " + rotatedBitmap.getHeight());

        int bitmapMaxInputSize = Math.max(rotatedBitmap.getWidth(), rotatedBitmap.getHeight());
        if (bitmapMaxInputSize < INPUT_SIZE) INPUT_SIZE = bitmapMaxInputSize;
        else INPUT_SIZE = 800;
        detector.setInputSize(INPUT_SIZE);
        Log.d("INPUT-SIZE-VALUE", "INPUT_SIZE ----> " + INPUT_SIZE);

        croppedBitmap = scaleCenterCrop(rotatedBitmap, INPUT_SIZE, INPUT_SIZE);

        imageView.setImageBitmap(croppedBitmap);
        recognizePictureButton.setEnabled(true);
        downloadPictureButton.setEnabled(false);
    }

    private void setLibraryPic(Bitmap bitmap) {

        int bitmapMaxInputSize = Math.max(bitmap.getWidth(), bitmap.getHeight());
        if (bitmapMaxInputSize < INPUT_SIZE) INPUT_SIZE = bitmapMaxInputSize;
        else INPUT_SIZE = 800;
        detector.setInputSize(INPUT_SIZE);
        Log.d("INPUT SIZE VALUE", "INPUT_SIZE ----> " + INPUT_SIZE);

        croppedBitmap = scaleCenterCrop(bitmap, INPUT_SIZE, INPUT_SIZE);
        imageView.setImageBitmap(croppedBitmap);
        recognizePictureButton.setEnabled(true);
        downloadPictureButton.setEnabled(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        switch(requestCode) {
            case 0:
                if(resultCode == RESULT_OK){

                    try {
                        setCameraPic();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Toast.makeText(getApplicationContext(),
                            "Success : Image from Camera",
                            Toast.LENGTH_SHORT).show();

                } else {

                    Toast.makeText(getApplicationContext(),
                            "Could not load image from Camera",
                            Toast.LENGTH_SHORT).show();
                }

                break;
            case 1:
                if(resultCode == RESULT_OK){

                    Uri selectedImage = imageReturnedIntent.getData();
                    Bitmap imageBitmap = null;
                    try {
                        imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                        setLibraryPic(imageBitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Toast.makeText(getApplicationContext(),
                            "Success : Image from Library",
                            Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(getApplicationContext(),
                            "Could not load image",
                            Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private Bitmap getPath(Uri uri) {

        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String filePath = cursor.getString(column_index);
        Log.d("filePath","filePath --> " + filePath);
        cursor.close();
        // Convert file path into bitmap image using below line.
        Bitmap bitmap = BitmapFactory.decodeFile(filePath);

        return bitmap;
    }

    public void uploadPhoto(View view) {
        try {
            executeMultipartPost();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void executeMultipartPost() throws Exception {

        try {

            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();

            Bitmap bitmap = drawable.getBitmap();

            bitmap.compress(CompressFormat.JPEG, 50, bos);

            byte[] data = bos.toByteArray();

            HttpClient httpClient = new DefaultHttpClient();

            HttpPost postRequest = new HttpPost(

                    "http://10.33.171.5:5005/api/upload_image");

            String fileName = String.format("File_%d.jpg",new Date().getTime());
            ByteArrayBody bab = new ByteArrayBody(data, fileName);

            // File file= new File("/mnt/sdcard/forest.png");

            // FileBody bin = new FileBody(file);

            MultipartEntity reqEntity = new MultipartEntity(

                    HttpMultipartMode.BROWSER_COMPATIBLE);

            reqEntity.addPart("file", bab);

            postRequest.setEntity(reqEntity);
            int timeoutConnection = 60000;
            HttpParams httpParameters = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParameters,
                    timeoutConnection);
            int timeoutSocket = 60000;
            HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
            HttpConnectionParams.setTcpNoDelay(httpParameters, true);

            HttpResponse response = httpClient.execute(postRequest);

            BufferedReader reader = new BufferedReader(new InputStreamReader(

                    response.getEntity().getContent(), "UTF-8"));

            String sResponse;

            StringBuilder s = new StringBuilder();

            while ((sResponse = reader.readLine()) != null) {

                s = s.append(sResponse);

            }

            System.out.println("Response: " + s);

        } catch (Exception e) {

            // handle exception here
            e.printStackTrace();

            // Log.e(e.getClass().getName(), e.getMessage());

        }

    }
}
