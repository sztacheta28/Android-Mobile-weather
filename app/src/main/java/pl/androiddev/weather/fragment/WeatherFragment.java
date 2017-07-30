package pl.androiddev.weather.fragment;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.Toast;

import pl.androiddev.weather.GlobalActivity;
import pl.androiddev.weather.R;
import pl.androiddev.weather.activity.FirstLaunch;
import pl.androiddev.weather.activity.WeatherActivity;
import pl.androiddev.weather.internet.CheckConnection;
import pl.androiddev.weather.internet.FetchWeather;
import pl.androiddev.weather.model.Info;
import pl.androiddev.weather.model.WeatherFort;
import pl.androiddev.weather.model.WeatherInfo;
import pl.androiddev.weather.permissions.GPSTracker;
import pl.androiddev.weather.permissions.Permissions;
import pl.androiddev.weather.preferences.Prefs;
import pl.androiddev.weather.utils.Constants;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import butterknife.BindView;
import butterknife.ButterKnife;
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;

public class WeatherFragment extends Fragment {
    Typeface weatherFont;
    @BindView(R.id.button1) TextView button;
    TextView detailsField[] = new TextView[10] , weatherIcon[] = new TextView[11];
    @BindView(R.id.wind_view) TextView windView;
    @BindView(R.id.humidity_view) TextView humidityView;
    @BindView(R.id.direction_view) TextView directionView;
    @BindView(R.id.daily_view) TextView dailyView;
    @BindView(R.id.updated_field) TextView updatedField;
    @BindView(R.id.city_field) TextView cityField;
    @BindView(R.id.sunrise_view) TextView sunriseView;
    @BindView(R.id.sunset_view) TextView sunsetView;
    @BindView(R.id.sunrise_icon) TextView sunriseIcon;
    @BindView(R.id.sunset_icon) TextView sunsetIcon;
    @BindView(R.id.wind_icon) TextView windIcon;
    @BindView(R.id.humidity_icon) TextView humidityIcon;
    @BindView(R.id.horizontalScrollView) HorizontalScrollView horizontalScrollView;
    double tc;
    Handler handler;
    BottomSheetDialogFragment bottomSheetDialogFragment;
    WeatherInfo json0;
    WeatherFort json1;
    @BindView(R.id.swipe) SwipeRefreshLayout swipeView;
    CheckConnection cc;
    Info json;
    MaterialDialog pd;
    FetchWeather wt;
    Prefs preferences;
    GPSTracker gps;
    View rootView;
    Permissions permission;

