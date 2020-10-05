package com.example.listify;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.amplifyframework.auth.AuthException;
import com.example.listify.data.Item;
import com.example.listify.data.List;
import com.example.listify.data.ListEntry;
import com.google.android.material.navigation.NavigationView;
import org.json.JSONException;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private AppBarConfiguration mAppBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //------------------------------Auth Testing---------------------------------------------//

        boolean testAuth = false;

        if (testAuth) {

            AuthManager authManager = new AuthManager();
            try {
                authManager.signIn("merzn@purdue.edu", "Password123");
                Log.i("Authentication", authManager.getAuthSession().toString());
                Log.i("Token", authManager.getAuthSession().getUserPoolTokens().getValue().getIdToken());
            } catch (AuthException e) {
                Log.i("Authentication", "Login failed. User probably needs to register. Exact error: " + e.getMessage());
                try {
                    authManager.startSignUp("merzn@purdue.edu", "Password123");
                    authManager.confirmSignUp("######");
                } catch (AuthException signUpError) {
                    Log.e("Authentication", "SignUp error: " + signUpError.getMessage());
                }
            }
        }


        //------------------------------------------------------------------------------------------//


        boolean testAPI = true;
        //----------------------------------API Testing---------------------------------------------//

        if (testAPI) {
            AuthManager authManager = new AuthManager();
            try {
                authManager.signIn("merzn@purdue.edu", "Password123");
            } catch (AuthException e) {
                e.printStackTrace();
            }
            Properties configs = new Properties();
            try {
                configs = AuthManager.loadProperties(this, "android.resource://" + getPackageName() + "/raw/auths.json");
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            Requestor requestor = new Requestor(authManager, configs.getProperty("apiKey"));

            authManager.deleteUser(requestor);

            //The name is the only part of this that is used, the rest is generated by the Lambda.
            List testList = new List(-1, "New List", "user filled by lambda", Instant.now().toEpochMilli());
            //Everything except addedDate is used for ItemEntry
            ListEntry entry = new ListEntry(1, 1, new Random().nextInt(), Instant.now().atZone(ZoneOffset.UTC).toLocalDateTime(),false);
            SynchronousReceiver<Integer> idReceiver = new SynchronousReceiver<>();
            try {
                requestor.postObject(testList, idReceiver, idReceiver);
                System.out.println(idReceiver.await());
                requestor.postObject(entry);
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }

            SynchronousReceiver<Item> itemReceiver = new SynchronousReceiver<>();
            requestor.getObject("1", Item.class, itemReceiver, itemReceiver);
            SynchronousReceiver<List> listReceiver = new SynchronousReceiver<>();
            requestor.getObject("39", List.class, listReceiver, listReceiver);
            SynchronousReceiver<Integer[]> listIdsReceiver = new SynchronousReceiver<>();
            requestor.getListOfIds(List.class, listIdsReceiver, listIdsReceiver);
            try {
                System.out.println(itemReceiver.await());
                System.out.println(listReceiver.await());
                System.out.println(Arrays.toString(listIdsReceiver.await()));
            } catch (IOException receiverError) {
                receiverError.printStackTrace();
            }
        }
        //------------------------------------------------------------------------------------------//


        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        /*FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });*/
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        // Handle search button click
        ImageButton searchButton = (ImageButton) findViewById(R.id.searchButton);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SearchResults.class);
                // Send user to SearchResults activity
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_left, R.anim.exit_from_left);

            }
        });
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }*/

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }
}