package app.vjsantojaca.merinosa.com.centinela;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
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

import org.json.JSONException;
import org.json.JSONObject;


import app.vjsantojaca.merinosa.com.centinela.services.gcm.RegistrationIntentService;
import app.vjsantojaca.merinosa.com.centinela.volley.ServerStatusRequestObject;
import app.vjsantojaca.merinosa.com.centinela.volley.VolleyS;

/*
* Developer Víctor Santoja
 * Centinela App pertenece al proyecto Centinela
*/
public class LoginActivity extends Activity
{
    private static final String TAG = LoginActivity.class.getName();
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private TextView tvPass;
    private EditText etPass;
    private EditText etNumber;
    private Button buttonPass;
    private ProgressBar pbPass;

    private BroadcastReceiver mRegistrationBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load);

        if( !App.activePolicyMananger() )
        {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, App.getComponentName());
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Centinela pertenece al proyecto centinela. Es propiedad de MerinoSA.");
            startActivityForResult(intent, 47);
        }

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
                                                etPass.setVisibility(View.GONE);
                                                buttonPass.setVisibility(View.GONE);
                                                pbPass.setVisibility(View.VISIBLE);
                                                if (checkPlayServices()) {
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
    protected void onResume() {
        super.onResume();
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
}