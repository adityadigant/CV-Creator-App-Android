package com.e.cv_creator_app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity {
    // here you can put your url address
    // @BindView(R.id.webView)
    WebView webView;
    public JsCallbackName jsCallbackName = null;
    private ValueCallback<Uri[]> mUploadMsg;
    private static final int PHOTO_CAMERA_REQUEST = 113;
    private static final int GALLERY_REQUEST = 118;
    private static final int CAMERA_ONLY_REQUEST = 111;
    private String pictureImagePath = "";
    private Uri cameraUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_DENIED) {

                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);

                Log.d("permission", "Access Denied: Fill Address Field Manually");
                String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
                requestPermissions(permissions, 1);


            }
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED) {
                Log.d("permission", "permission denied to WRITE_EXTERNAL_STORAGE - requesting it");
                String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                requestPermissions(permissions, 1);
            }


            if (checkSelfPermission(Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_DENIED) {
                Log.d("permission", "permission denied to WRITE_EXTERNAL_STORAGE - requesting it");
                String[] permissions = {Manifest.permission.CAMERA};
                requestPermissions(permissions, 1);
            }
        }





        webView = (WebView)findViewById(R.id.webView);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setUseWideViewPort(true);

        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        //webView.setWebViewClient(new GeoWebViewClient());
        // Below required for geolocation

        webView.getSettings().setAllowFileAccess(true);

        webView.getSettings().setGeolocationEnabled(true);
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }
        });
        webView.loadUrl("https://adityadigant.github.io/CV-Creator-New/");
        webView.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                // callback.invoke(String origin, boolean allow, boolean remember);
                callback.invoke(origin, true, false);

            }

            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> uploadMsg, WebChromeClient.FileChooserParams fileChooserParams) {
                mUploadMsg = uploadMsg;
                final String[] PERMISSIONS_PHOTO_CAMERA = {
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                };

                final String[] PERMISSIONS_PHOTO_GALLERY = {
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                };

                final Context context = MainActivity.this;
                final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Choose Image Location");
                builder.setCancelable(true);
                builder.setItems(new CharSequence[]
                                {"Camera", "Gallery"},
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        if (!hasPermissions(MainActivity.this, PERMISSIONS_PHOTO_CAMERA)) {

                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_PHOTO_CAMERA, PHOTO_CAMERA_REQUEST);
                                            } else {
                                                String title = "Storage and Camera Permission";
                                                String message = "Allow Storage and Camera Permission to get Picture";
                                                showPermissionNotAllowedDialog(title, message, true);
                                            }
                                        } else {

                                            startActivityForResult(photoCameraIntent(), PHOTO_CAMERA_REQUEST);

                                        }

                                        break;
                                    case 1:
                                        if (!hasPermissions(MainActivity.this, PERMISSIONS_PHOTO_GALLERY)) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_PHOTO_GALLERY, GALLERY_REQUEST);
                                            } else {
                                                String title = "Storage and Camera Permission";
                                                String message = "Allow Storage and Camera Permission to get Picture";
                                                showPermissionNotAllowedDialog(title, message, true);
                                            }
                                        } else {
                                            startActivityForResult(photoGalleryIntent(), GALLERY_REQUEST);
                                        }
                                        break;
                                }
                            }
                        });
                builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        mUploadMsg.onReceiveValue(null);
                        mUploadMsg = null;
                    }
                });
                builder.create().show();
                return true;
            }
        });



    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);


        switch (requestCode) {
            case CAMERA_ONLY_REQUEST:
                if (resultCode == RESULT_OK) {
                    File imgFile = new File(pictureImagePath);
                    String callbackName = jsCallbackName.getName();
                    if (imgFile.exists()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                        final int maxWidth = deviceWidth();
                        final int maxHeight = deviceHeight();

                        int inWidth = bitmap.getWidth();
                        int inHeight = bitmap.getHeight();
                        int outWidth = inWidth;
                        int outHeight = inHeight;

                        if (inWidth > inHeight) {
                            if (inWidth > maxWidth) {
                                outWidth = maxWidth;
                                outHeight = (inHeight * maxWidth) / inWidth;
                            }
                        } else {
                            if (inHeight > maxHeight) {
                                outHeight = maxHeight;
                                outWidth = (inWidth * maxHeight) / inHeight;
                            }
                        }
                        ;


                        Log.d("outWidth", "" + outWidth);
                        Log.d("outHeight", "" + outHeight);
                        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, false);


                        try {
                            Bitmap changedBit = null;
                            ExifInterface ei = new ExifInterface(imgFile.getAbsolutePath());
                            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);



                            webView.loadUrl("javascript:" + callbackName + "('" + encodeImage(changedBit) + "')");


                        } catch (IOException e) {
                            webView.loadUrl("javascript:" + callbackName + "('" + encodeImage(scaled) + "')");
                            e.printStackTrace();

                        }
                        return;
                    }
                    webView.loadUrl("javascript:" + callbackName + "Failed('Please Try Again')");

                }
