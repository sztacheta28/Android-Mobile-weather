package pl.androiddev.weather.activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;

import pl.androiddev.weather.R;
import pl.androiddev.weather.fragment.GraphsFragment;
import pl.androiddev.weather.fragment.MapsFragment;
import pl.androiddev.weather.fragment.WeatherFragment;
import pl.androiddev.weather.model.WeatherFort;
import pl.androiddev.weather.preferences.Prefs;
import pl.androiddev.weather.service.NotificationService;
import pl.androiddev.weather.utils.Constants;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.interfaces.OnCheckedChangeListener;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondarySwitchDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.weather_icons_typeface_library.WeatherIcons;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import shortbread.Shortbread;
import shortbread.Shortcut;

public class WeatherActivity extends AppCompatActivity {
    Prefs preferences;
    WeatherFragment wf;
    GraphsFragment gf;
    MapsFragment mf;
    @BindView(R.id.toolbar) Toolbar toolbar;
    Drawer drawer;
    NotificationManagerCompat mManager;
    Handler handler;

    @Shortcut(id = "home", icon = R.drawable.shortcut_home , shortLabel = "Weather Info", rank = 2)
    public void addWeather() {

    }

    @Shortcut(id = "graphs", icon = R.drawable.shortcut_graph , shortLabel = "Weather Graphs" , rank = 1)
    public void addGraphs() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                drawer.setSelectionAtPosition(2);
                GraphsFragment graphsFragment = newGraphInstance(new ArrayList<>(wf.getDailyJson()));
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment, graphsFragment)
                        .commit();
            }
        } , 750);
    }

    @Shortcut(id = "maps", icon = R.drawable.shortcut_map , shortLabel = "Weather Maps")
    public void addMaps() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                drawer.setSelectionAtPosition(3);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment, mf)
                        .commit();
            }
        } , 750);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("Activity" , WeatherActivity.class.getSimpleName());
        mManager = NotificationManagerCompat.from(this);
        preferences = new Prefs(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        Intent intent = getIntent();
        handler = new Handler();

        wf = new WeatherFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("mode" , intent.getIntExtra(Constants.MODE , 0));
        wf.setArguments(bundle);
        gf = new GraphsFragment();
        mf = new MapsFragment();

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment, wf)
                .commit();
        initDrawer();
    }

    public void createShortcuts() {
        Shortbread.create(this);
    }

    public void initDrawer() {
        final Context context = this;
        final IProfile profile = new ProfileDrawerItem().withName(getString(R.string.app_name))
                .withIcon(R.drawable.ic_launcher);
        AccountHeader headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.header)
                .withTextColor(ContextCompat.getColor(this , R.color.md_white_1000))
                .addProfiles(
                        profile
                )
                .withSelectionListEnabled(false)
                .withProfileImagesClickable(false)
                .build();
        SecondaryDrawerItem item1 = new SecondaryDrawerItem().withIdentifier(1).withName(R.string.drawer_item_home)
                .withIcon(new IconicsDrawable(this)
                        .color(ContextCompat.getColor(this , R.color.colorPrimary))
                        .icon(WeatherIcons.Icon.wic_day_sunny));
        SecondaryDrawerItem item2 = new SecondaryDrawerItem().withIdentifier(2).withName(R.string.drawer_item_graph)
                .withIcon(new IconicsDrawable(this)
                        .color(ContextCompat.getColor(this , R.color.colorPrimary))
                        .icon(GoogleMaterial.Icon.gmd_trending_up));
        SecondaryDrawerItem item3 = new SecondaryDrawerItem().withIdentifier(3).withName(R.string.drawer_item_map)
                .withIcon(new IconicsDrawable(this)
                        .color(ContextCompat.getColor(this , R.color.colorPrimary))
                        .icon(GoogleMaterial.Icon.gmd_map));
        SecondarySwitchDrawerItem item4 = new SecondarySwitchDrawerItem().withIdentifier(4).withName(getString(R.string.drawer_item_fahrenheit))
                .withChecked(preferences.getUnits().equals(Constants.IMPERIAL))
                .withIcon(new IconicsDrawable(this)
                        .color(ContextCompat.getColor(this , R.color.colorPrimary))
                        .icon(WeatherIcons.Icon.wic_fahrenheit))
                .withSelectable(false);
        SecondarySwitchDrawerItem item5 = new SecondarySwitchDrawerItem().withIdentifier(5).withName(getString(R.string.drawer_item_notifications))
                .withChecked(preferences.getNotifs())
                .withIcon(new IconicsDrawable(this)
                        .color(ContextCompat.getColor(this , R.color.colorPrimary))
                        .icon(GoogleMaterial.Icon.gmd_notifications))
                .withSelectable(false);
        SecondaryDrawerItem item6 = new SecondaryDrawerItem().withIdentifier(6).withName(getString(R.string.drawer_item_about))
                .withIcon(new IconicsDrawable(this)
                        .color(ContextCompat.getColor(this , R.color.colorPrimary))
                        .icon(GoogleMaterial.Icon.gmd_create))
                .withSelectable(false);
        item4.withOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(IDrawerItem drawerItem, CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    preferences.setUnits(Constants.IMPERIAL);
                }
                else {
                    preferences.setUnits(Constants.METRIC);
                }
                Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragment);
                if (f instanceof WeatherFragment) {
                    wf = new WeatherFragment();
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment, wf)
                            .commit();
                    drawer.closeDrawer();
                }
            }
        });
        item5.withOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(IDrawerItem drawerItem, CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    preferences.setNotifs(true);
                }
                else {
                    preferences.setNotifs(false);
                    mManager.cancelAll();
                }
                NotificationService.setNotificationServiceAlarm(context , preferences.getNotifs());
            }
        });
        drawer = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withSelectedItem(1)
                .withTranslucentStatusBar(true)
                .withAccountHeader(headerResult)
                .withActionBarDrawerToggleAnimated(true)
                .addDrawerItems(
                        item1,
                        item2,
                        item3,
                        new DividerDrawerItem(),
                        item4,
                        item5,
                        new DividerDrawerItem(),
                        item6
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        // do something with the clicked item :D
                            if (drawerItem != null) {
                                switch((int) drawerItem.getIdentifier()) {
                                    case 1:
                                        Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragment);
                                        if (!(f instanceof WeatherFragment)) {
                                            wf = new WeatherFragment();
                                            getSupportFragmentManager().beginTransaction()
                                                    .replace(R.id.fragment, wf)
                                                    .commit();
                                        }
                                        break;
                                    case 2:
                                        f = getSupportFragmentManager().findFragmentById(R.id.fragment);
                                        if (!(f instanceof GraphsFragment)) {
                                            GraphsFragment graphsFragment = newGraphInstance(new ArrayList<>(wf.getDailyJson()));
                                            getSupportFragmentManager().beginTransaction()
                                                    .replace(R.id.fragment, graphsFragment)
                                                    .commit();
                                        }
                                        break;
                                    case 3:
                                        f = getSupportFragmentManager().findFragmentById(R.id.fragment);
                                        if (!(f instanceof MapsFragment)) {
                                            MapsFragment mapsFragment = new MapsFragment();
                                            getSupportFragmentManager().beginTransaction()
                                                    .replace(R.id.fragment, mapsFragment)
                                                    .commit();
                                        }
                                        break;
                                    case 6:
                                        showAuthor();
                                        break;
                                }
                            }
                        return false;
                    }
                })
                .build();
    }

    private void showAuthor() {
        new MaterialDialog.Builder(this)
                .title(getString(R.string.author_title))
                .content(getString(R.string.author_content))
                .show();
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen()) {
            drawer.closeDrawer();
        }
        else {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    private static final String DESCRIBABLE_KEY = "describable_key";

    public static GraphsFragment newGraphInstance(ArrayList<WeatherFort.WeatherList> describable) {
        GraphsFragment fragment = new GraphsFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(DESCRIBABLE_KEY, describable);
        fragment.setArguments(bundle);

        return fragment;
    }
}
