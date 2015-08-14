package app.vjsantojaca.merinosa.com.centinela;

import android.app.Application;
import android.content.Context;
import android.util.Log;

/*
* Developer Víctor Santoja
 * Centinela App pertenece al proyecto Centinela
*/
public class App extends Application
{
    private static Context context;
    private final static String TAG = App.class.getName();

    public void onCreate()
    {
        super.onCreate();
        Log.i(TAG, "App Creada");

        App.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return App.context;
    }
}
