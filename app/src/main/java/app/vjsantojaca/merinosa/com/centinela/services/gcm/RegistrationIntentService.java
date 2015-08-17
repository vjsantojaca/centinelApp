package app.vjsantojaca.merinosa.com.centinela.services.gcm;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import org.json.JSONException;
import org.json.JSONObject;

import app.vjsantojaca.merinosa.com.centinela.App;
import app.vjsantojaca.merinosa.com.centinela.Constants;
import app.vjsantojaca.merinosa.com.centinela.LoginActivity;
import app.vjsantojaca.merinosa.com.centinela.Utils;
import app.vjsantojaca.merinosa.com.centinela.volley.ServerStatusRequestObject;
import app.vjsantojaca.merinosa.com.centinela.volley.VolleyS;

/*
* Developer Víctor Santoja
 * Centinela App pertenece al proyecto Centinela
*/
public class RegistrationIntentService extends IntentService
{
    private static final String TAG = RegistrationIntentService.class.getName();

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);


        try {
            // In the (unlikely) event that multiple refresh operations occur simultaneously,
            // ensure that they are processed sequentially.
            synchronized (TAG) {
                // Initially this call goes out to the network to retrieve the token, subsequent calls
                // are local.
                InstanceID instanceID = InstanceID.getInstance(this);
                String token = instanceID.getToken("877536983813", GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                //String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                Log.i(TAG, "GCM Registration Token: " + token);

                sendRegistrationToServer(token);


                // You should store a boolean that indicates whether the generated token has been
                // sent to your server. If the boolean is false, send the token to your server,
                // otherwise your server should have already received the token.
                sharedPreferences.edit().putBoolean(Constants.SENT_TOKEN_TO_SERVER, true).apply();
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
            sharedPreferences.edit().putBoolean(Constants.SENT_TOKEN_TO_SERVER, false).apply();
        }
        // Notify UI that registration has completed, so the progress indicator can be hidden.
        Intent registrationComplete = new Intent(Constants.REGISTRATION_COMPLETE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }


    /**
     * Persist registration to third-party servers.
     *
     * Modify this method to associate the user's GCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token)
    {
        VolleyS volleyS = App.getVolley();
        String url = Constants.URL_SERVER + Constants.PATH_GCM;
        JSONObject object = new JSONObject();
        try {
            object.put("regId", token);
            object.put("idUser", Utils.getNumberPhone());
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
                            Log.d(TAG, "Guardado correctamente en el servidor");
                            Intent intent = new Intent(App.getAppContext(), LoginActivity.class);
                            startService(intent);
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
}