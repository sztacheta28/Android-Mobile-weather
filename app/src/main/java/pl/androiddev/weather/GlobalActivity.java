package pl.androiddev.weather;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import pl.androiddev.weather.activity.FirstLaunch;
import pl.androiddev.weather.activity.WeatherActivity;
import pl.androiddev.weather.preferences.Preferences;
import pl.androiddev.weather.preferences.Prefs;

public class GlobalActivity extends AppCompatActivity {

    public static Preferences cp;
    public static Prefs prefs;
    public static int i = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_global);
        Log.i("Loaded" , "Global");
    }

    @Override
    protected void onResume() {
        cp = new Preferences(this);
        prefs = new Prefs(this);
        super.onResume();

        if (!cp.getPrefs().getBoolean("first" , true)) {
            prefs.setLaunched();
            prefs.setCity(cp.getCity());
        }

        super.onResume();
        Intent intent;

        if (prefs.getLaunched()) {
            intent = new Intent(GlobalActivity.this, WeatherActivity.class);
        }
        else {
            intent = new Intent(GlobalActivity.this, FirstLaunch.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