    public WeatherFragment() {
        handler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_weather, container, false);
        ButterKnife.bind(this , rootView);
        MaterialDialog.Builder builder = new MaterialDialog.Builder(this.getActivity())
                .title(getString(R.string.please_wait))
                .content(getString(R.string.loading))
                .cancelable(false)
                .progress(true , 0);
        pd = builder.build();
        setHasOptionsMenu(true);
        preferences = new Prefs(getContext());
        weatherFont = Typeface.createFromAsset(getActivity().getAssets(), "fonts/weather.ttf");
        Bundle bundle = getArguments();
        int mode;
        if (bundle != null)
            mode = bundle.getInt(Constants.MODE , 0);
        else
            mode = 0;
        if (mode == 0)
            updateWeatherData(preferences.getCity(), null, null);
        else
            updateWeatherData(null , Float.toString(preferences.getLatitude()) , Float.toString(preferences.getLongitude()));
        gps = new GPSTracker(getContext());
        cityField.setTextColor(ContextCompat.getColor(getContext() , R.color.textColor));
        updatedField.setTextColor(ContextCompat.getColor(getContext() , R.color.textColor));
        humidityView.setTextColor(ContextCompat.getColor(getContext() , R.color.textColor));
        sunriseIcon.setTextColor(ContextCompat.getColor(getContext() , R.color.yellow));
        sunriseIcon.setTypeface(weatherFont);
        sunriseIcon.setText(getActivity().getString(R.string.sunrise_icon));
        sunsetIcon.setTextColor(ContextCompat.getColor(getContext() , R.color.orange));
        sunsetIcon.setTypeface(weatherFont);
        sunsetIcon.setText(getActivity().getString(R.string.sunset_icon));
        windIcon.setTextColor(ContextCompat.getColor(getContext() , R.color.md_white_1000));
        windIcon.setTypeface(weatherFont);
        windIcon.setText(getActivity().getString(R.string.speed_icon));
        humidityIcon.setTextColor(ContextCompat.getColor(getContext() , R.color.blue));
        humidityIcon.setTypeface(weatherFont);
        humidityIcon.setText(getActivity().getString(R.string.humidity_icon));
        windView.setTextColor(ContextCompat.getColor(getContext() , R.color.textColor));
        swipeView.setColorSchemeResources(R.color.red, R.color.green , R.color.blue , R.color.yellow , R.color.orange);
        swipeView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeView.setRefreshing(true);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        changeCity(preferences.getCity());
                    }
                });
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        swipeView.setRefreshing(false);
                    }
                }, 10000);
            }
        });
        directionView.setTypeface(weatherFont);
        directionView.setTextColor(ContextCompat.getColor(getContext() , R.color.textColor));
        dailyView.setText(getString(R.string.daily));
        dailyView.setTextColor(ContextCompat.getColor(getContext() , R.color.textColor));
        sunriseView.setTextColor(ContextCompat.getColor(getContext() , R.color.textColor));
        sunsetView.setTextColor(ContextCompat.getColor(getContext() , R.color.textColor));
        button.setTextColor(ContextCompat.getColor(getContext() , R.color.textColor));
        pd.show();
        for (int i = 0; i < 11; ++i)
        {
            String f = "details_view" + (i + 1) , g = "weather_icon" + (i + 1);
            if (i != 10) {
                int resID = getResources().getIdentifier(f, "id", getContext().getPackageName());
                detailsField[i] = (TextView) rootView.findViewById(resID);
                detailsField[i].setTextColor(ContextCompat.getColor(getContext() , R.color.textColor));
            }
            int resIDI = getResources().getIdentifier(g, "id" , getContext().getPackageName());
            weatherIcon[i] = (TextView)rootView.findViewById(resIDI);
            weatherIcon[i].setTypeface(weatherFont);
            weatherIcon[i].setTextColor(ContextCompat.getColor(getContext() , R.color.textColor));
        }
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu , MenuInflater inflater) {
        getActivity().getMenuInflater().inflate(R.menu.menu_weather, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.location :
                permission = new Permissions(getContext());
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION} , Constants.READ_COARSE_LOCATION);
                break;
            case R.id.search :
                showMenuInputDialog();
                break;
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            cc = new CheckConnection(getContext());
            if (!cc.isNetworkAvailable())
                showNoInternet();
            else {
                pd.show();
                updateWeatherData(preferences.getCity(), null, null);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        gps.stopUsingGPS();
    }

    private void updateWeatherData(final String city, final String lat, final String lon) {
        wt = new FetchWeather(getContext());
        new Thread() {
            public void run() {
                try {
                    if (lat == null && lon == null) {
                        json = wt.execute(city).get();
                    } else if (city == null) {
                        json = wt.execute(lat, lon).get();
                    }
                }
                catch (InterruptedException iex) {
                    Log.e("InterruptedException" , "iex");
                }
                catch (ExecutionException eex) {
                    Log.e("ExecutionException" , "eex");
                }
                if (pd.isShowing())
                    pd.dismiss();
                if (swipeView != null && swipeView.isRefreshing())
                    swipeView.post(new Runnable() {
                        @Override
                        public void run() {
                            swipeView.setRefreshing(false);
                        }
                    });
                if (json == null) {
                    preferences.setCity(preferences.getLastCity());
                    handler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(getActivity(),
                                    getActivity().getString(R.string.place_not_found),
                                    Toast.LENGTH_LONG).show();
                            GlobalActivity.i = 1;
                            if (!preferences.getLaunched()) {
                                FirstStart();
                            } else {
                                cc = new CheckConnection(getContext());
                                if (!cc.isNetworkAvailable()) {
                                    showNoInternet();
                                }
                                else {
                                    if (pd.isShowing())
                                        pd.dismiss();
                                    showInputDialog();
                                }
                            }
                        }
                    });
                }
                else {
                    handler.post(new Runnable() {
                        public void run() {
                            preferences.setLaunched();
                            renderWeather(json);
                            Snackbar snackbar = Snackbar.make(rootView, "Loaded Weather Data", 500);
                            snackbar.show();
                            //function();
                            if (!preferences.getv3TargetShown())
                                showTargets();
                            if (pd.isShowing())
                                pd.dismiss();
                            preferences.setLastCity(json.day.getName());
                            ((WeatherActivity) getActivity()).createShortcuts();
                        }
                    });
                }
            }
        }.start();
    }

    public void FirstStart() {
        if (pd.isShowing())
            pd.dismiss();
        Intent intent = new Intent(getActivity(), FirstLaunch.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    public List<WeatherFort.WeatherList> getDailyJson() {
        return json.fort.getList();
    }

    public void changeCity(String city)
    {
        if (!swipeView.isRefreshing())
            pd.show();
        updateWeatherData(city, null, null);
        preferences.setCity(city);
    }

    public void changeCity(String lat , String lon)
    {
        pd.show();
        updateWeatherData(null, lat, lon);
    }

    private void showInputDialog() {
        new MaterialDialog.Builder(this.getActivity())
                .title(getString(R.string.change_city))
                .content(getString(R.string.could_not_find))
                .negativeText(getString(R.string.cancel))
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog , @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .input(null, null, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, @NonNull CharSequence input) {
                        changeCity(input.toString());
                    }
                })
                .cancelable(false)
                .show();
    }

    private void showTargets() {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    new MaterialTapTargetPrompt.Builder(getActivity())
                            .setTarget(R.id.search)
                            .setBackgroundColour(ContextCompat.getColor(getContext() , R.color.md_light_blue_400))
                            .setFocalColour(ContextCompat.getColor(getContext() , R.color.colorAccent))
                            .setPrimaryText(getString(R.string.target_search_title))
                            .setSecondaryText(getString(R.string.target_search_content))
                            .setIconDrawableColourFilter(ContextCompat.getColor(getContext() , R.color.md_black_1000))
                            .setOnHidePromptListener(new MaterialTapTargetPrompt.OnHidePromptListener() {
                                @Override
                                public void onHidePrompt(MotionEvent event, boolean tappedTarget) {

                                }

                                @Override
                                public void onHidePromptComplete() {
                                    showRefresh();
                                }
                            })
                            .show();
                }
            }, 1500);
    }

    private void showLocTarget() {
        new MaterialTapTargetPrompt.Builder(getActivity())
                .setTarget(R.id.location)
                .setBackgroundColour(ContextCompat.getColor(getContext() , R.color.md_light_blue_400))
                .setPrimaryText(getString(R.string.target_location_title))
                .setFocalColour(ContextCompat.getColor(getContext() , R.color.colorAccent))
                .setSecondaryText(getString(R.string.target_location_content))
                .setOnHidePromptListener(new MaterialTapTargetPrompt.OnHidePromptListener() {
                    @Override
                    public void onHidePrompt(MotionEvent event, boolean tappedTarget) {
                        preferences.setv3TargetShown(true);
                    }

                    @Override
                    public void onHidePromptComplete() {
                        preferences.setv3TargetShown(true);
                    }
                })
                .show();
    }

    private void showRefresh() {
        new MaterialTapTargetPrompt.Builder(getActivity())
                .setTarget(R.id.toolbar)
                .setBackgroundColour(ContextCompat.getColor(getContext() , R.color.md_light_blue_400))
                .setPrimaryText(getString(R.string.target_refresh_title))
                .setFocalColour(ContextCompat.getColor(getContext() , R.color.colorAccent))
                .setSecondaryText(getString(R.string.target_refresh_content))
                .setOnHidePromptListener(new MaterialTapTargetPrompt.OnHidePromptListener() {
                    @Override
                    public void onHidePrompt(MotionEvent event, boolean tappedTarget) {

                    }

                    @Override
                    public void onHidePromptComplete()
                    {
                        showLocTarget();
                    }
                })
                .show();
    }

    public void showNoInternet() {
        new MaterialDialog.Builder(getContext())
                .title(getString(R.string.no_internet_title))
                .cancelable(false)
                .content(getString(R.string.no_internet_content))
                .positiveText(getString(R.string.no_internet_mobile_data))
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        Intent intent = new Intent();
                        intent.setComponent(new ComponentName("com.android.settings","com.android.settings.Settings$DataUsageSummaryActivity"));
                        startActivityForResult(intent , 0);
                    }
                })
                .negativeText(getString(R.string.no_internet_wifi))
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS) , 0);
                    }
                })
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode ,
                                           @NonNull String permissions[] ,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.READ_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showCity();
                } else {
                    permission.permissionDenied();
                }
                break;
            }
        }
    }

    private void showCity() {
        gps = new GPSTracker(getContext());
        if (!gps.canGetLocation())
            gps.showSettingsAlert();
        else {
            String lat = gps.getLatitude();
            String lon = gps.getLongitude();
            changeCity(lat, lon);
        }
    }

    private void setWeatherIcon(int id , int i) {
        String icon = "";
        int color = ContextCompat.getColor(getContext() , R.color.red);;

        if (i == 10) {
            if (checkDay())
                switch (id) {
                    case 501:
                        icon = getActivity().getString(R.string.day_drizzle);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 500:
                        icon = getActivity().getString(R.string.day_drizzle);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 502:
                        icon = getActivity().getString(R.string.day_rainy);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 503:
                        icon = getActivity().getString(R.string.day_rainy);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 504:
                        icon = getActivity().getString(R.string.day_rainy);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 511:
                        icon = getActivity().getString(R.string.day_rain_wind);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 520:
                        icon = getActivity().getString(R.string.day_rain_drizzle);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 521:
                        icon = getActivity().getString(R.string.day_drizzle);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 522:
                        icon = getActivity().getString(R.string.day_thunder);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 531:
                        icon = getActivity().getString(R.string.day_thunder);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 200:
                        icon = getActivity().getString(R.string.day_thunder);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 201:
                        icon = getActivity().getString(R.string.day_thunder);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 202:
                        icon = getActivity().getString(R.string.day_thunder);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 210:
                        icon = getActivity().getString(R.string.day_thunder);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 211:
                        icon = getActivity().getString(R.string.day_thunder);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 212:
                        icon = getActivity().getString(R.string.day_thunder);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 221:
                        icon = getActivity().getString(R.string.day_thunder);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 230:
                        icon = getActivity().getString(R.string.day_thunder);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 231:
                        icon = getActivity().getString(R.string.day_thunder);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 232:
                        icon = getActivity().getString(R.string.day_thunder);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 300:
                        icon = getActivity().getString(R.string.day_rain_drizzle);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 301:
                        icon = getActivity().getString(R.string.day_rain_drizzle);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 302:
                        icon = getActivity().getString(R.string.day_heavy_drizzle);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 310:
                        icon = getActivity().getString(R.string.day_rain_drizzle);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 311:
                        icon = getActivity().getString(R.string.day_rain_drizzle);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 312:
                        icon = getActivity().getString(R.string.day_heavy_drizzle);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 313:
                        icon = getActivity().getString(R.string.day_rain_drizzle);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 314:
                        icon = getActivity().getString(R.string.day_heavy_drizzle);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 321:
                        icon = getActivity().getString(R.string.day_heavy_drizzle);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 600:
                        icon = getActivity().getString(R.string.day_snowy);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 601:
                        icon = getActivity().getString(R.string.day_snowy);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 602:
                        icon = getActivity().getString(R.string.snow);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 611:
                        icon = getActivity().getString(R.string.sleet);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 612:
                        icon = getActivity().getString(R.string.day_snowy);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 903:
                    case 615:
                        icon = getActivity().getString(R.string.day_snowy);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 616:
                        icon = getActivity().getString(R.string.day_snowy);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 620:
                        icon = getActivity().getString(R.string.day_snowy);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 621:
                        icon = getActivity().getString(R.string.day_snowy);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 622:
                        icon = getActivity().getString(R.string.day_snowy);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 701:
                    case 702:
                    case 721:
                        icon = getActivity().getString(R.string.smoke);
                        color = ContextCompat.getColor(getContext() , R.color.gray);
                        break;
                    case 751:
                    case 761:
                    case 731:
                        icon = getActivity().getString(R.string.dust);
                        color = ContextCompat.getColor(getContext() , R.color.yellow);
                        break;
                    case 741:
                        icon = getActivity().getString(R.string.fog);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 762:
                        icon = getActivity().getString(R.string.volcano);
                        color = ContextCompat.getColor(getContext() , R.color.red);
                        break;
                    case 771:
                    case 900:
                    case 781:
                        icon = getActivity().getString(R.string.tornado);
                        color = ContextCompat.getColor(getContext() , R.color.gray);
                        break;
                    case 904:
                        icon = getActivity().getString(R.string.day_clear);
                        color = ContextCompat.getColor(getContext() , R.color.yellow);
                        break;
                    case 800:
                        icon = getActivity().getString(R.string.day_clear);
                        color = ContextCompat.getColor(getContext() , R.color.yellow);
                        break;
                    case 801:
                        icon = getActivity().getString(R.string.day_cloudy);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 802:
                        icon = getActivity().getString(R.string.day_cloudy);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 803:
                        icon = getActivity().getString(R.string.day_cloudy);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 804:
                        icon = getActivity().getString(R.string.day_cloudy);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 901:
                        icon = getActivity().getString(R.string.storm_showers);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 902:
                        icon = getActivity().getString(R.string.hurricane);
                        color = ContextCompat.getColor(getContext() , R.color.gray);
                        break;
                }
            else
                switch (id) {
                    case 501:
                        icon = getActivity().getString(R.string.night_drizzle);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 500:
                        icon = getActivity().getString(R.string.night_drizzle);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 502:
                        icon = getActivity().getString(R.string.night_rainy);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 503:
                        icon = getActivity().getString(R.string.night_rainy);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 504:
                        icon = getActivity().getString(R.string.night_rainy);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 511:
                        icon = getActivity().getString(R.string.night_rain_wind);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 520:
                        icon = getActivity().getString(R.string.night_rain_drizzle);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 521:
                        icon = getActivity().getString(R.string.night_drizzle);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 522:
                        icon = getActivity().getString(R.string.night_thunder);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 531:
                        icon = getActivity().getString(R.string.night_thunder);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 200:
                        icon = getActivity().getString(R.string.night_thunder);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 201:
                        icon = getActivity().getString(R.string.night_thunder);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 202:
                        icon = getActivity().getString(R.string.night_thunder);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 210:
                        icon = getActivity().getString(R.string.night_thunder);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 211:
                        icon = getActivity().getString(R.string.night_thunder);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 212:
                        icon = getActivity().getString(R.string.night_thunder);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 221:
                        icon = getActivity().getString(R.string.night_thunder);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 230:
                        icon = getActivity().getString(R.string.night_thunder);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 231:
                        icon = getActivity().getString(R.string.night_thunder);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 232:
                        icon = getActivity().getString(R.string.night_thunder);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 300:
                        icon = getActivity().getString(R.string.night_rain_drizzle);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 301:
                        icon = getActivity().getString(R.string.night_rain_drizzle);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 302:
                        icon = getActivity().getString(R.string.night_heavy_drizzle);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 310:
                        icon = getActivity().getString(R.string.night_rain_drizzle);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 311:
                        icon = getActivity().getString(R.string.night_rain_drizzle);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 312:
                        icon = getActivity().getString(R.string.night_heavy_drizzle);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 313:
                        icon = getActivity().getString(R.string.night_rain_drizzle);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 314:
                        icon = getActivity().getString(R.string.night_heavy_drizzle);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 321:
                        icon = getActivity().getString(R.string.night_heavy_drizzle);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 600:
                        icon = getActivity().getString(R.string.night_snowy);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 601:
                        icon = getActivity().getString(R.string.night_snowy);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 602:
                        icon = getActivity().getString(R.string.snow);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 611:
                        icon = getActivity().getString(R.string.sleet);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 612:
                        icon = getActivity().getString(R.string.night_snowy);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 903:
                    case 615:
                        icon = getActivity().getString(R.string.night_snowy);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 616:
                        icon = getActivity().getString(R.string.night_snowy);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 620:
                        icon = getActivity().getString(R.string.night_snowy);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 621:
                        icon = getActivity().getString(R.string.night_snowy);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 622:
                        icon = getActivity().getString(R.string.night_snowy);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 701:
                    case 702:
                    case 721:
                        icon = getActivity().getString(R.string.smoke);
                        color = ContextCompat.getColor(getContext() , R.color.gray);
                        break;
                    case 751:
                    case 761:
                    case 731:
                        icon = getActivity().getString(R.string.dust);
                        color = ContextCompat.getColor(getContext() , R.color.yellow);
                        break;
                    case 741:
                        icon = getActivity().getString(R.string.fog);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 762:
                        icon = getActivity().getString(R.string.volcano);
                        color = ContextCompat.getColor(getContext() , R.color.red);
                        break;
                    case 771:
                    case 900:
                    case 781:
                        icon = getActivity().getString(R.string.tornado);
                        color = ContextCompat.getColor(getContext() , R.color.gray);
                        break;
                    case 904:
                        icon = getActivity().getString(R.string.night_clear);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 800:
                        icon = getActivity().getString(R.string.night_clear);
                        color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                        break;
                    case 801:
                        icon = getActivity().getString(R.string.night_cloudy);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 802:
                        icon = getActivity().getString(R.string.night_cloudy);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 803:
                        icon = getActivity().getString(R.string.night_cloudy);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 804:
                        icon = getActivity().getString(R.string.night_cloudy);
                        color = ContextCompat.getColor(getContext() , R.color.blue);
                        break;
                    case 901:
                        icon = getActivity().getString(R.string.storm_showers);
                        color = ContextCompat.getColor(getContext() , R.color.gray);
                        break;
                    case 902:
                        icon = getActivity().getString(R.string.hurricane);
                        color = ContextCompat.getColor(getContext() , R.color.gray);
                        break;
                }
        }
        else {
            switch(id) {
                case 501 : icon = getActivity().getString(R.string.weather_drizzle);
                    color = ContextCompat.getColor(getContext() , R.color.blue);
                    break;
                case 500 : icon = getActivity().getString(R.string.weather_drizzle);
                    color = ContextCompat.getColor(getContext() , R.color.blue);
                    break;
                case 502 : icon = getActivity().getString(R.string.weather_rainy);
                    color = ContextCompat.getColor(getContext() , R.color.blue);
                    break;
                case 503 : icon = getActivity().getString(R.string.weather_rainy);
                    color = ContextCompat.getColor(getContext() , R.color.blue);
                    break;
                case 504 : icon = getActivity().getString(R.string.weather_rainy);
                    color = ContextCompat.getColor(getContext() , R.color.blue);
                    break;
                case 511 : icon = getActivity().getString(R.string.weather_rain_wind);
                    color = ContextCompat.getColor(getContext() , R.color.blue);
                    break;
                case 520 : icon = getActivity().getString(R.string.weather_shower_rain);
                    color = ContextCompat.getColor(getContext() , R.color.blue);
                    break;
                case 521 : icon = getActivity().getString(R.string.weather_drizzle);
                    color = ContextCompat.getColor(getContext() , R.color.blue);
                    break;
                case 522 : icon = getActivity().getString(R.string.weather_thunder);
                    color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                    break;
                case 531 : icon = getActivity().getString(R.string.weather_thunder);
                    color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                    break;
                case 200 : icon = getActivity().getString(R.string.weather_thunder);
                    color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                    break;
                case 201 : icon = getActivity().getString(R.string.weather_thunder);
                    color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                    break;
                case 202 : icon = getActivity().getString(R.string.weather_thunder);
                    color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                    break;
                case 210 : icon = getActivity().getString(R.string.weather_thunder);
                    color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                    break;
                case 211 : icon = getActivity().getString(R.string.weather_thunder);
                    color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                    break;
                case 212 : icon = getActivity().getString(R.string.weather_thunder);
                    color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                    break;
                case 221 : icon = getActivity().getString(R.string.weather_thunder);
                    color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                    break;
                case 230 : icon = getActivity().getString(R.string.weather_thunder);
                    color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                    break;
                case 231 : icon = getActivity().getString(R.string.weather_thunder);
                    color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                    break;
                case 232 : icon = getActivity().getString(R.string.weather_thunder);
                    color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                    break;
                case 300 : icon = getActivity().getString(R.string.weather_shower_rain);
                    color = ContextCompat.getColor(getContext() , R.color.blue);
                    break;
                case 301 : icon = getActivity().getString(R.string.weather_shower_rain);
                    color = ContextCompat.getColor(getContext() , R.color.blue);
                    break;
                case 302 : icon = getActivity().getString(R.string.weather_heavy_drizzle);
                    color = ContextCompat.getColor(getContext() , R.color.blue);
                    break;
                case 310 : icon = getActivity().getString(R.string.weather_shower_rain);
                    color = ContextCompat.getColor(getContext() , R.color.blue);
                    break;
                case 311 : icon = getActivity().getString(R.string.weather_shower_rain);
                    color = ContextCompat.getColor(getContext() , R.color.blue);
                    break;
                case 312 : icon = getActivity().getString(R.string.weather_heavy_drizzle);
                    color = ContextCompat.getColor(getContext() , R.color.blue);
                    break;
                case 313 : icon = getActivity().getString(R.string.weather_rain_drizzle);
                    color = ContextCompat.getColor(getContext() , R.color.blue);
                    break;
                case 314 : icon = getActivity().getString(R.string.weather_heavy_drizzle);
                    color = ContextCompat.getColor(getContext() , R.color.blue);
                    break;
                case 321 : icon = getActivity().getString(R.string.weather_heavy_drizzle);
                    color = ContextCompat.getColor(getContext() , R.color.blue);
                    break;
                case 600 : icon = getActivity().getString(R.string.weather_snowy);
                    color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                    break;
                case 601 : icon = getActivity().getString(R.string.weather_snowy);
                    color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                    break;
                case 602 : icon = getActivity().getString(R.string.weather_heavy_snow);
                    color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                    break;
                case 611 : icon = getActivity().getString(R.string.weather_sleet);
                    color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                    break;
                case 612 : icon = getActivity().getString(R.string.weather_heavy_snow);
                    color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                    break;
                case 903 :
                case 615 : icon = getActivity().getString(R.string.weather_snowy);
                    color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                    break;
                case 616 : icon = getActivity().getString(R.string.weather_snowy);
                    color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                    break;
                case 620 : icon = getActivity().getString(R.string.weather_snowy);
                    color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                    break;
                case 621 : icon = getActivity().getString(R.string.weather_snowy);
                    color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                    break;
                case 622 : icon = getActivity().getString(R.string.weather_snowy);
                    color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                    break;
                case 701 :
                case 702 :
                case 721 : icon = getActivity().getString(R.string.weather_smoke);
                    color = ContextCompat.getColor(getContext() , R.color.gray);
                    break;
                case 751 :
                case 761 :
                case 731 : icon = getActivity().getString(R.string.weather_dust);
                    color = ContextCompat.getColor(getContext() , R.color.yellow);
                    break;
                case 741 : icon = getActivity().getString(R.string.weather_foggy);
                    color = ContextCompat.getColor(getContext() , R.color.md_white_1000);
                    break;
                case 762 : icon = getActivity().getString(R.string.weather_volcano);
                    color = ContextCompat.getColor(getContext() , R.color.red);
                    break;
                case 771 :
                case 900 :
                case 781 : icon = getActivity().getString(R.string.weather_tornado);
                    color = ContextCompat.getColor(getContext() , R.color.gray);
                    break;
                case 904 : icon = getActivity().getString(R.string.weather_sunny);
                    color = ContextCompat.getColor(getContext() , R.color.blue);
                    break;
                case 800 : icon = getActivity().getString(R.string.weather_sunny);
                    color = ContextCompat.getColor(getContext() , R.color.yellow);
                    break;
                case 801 : icon = getActivity().getString(R.string.weather_cloudy);
                    color = ContextCompat.getColor(getContext() , R.color.blue);
                    break;
                case 802 : icon = getActivity().getString(R.string.weather_cloudy);
                    color = ContextCompat.getColor(getContext() , R.color.blue);
                    break;
                case 803 : icon = getActivity().getString(R.string.weather_cloudy);
                    color = ContextCompat.getColor(getContext() , R.color.blue);
                    break;
                case 804 : icon = getActivity().getString(R.string.weather_cloudy);
                    color = ContextCompat.getColor(getContext() , R.color.blue);
                    break;
                case 901 : icon = getActivity().getString(R.string.weather_storm);
                    color = ContextCompat.getColor(getContext() , R.color.gray);
                    break;
                case 902 : icon = getActivity().getString(R.string.weather_hurricane);
                    color = ContextCompat.getColor(getContext() , R.color.gray);
                    break;
            }
        }
        weatherIcon[i].setText(icon);
        weatherIcon[i].setTextColor(color);
    }

    private boolean checkDay() {
        Calendar c = Calendar.getInstance();
        int hours = c.get(Calendar.HOUR_OF_DAY);

        return !(hours >= 18 || hours <= 6);
    }

    private void renderWeather(Info jsonObj){
        try {
            json0 = jsonObj.day;
            json1 = jsonObj.fort;
            tc = json0.getMain().getTemp();
            preferences.setLatitude((float) json1.getCity().getCoord().getLatitude());
            preferences.setLongitude((float) json1.getCity().getCoord().getLongitude());
            preferences.setCity(json1.getCity().getName());
            int a = (int) Math.round(json0.getMain().getTemp());
            final String city = json1.getCity().getName().toUpperCase(Locale.US) +
                    ", " +
                    json1.getCity().getCountry();
            cityField.setText(city);
            cityField.setOnClickListener(new View.OnClickListener()
            {
                public void onClick(View v) {
                    Snackbar.make(v, city, Snackbar.LENGTH_SHORT)
                            .show();
                }
            });
            Log.i("Location" , "Location Received");
            List<WeatherFort.WeatherList> details = json1.getList();
            for (int i = 0; i < 10; ++i)
            {
                details.set(i , json1.getList().get(i));
            }
            for (int i = 0; i < 10; ++i)
            {
                final WeatherFort.WeatherList J = details.get(i);
                long date1 = J.getDt();
                Date expiry = new Date(date1 * 1000);
                String date = new SimpleDateFormat("EE, dd" , Locale.US).format(expiry);
                SpannableString ss1 = new SpannableString(date + "\n"
                        + J.getTemp().getMax() + "°" + "      "
                        + J.getTemp().getMin() + "°" + "\n");

                ss1.setSpan(new RelativeSizeSpan(1.1f) , 0 , 7 , 0); // set size

                if(J.getTemp().getMax() <= -5){
                    ss1.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getContext() , R.color.darkBlue)) , 8 , 12 , 0); // set size
                }else if(J.getTemp().getMax() > -5 && J.getTemp().getMax() <= 5){
                    ss1.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getContext() , R.color.blue)) , 8 , 12 , 0); // set size
                }else if(J.getTemp().getMax() > 5 && J.getTemp().getMax() <= 15){
                    ss1.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getContext() , R.color.lightYellow)) , 8 , 12 , 0); // set size
                }else if(J.getTemp().getMax() > 15 && J.getTemp().getMax() <= 20){
                    ss1.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getContext() , R.color.yellow)) , 8 , 12 , 0); // set size
                }else if(J.getTemp().getMax() > 20 && J.getTemp().getMax() <= 30){
                    ss1.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getContext() , R.color.orange)) , 8 , 12 , 0); // set size
                }else if(J.getTemp().getMax() > 30){
                    ss1.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getContext() , R.color.red)) , 8 , 12 , 0); // set size
                }

                if(J.getTemp().getMin() <= -5){
                    ss1.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getContext() , R.color.darkBlue)) , 16 , 20 , 0); // set size
                }else if(J.getTemp().getMin() > -5 && J.getTemp().getMin() <= 5){
                    ss1.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getContext() , R.color.blue)) , 16 , 20 , 0); // set size
                }else if(J.getTemp().getMin() > 5 && J.getTemp().getMin() <= 15){
                    ss1.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getContext() , R.color.lightYellow)) , 16 , 20 , 0); // set size
                }else if(J.getTemp().getMin() > 15 && J.getTemp().getMin() <= 20){
                    ss1.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getContext() , R.color.yellow)) , 16 , 20 , 0); // set size
                }else if(J.getTemp().getMin() > 20 && J.getTemp().getMin() <= 30){
                    ss1.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getContext() , R.color.orange)) , 16 , 20 , 0); // set size
                }else if(J.getTemp().getMin() > 30){
                    ss1.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getContext() , R.color.red)) , 16 , 20 , 0); // set size
                }


                ss1.setSpan(new RelativeSizeSpan(1.4f) , 8 , 12 , 0);
                detailsField[i].setText(ss1);
                setWeatherIcon(J.getWeather().get(0).getId() , i);
                detailsField[i].setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        bottomSheetDialogFragment = newInstance(J);
                        bottomSheetDialogFragment.show(getActivity().getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                    }
                });
                weatherIcon[i].setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v)
                    {
                        bottomSheetDialogFragment = newInstance(J);
                        bottomSheetDialogFragment.show(getActivity().getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                    }
                });
            }
            final String d1 = new java.text.SimpleDateFormat("hh:mm a" , Locale.US).format(new Date(json0.getSys().getSunrise() * 1000));
            final String d2 = new java.text.SimpleDateFormat("hh:mm a" , Locale.US).format(new Date(json0.getSys().getSunset() * 1000));
            sunriseView.setText(d1);
            sunsetView.setText(d2);
            DateFormat df = DateFormat.getDateTimeInstance();
            String updatedOn = "Last update: " + df.format(new Date(json0.getDt() * 1000));
            updatedField.setText(updatedOn);
            final String humidity = String.format(getString(R.string.humidity_) , json0.getMain().getHumidity());
            humidityView.setText(humidity);
            humidityIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Snackbar.make(rootView , String.format(Locale.ENGLISH , getString(R.string.humidity) , json0.getMain().getHumidity()) + " %" , Snackbar.LENGTH_SHORT).show();
                }
            });
            humidityView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Snackbar.make(rootView , String.format(Locale.ENGLISH , getString(R.string.humidity) , json0.getMain().getHumidity()) + " %" , Snackbar.LENGTH_SHORT).show();
                }
            });
            final String wind = String.format(Locale.ENGLISH , getString(R.string.wind) , json0.getWind().getSpeed() , preferences.getUnits().equals("metric") ? getString(R.string.mps) : getString(R.string.mph));
            windView.setText(wind);
            windIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Snackbar.make(rootView , String.format(Locale.ENGLISH , getString(R.string.wind_speed) , json0.getWind().getSpeed() , preferences.getUnits().equals("metric") ? getString(R.string.mps) : getString(R.string.mph)) , Snackbar.LENGTH_SHORT).show();
                }
            });
            windView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Snackbar.make(rootView , String.format(Locale.ENGLISH , getString(R.string.wind_speed) , json0.getWind().getSpeed() , preferences.getUnits().equals("metric") ? getString(R.string.mps) : getString(R.string.mph)) , Snackbar.LENGTH_SHORT).show();
                }
            });
            humidityIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Snackbar.make(rootView , String.format(Locale.ENGLISH , getString(R.string.humidity) , json0.getMain().getHumidity()) , Snackbar.LENGTH_SHORT).show();
                }
            });
            humidityView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Snackbar.make(rootView , String.format(Locale.ENGLISH , getString(R.string.humidity) , json0.getMain().getHumidity()) , Snackbar.LENGTH_SHORT).show();
                }
            });
            sunriseIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Snackbar.make(rootView , String.format(Locale.ENGLISH , getString(R.string.sunrise) , d1) , Snackbar.LENGTH_SHORT).show();
                }
            });
            sunriseView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Snackbar.make(rootView , String.format(Locale.ENGLISH , getString(R.string.sunrise) , d1) , Snackbar.LENGTH_SHORT).show();
                }
            });
            sunsetIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Snackbar.make(rootView , String.format(Locale.ENGLISH , getString(R.string.sunset) , d2) , Snackbar.LENGTH_SHORT).show();
                }
            });
            sunsetView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Snackbar.make(rootView , String.format(Locale.ENGLISH , getString(R.string.sunset) , d2) , Snackbar.LENGTH_SHORT).show();
                }
            });
            setWeatherIcon(json0.getWeather().get(0).getId() , 10);
            weatherIcon[10].setOnClickListener(new View.OnClickListener()
            {
                public void onClick (View v)
                {
                    try {
                        String rs = json0.getWeather().get(0).getDescription();
                        String[] strArray = rs.split(" ");
                        StringBuilder builder = new StringBuilder();
                        for (String s : strArray) {
                            String cap = s.substring(0, 1).toUpperCase() + s.substring(1);
                            builder.append(cap.concat(" "));
                        }
                        Snackbar.make(v , String.format(getString(R.string.hey_there_condition) , builder.toString()), Snackbar.LENGTH_SHORT)
                                .show();
                    }
                    catch (Exception e) {
                        Log.e("Error", "Main Weather Icon OnClick Details could not be loaded");
                    }
                }
            });
            String r1 = Integer.toString(a) + "°";
            if(a <= -5){
                button.setTextColor(ContextCompat.getColor(getContext() , R.color.darkBlue));
            }else if(a > -5 && a <= 5){
                button.setTextColor(ContextCompat.getColor(getContext() , R.color.blue));
            }else if(a > 5 && a <= 15){
                button.setTextColor(ContextCompat.getColor(getContext() , R.color.lightYellow));
            }else if(a > 15 && a <= 20){
                button.setTextColor(ContextCompat.getColor(getContext() , R.color.yellow));
            }else if(a > 20 && a <= 30){
                button.setTextColor(ContextCompat.getColor(getContext() , R.color.orange));
            }else if(a > 30){
                button.setTextColor(ContextCompat.getColor(getContext() , R.color.red));
            }
            button.setText(r1);
            int deg = json0.getWind().getDirection();
            setDeg(deg);
        }catch(Exception e){
            Log.e(WeatherFragment.class.getSimpleName() , "One or more fields not found in the JSON data");
        }
    }

    private void setDeg(int deg) {
        int index = Math.abs(Math.round(deg % 360) / 45);
        switch (index) {
            case 0 : directionView.setText(getActivity().getString(R.string.top));
                directionView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Snackbar.make(view , getString(R.string.north) , Snackbar.LENGTH_SHORT).show();
                    }
                });
                break;
            case 1 : directionView.setText(getActivity().getString(R.string.top_right));
                directionView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Snackbar.make(view , getString(R.string.north_east) , Snackbar.LENGTH_SHORT).show();
                    }
                });
                break;
            case 2 : directionView.setText(getActivity().getString(R.string.right));
                directionView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Snackbar.make(view , getString(R.string.east) , Snackbar.LENGTH_SHORT).show();
                    }
                });
                break;
            case 3 : directionView.setText(getActivity().getString(R.string.bottom_right));
                directionView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Snackbar.make(view , getString(R.string.south_east) , Snackbar.LENGTH_SHORT).show();
                    }
                });
                break;
            case 4 : directionView.setText(getActivity().getString(R.string.down));
                directionView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Snackbar.make(view , getString(R.string.south) , Snackbar.LENGTH_SHORT).show();
                    }
                });
                break;
            case 5 : directionView.setText(getActivity().getString(R.string.bottom_left));
                directionView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Snackbar.make(view , getString(R.string.south_west) , Snackbar.LENGTH_SHORT).show();
                    }
                });
                break;
            case 6 : directionView.setText(getActivity().getString(R.string.left));
                directionView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Snackbar.make(view , getString(R.string.west) , Snackbar.LENGTH_SHORT).show();
                    }
                });
                break;
            case 7 : directionView.setText(getActivity().getString(R.string.top_left));
                directionView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Snackbar.make(view , getString(R.string.north_west) , Snackbar.LENGTH_SHORT).show();
                    }
                });
                break;
        }
    }

    public static CustomBottomSheetDialogFragment newInstance(WeatherFort.WeatherList describable) {
        CustomBottomSheetDialogFragment fragment = new CustomBottomSheetDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(Constants.DESCRIBABLE_KEY, describable);
        fragment.setArguments(bundle);

        return fragment;
    }

    private void showMenuInputDialog() {
        new MaterialDialog.Builder(getContext())
                .title(getString(R.string.change_city))
                .content(getString(R.string.enter_zip_code))
                .negativeText(getString(R.string.cancel))
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog , @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .input(null, null, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, @NonNull CharSequence input) {
                        changeCity(input.toString());
                    }
                }).show();
    }
}
