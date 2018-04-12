package de.openfiresource.falarm.ui;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.listener.multi.CompositeMultiplePermissionsListener;
import com.karumi.dexter.listener.multi.SnackbarOnAnyDeniedMultiplePermissionsListener;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import de.openfiresource.falarm.R;
import de.openfiresource.falarm.dialogs.MainMultiplePermissionsListener;
import de.openfiresource.falarm.ui.settings.RuleListActivity;
import de.openfiresource.falarm.ui.settings.SettingsActivity;
import de.openfiresource.falarm.utils.PlayServiceUtils;

public class MainActivity extends AppCompatActivity implements HasSupportFragmentInjector {

    private static final String TAG = "MainActivity";

    public static final String PREF_SHOW_WELCOME_CARD_VERSION = "showWelcomeCardVersion";

    @Inject
    DispatchingAndroidInjector<Fragment> supportFragmentDispatchingAndroidInjector;

    @Inject
    SharedPreferences sharedPreferences;

    ViewGroup rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));

        rootView = findViewById(android.R.id.content);

        checkPermissions();

        int lastVersion = sharedPreferences.getInt(PREF_SHOW_WELCOME_CARD_VERSION, 0);
        if (lastVersion < getVersionCode()) {
            showWelcomeDialog();
        }
    }

    private void checkPermissions() {
        CompositeMultiplePermissionsListener compositeMultiplePermissionsListener = new CompositeMultiplePermissionsListener(new MainMultiplePermissionsListener(this),
                SnackbarOnAnyDeniedMultiplePermissionsListener.Builder.with(rootView,
                        R.string.permission_rationale_message)
                        .withOpenSettingsButton(R.string.permission_rationale_settings_button_text)
                        .build());

        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).withListener(compositeMultiplePermissionsListener)
                .check();
    }

    private int getVersionCode() {
        PackageManager pm = getBaseContext().getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(getBaseContext().getPackageName(), 0);
            return pi.versionCode;
        } catch (PackageManager.NameNotFoundException ex) {
            Log.e(TAG, "getVersionCode: ", ex);
        }
        return 0;
    }

    @Override
    public void onResume() {
        PlayServiceUtils.checkPlayServices(this);
        super.onResume();
    }

    private void showWelcomeDialog() {
        @StringRes int text = 0;
        int lastVersion = sharedPreferences.getInt(PREF_SHOW_WELCOME_CARD_VERSION, 0);
        if (lastVersion != 0) {
            switch (getVersionCode()) {
                case 3:
                    text = R.string.welcome_card_desc_v3;
                    break;
                case 4:
                    text = R.string.welcome_card_desc_v4;
                    break;
                case 5:
                    text = R.string.welcome_card_desc_v5;
                    break;
                case 6:
                    text = R.string.welcome_card_desc_v6;
                    break;
                case 7:
                    text = R.string.welcome_card_desc_v7;
                    break;
            }
        } else {
            text = R.string.welcome_card_desc;
        }

        if (text != 0) {
            new AlertDialog.Builder(this)
                    .setMessage(text)
                    .setPositiveButton(android.R.string.ok, (dialog1, which) -> sharedPreferences.edit()
                            .putInt(PREF_SHOW_WELCOME_CARD_VERSION, getVersionCode())
                            .apply())
                    .create()
                    .show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        Intent intent;

        switch (id) {
            case R.id.action_rules:
                intent = new Intent(this, RuleListActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_about:
                // todo: Create About section
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public AndroidInjector<Fragment> supportFragmentInjector() {
        return supportFragmentDispatchingAndroidInjector;
    }
}