//                intentStarted = false;
                break;

            case GALLERY_REQUEST:
                if (resultCode == RESULT_OK) {
                    Uri uri = intent.getData();

                    if (uri != null) {
                        mUploadMsg.onReceiveValue(new Uri[]{uri});
                        mUploadMsg = null;
                    } else {
                        mUploadMsg.onReceiveValue(null);
                        mUploadMsg = null;

                    }
                } else {
                    mUploadMsg.onReceiveValue(null);
                    mUploadMsg = null;

                }
//                intentStarted = false;
                break;
            case PHOTO_CAMERA_REQUEST:
                if (resultCode == RESULT_OK) {
                    File imgFile = new File(pictureImagePath);
                    if (imgFile.exists()) {
                        if (cameraUri != null) {
                            mUploadMsg.onReceiveValue(new Uri[]{cameraUri});
                            mUploadMsg = null;
                        } else {
                            mUploadMsg.onReceiveValue(null);
                            mUploadMsg = null;
                        }
                    } else {
                        pictureImagePath = null;
                        Toast.makeText(MainActivity.this, "Please Try Again", Toast.LENGTH_LONG).show();
                        mUploadMsg.onReceiveValue(null);
                        mUploadMsg = null;
                    }
                } else {
                    mUploadMsg.onReceiveValue(null);
                    mUploadMsg = null;

                }
//                intentStarted = false;
                break;

        }
    }

    public class JsCallbackName {
        String functionName;

        public JsCallbackName(String name) {
            this.functionName = name;
        }

        public String getName() {
            return functionName;
        }
    }

    public int deviceWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;

    }

    public int deviceHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;

    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {


        if (requestCode == CAMERA_ONLY_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {

                startActivityForResult(photoCameraIntent(), CAMERA_ONLY_REQUEST);
//                    intentStarted = true;

            }
        } else if (requestCode == PHOTO_CAMERA_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {

                startActivityForResult(photoCameraIntent(), PHOTO_CAMERA_REQUEST);
//                intentStarted = true;

            } else {
                pictureImagePath = null;
                cameraUri = null;
            }
        } else if (requestCode == GALLERY_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(photoGalleryIntent(), GALLERY_REQUEST);
//                intentStarted = true;
            } else {
                mUploadMsg.onReceiveValue(null);
                mUploadMsg = null;
            }
        }
    }



    private void showPermissionNotAllowedDialog(String title, String message, boolean cancelable) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_Material_Light_Dialog_Alert);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(cancelable);
        builder.show();
    }
    private Intent photoCameraIntent() {
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = timeStamp + ".jpg";

        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        pictureImagePath = storageDir.getAbsolutePath() + "/" + imageFileName;
        File file = new File(pictureImagePath);
        Uri outputFileUri = Uri.fromFile(file);
        cameraUri = outputFileUri;
        takePicture.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        return takePicture;

    }
    private Intent photoGalleryIntent() {
        Intent choosePicture = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        choosePicture.setType("image/*");
        return choosePicture;
    }
    private static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private String encodeImage(Bitmap bm) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
        Log.d("encoded", encoded);
        return encoded;
    }
    private class viewLoadJavaInterface {
        Context mContext;

        viewLoadJavaInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface


        public void startCamera(String functionName) {


            String[] PERMISSIONS_CAMERA = {
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            jsCallbackName = new JsCallbackName(functionName);


            if (!hasPermissions(MainActivity.this, PERMISSIONS_CAMERA)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_CAMERA, CAMERA_ONLY_REQUEST);

                } else {
                    String title = "Storage and Camera Permission";
                    String message = "Allow Storage and Camera Permission to get Picture";
                    showPermissionNotAllowedDialog(title, message, true);
                }

            } else {
                startActivityForResult(photoCameraIntent(), CAMERA_ONLY_REQUEST);
            }
        }




    }

}