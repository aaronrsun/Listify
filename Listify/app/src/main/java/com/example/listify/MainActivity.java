package com.example.listify;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.amplifyframework.auth.AuthException;
import com.example.listify.data.Item;
import com.example.listify.data.ItemSearch;
import com.example.listify.data.List;
import com.example.listify.data.ListEntry;
import com.google.android.material.navigation.NavigationView;
import org.json.JSONException;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements CreateListDialogFragment.OnNewListListener {
    private AppBarConfiguration mAppBarConfiguration;

    public static AuthManager am = new AuthManager();

    boolean showSplash = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(showSplash) {
            showSplash = false;

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(MainActivity.this, SplashActivity.class);
                    startActivity(intent);
                    finish();
                }
            }, 1);
        }


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
        //NOTE: deleteUser is slightly unusual in that it requires a Requestor. See below for building one
        //authManager.deleteUser(requestor);

        //------------------------------------------------------------------------------------------//


        boolean testAPI = false;
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


            //The name is the only part of this that is used, the rest is generated by the Lambda.
            List testList = new List(-1, "New List", "user filled by lambda", Instant.now().toEpochMilli());
            //Everything except addedDate is used for ItemEntry
            ListEntry entry = new ListEntry(1, 4, Math.abs(new Random().nextInt()), Instant.now().toEpochMilli(),false);
          
            SynchronousReceiver<Integer> idReceiver = new SynchronousReceiver<>();
            try {
                requestor.postObject(testList, idReceiver, idReceiver);
                System.out.println(idReceiver.await());
                requestor.postObject(entry);
            } 
            catch (Exception e) {
                Log.i("Authentication", e.toString());
                e.printStackTrace();
            }

            SynchronousReceiver<Item> itemReceiver = new SynchronousReceiver<>();
            requestor.getObject("1", Item.class, itemReceiver, itemReceiver);
            SynchronousReceiver<List> listReceiver = new SynchronousReceiver<>();
            requestor.getObject("39", List.class, listReceiver, listReceiver);
            SynchronousReceiver<Integer[]> listIdsReceiver = new SynchronousReceiver<>();
            requestor.getListOfIds(List.class, listIdsReceiver, listIdsReceiver);
            SynchronousReceiver<ItemSearch> itemSearchReceiver = new SynchronousReceiver<>();
            requestor.getObject("r", ItemSearch.class, itemSearchReceiver, itemSearchReceiver);
            try {
                System.out.println(itemReceiver.await());
                System.out.println(listReceiver.await());
                System.out.println(Arrays.toString(listIdsReceiver.await()));
                System.out.println(itemSearchReceiver.await());
            } catch (Exception receiverError) {
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
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow, R.id.nav_lists)
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

    // This function only exists for the create new list option in hamburger menu
    public void onClickCreateList(MenuItem m) {
        m.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                CreateListDialogFragment createListDialogFragment = new CreateListDialogFragment();
                createListDialogFragment.show(getSupportFragmentManager(), "Create New List");
                return false;
            }
        });
    }

    @Override
    public void sendNewListName(String name) {
        Properties configs = new Properties();
        try {
            configs = AuthManager.loadProperties(this, "android.resource://" + getPackageName() + "/raw/auths.json");
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        Requestor requestor = new Requestor(am, configs.getProperty("apiKey"));
        SynchronousReceiver<Integer> idReceiver = new SynchronousReceiver<>();

        List newList = new List(-1, name, "user filled by lambda", Instant.now().toEpochMilli());

        try {
            requestor.postObject(newList, idReceiver, idReceiver);
            System.out.println(idReceiver.await());
            Toast.makeText(this, String.format("%s created", name), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "An error occurred", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
}