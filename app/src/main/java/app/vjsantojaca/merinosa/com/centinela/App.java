package app.vjsantojaca.merinosa.com.centinela;

import android.app.Application;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import app.vjsantojaca.merinosa.com.centinela.services.DeviceAdmReceiver;
import app.vjsantojaca.merinosa.com.centinela.services.system.ShutdownReceiver;
import app.vjsantojaca.merinosa.com.centinela.services.system.StartReceiver;
import app.vjsantojaca.merinosa.com.centinela.volley.VolleyS;

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
    private static VolleyS volley;

    public void onCreate()
    {
        super.onCreate();
        Log.i(TAG, "App Creada");

        App.context = getApplicationContext();
        App.volley = VolleyS.getInstance(App.context);
        App.componentName = new ComponentName(this, DeviceAdmReceiver.class);
        App.devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        IntentFilter filterShutdown = new IntentFilter(Intent.ACTION_SHUTDOWN);
        BroadcastReceiver mReceiverShutdown = new ShutdownReceiver();
        registerReceiver(mReceiverShutdown, filterShutdown);

        IntentFilter filterStart = new IntentFilter(Intent.ACTION_BOOT_COMPLETED);
        BroadcastReceiver mReceiverStart = new StartReceiver();
        registerReceiver(mReceiverStart, filterStart);

    }

    public static Context getAppContext() { return App.context; }

    public static boolean activePolicyMananger() { return devicePolicyManager.isAdminActive(componentName); }

    public static ComponentName getComponentName(){ return componentName; }

    public static VolleyS getVolley(){ return volley; }
}