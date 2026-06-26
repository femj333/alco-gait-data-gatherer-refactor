package edu.wpi.alcogaitdatagatherer.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.box.androidsdk.content.BoxApiFile;
import com.box.androidsdk.content.BoxApiFolder;
import com.box.androidsdk.content.BoxConfig;
import com.box.androidsdk.content.auth.BoxAuthentication;
import com.box.androidsdk.content.models.BoxSession;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import edu.wpi.alcogaitdatagatherer.R;
import edu.wpi.alcogaitdatagatherer.ui.adapters.SurveyListAdapter;

public class HomeActivity extends AppCompatActivity implements BoxAuthentication.AuthListener{

    private SurveyListAdapter surveyListAdapter;
    private ListView surveyListView;
    private LinkedList<File> surveyFiles;
    public static final String FILE_SHOULD_START_WITH = "ID_";
    private static final int READ_WRITE_PERMISSION_CODE = 1000;

    private static final String CLIENT_ID = "jqkqfexx2sdtk8fd145dwfexr851drh3";
    private static final String CLIENT_SECRET = "NjaaG4NrOjCFFpvRn2gSFr5YtEuiReCl";
    private static final String REDIRECT_URI = "https://localhost";

    private BoxSession boxSession;
    private BoxSession oldBoxSession;

    private BoxApiFolder mFolderApi;
    private BoxApiFile mFileApi;

    private FloatingActionButton uab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        surveyListView = findViewById(R.id.surveyList);

        surveyFiles = new LinkedList<>();

        readFiles();

        surveyListAdapter = new SurveyListAdapter(this, surveyFiles, surveyListView);

        surveyListView.setAdapter(surveyListAdapter);

        requestPermissions();

        if (isBoxPreferenceEnabled()) {
            configureBoxClient();
            initializeBoxSession();
        }

        FloatingActionButton aab = findViewById(R.id.aab);
        aab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomeActivity.this, SurveyFormActivity.class);
                startActivity(intent);
            }
        });

        uab = findViewById(R.id.uab);
    }

    public void requestPermissions(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        READ_WRITE_PERMISSION_CODE);
            }
        }

    }

    public void readFiles(){
        File alcoGaitDirectory = new File(getExternalFilesDir(null), "AlcoGaitDataGatherer");
        if (!alcoGaitDirectory.exists()) {
            alcoGaitDirectory.mkdirs();
        }

        File[] allFilesFromDir = alcoGaitDirectory.listFiles();

        surveyFiles.clear();

        if (allFilesFromDir != null) {
            for(File file: allFilesFromDir){
                String fileName = file.getName();
                if (fileName.length() == 6 && file.isDirectory() && fileName.startsWith(FILE_SHOULD_START_WITH)) {
                    surveyFiles.add(file);
                }
            }
        }

        Collections.sort(surveyFiles, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return Long.compare(f2.lastModified(), f1.lastModified());
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case READ_WRITE_PERMISSION_CODE : {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    readFiles();
                    surveyListAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void configureBoxClient(){
        BoxConfig.CLIENT_ID = CLIENT_ID;
        BoxConfig.CLIENT_SECRET = CLIENT_SECRET;
        BoxConfig.REDIRECT_URL = REDIRECT_URI;
    }

    private void initializeBoxSession(){
        boxSession = new BoxSession(this);
        boxSession.setSessionAuthListener(this);
        boxSession.authenticate(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRefreshed(BoxAuthentication.BoxAuthenticationInfo info) {
    }

    @Override
    public void onAuthCreated(BoxAuthentication.BoxAuthenticationInfo info) {
        mFolderApi = new BoxApiFolder(boxSession);
        mFileApi = new BoxApiFile(boxSession);
    }

    @Override
    public void onAuthFailure(BoxAuthentication.BoxAuthenticationInfo info, Exception ex) {
        if (ex == null && info == null && oldBoxSession != null) {
            boxSession = oldBoxSession;
            boxSession.setSessionAuthListener(this);
            oldBoxSession = null;
            onAuthCreated(boxSession.getAuthInfo());
        }
    }

    @Override
    public void onLoggedOut(BoxAuthentication.BoxAuthenticationInfo info, Exception ex) {
        initializeBoxSession();
    }

    private void showToast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(HomeActivity.this, text, Toast.LENGTH_LONG).show();
            }
        });
    }

    public boolean isBoxPreferenceEnabled() {
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        return SP.getBoolean(getString(R.string.box_integration_preference), false);
    }

    @Override
    public void onResume(){
        super.onResume();
        readFiles();
        if (isBoxPreferenceEnabled()) {
            uab.setVisibility(View.VISIBLE);
            uab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    surveyListAdapter.syncWithBox(mFileApi);
                }
            });
        } else {
            uab.setVisibility(View.GONE);
        }
        surveyListAdapter.notifyDataSetChanged();
    }

}
