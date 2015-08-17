package app.vjsantojaca.merinosa.com.centinela;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TextView;

/*
* Developer Víctor Santoja
 * Centinela App pertenece al proyecto Centinela
*/
public class MessageActivity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String message = prefs.getString("message", "");

        TextView textView = (TextView) findViewById(R.id.textMessage);
        textView.setText(message);
    }
}