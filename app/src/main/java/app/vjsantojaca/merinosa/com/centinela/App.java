package app.vjsantojaca.merinosa.com.centinela;

import android.app.Application;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import app.vjsantojaca.merinosa.com.centinela.services.DeviceAdmReceiver;

/*
* Developer Víctor Santoja
 * Centinela App pertenece al proyecto Centinela
*/
public class App extends Application
{
    private final static String TAG = App.class.getName();

    private static Context context;
    private static DevicePolicyManager devicePolicyManager;
    private static ComponentName componentName;

    public void onCreate()
    {
        super.onCreate();
        Log.i(TAG, "App Creada");

        App.componentName = new ComponentName(this, DeviceAdmReceiver.class);
        App.devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        App.context = getApplicationContext();
    }

    public static Context getAppContext() { return App.context; }

    public static boolean activePolicyMananger() { return devicePolicyManager.isAdminActive(componentName); }

    public static ComponentName getComponentName(){ return componentName; }
}
