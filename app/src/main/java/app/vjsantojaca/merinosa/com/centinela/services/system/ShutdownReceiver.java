package app.vjsantojaca.merinosa.com.centinela.services.system;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import app.vjsantojaca.merinosa.com.centinela.App;
import app.vjsantojaca.merinosa.com.centinela.Constants;
import app.vjsantojaca.merinosa.com.centinela.SystemEnum;
import app.vjsantojaca.merinosa.com.centinela.Utils;
import app.vjsantojaca.merinosa.com.centinela.volley.ServerStatusRequestObject;
import app.vjsantojaca.merinosa.com.centinela.volley.VolleyS;

/**
 * Created by vsantoja on 17/08/15.
 */
public class ShutdownReceiver extends BroadcastReceiver
{
    public static final String TAG = ShutdownReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent.getAction().equals(Intent.ACTION_SHUTDOWN))
        {
            VolleyS volleyS = App.getVolley();
            String url = Constants.URL_SERVER + Constants.PATH_SYSTEM;
            JSONObject object = new JSONObject();
            try {
                object.put("id", Utils.getNumberPhone());
                object.put("type", SystemEnum.SHUTDOWN);
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
                        public void onErrorResponse(VolleyError error) {
                            Log.d(TAG, "Error Respuesta: " + error);
                        }
                    }
            );

            volleyS.getRequestQueue().add(serverStatusRequest);
        }
    }
}
