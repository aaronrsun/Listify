package com.example.listify;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import com.amplifyframework.auth.AuthException;
import com.bumptech.glide.Glide;
import com.example.listify.data.List;
import com.example.listify.data.ListEntry;
import com.example.listify.model.Product;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Properties;

import static com.example.listify.MainActivity.am;

public class ItemDetails extends AppCompatActivity implements ListPickerDialogFragment.OnListPickListener, CreateListAddDialogFragment.OnNewListAddListener {
    private Product curProduct;
    private LinearLayout linAddItem;
    private LinearLayout linCreateList;
    private TextView tvCreateNew;
    private TextView tvAddItem;
    private TextView tvItemName;
    private TextView tvStoreName;
    private ImageView itemImage;
    private TextView tvItemPrice;
    private TextView tvItemDesc;
    private ImageButton backToSearchbutton;

    ArrayList<List> shoppingLists = new ArrayList<>();

    private boolean isFABOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_details);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Load Product object from search results activity
        curProduct = (Product) getIntent().getSerializableExtra("SelectedProduct");
        // Set up floating action buttons
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        linAddItem = (LinearLayout) findViewById(R.id.lin_add_item);
        linCreateList = (LinearLayout) findViewById(R.id.lin_create_list);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isFABOpen) {
                    showFABMenu();
                } else {
                    closeFABMenu();
                }
            }
        });

        linAddItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFABMenu();
                // Create and show a loading dialog
                Dialog loadingDialog = new Dialog(ItemDetails.this);
                loadingDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                loadingDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
                // layout to display
                loadingDialog.setContentView(R.layout.dialog_loading);
                // set color transpartent
                loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                loadingDialog.setCancelable(false);
                loadingDialog.setCanceledOnTouchOutside(false);
                loadingDialog.show();

                Properties configs = new Properties();
                try {
                    configs = AuthManager.loadProperties(ItemDetails.this, "android.resource://" + getPackageName() + "/raw/auths.json");
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }

                Requestor requestor = new Requestor(am, configs.getProperty("apiKey"));
                SynchronousReceiver<Integer[]> listIdsReceiver = new SynchronousReceiver<>();
                SynchronousReceiver<List> listReceiver = new SynchronousReceiver<>();
                requestor.getListOfIds(List.class, listIdsReceiver, listIdsReceiver);

                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Integer[] listIds = listIdsReceiver.await();
                            for (int i = 0; i < listIds.length; i++) {
                                requestor.getObject(Integer.toString(listIds[i]), List.class, listReceiver, listReceiver);
                                shoppingLists.add(listReceiver.await());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        loadingDialog.cancel();
                        ListPickerDialogFragment listPickerDialog = new ListPickerDialogFragment(shoppingLists);
                        listPickerDialog.show(getSupportFragmentManager(), "User Lists");
                    }
                });
                t.start();
            }
        });

        linCreateList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFABMenu();

                CreateListAddDialogFragment createListAddDialogFragment = new CreateListAddDialogFragment();
                createListAddDialogFragment.show(getSupportFragmentManager(), "Create New List");
            }
        });

        // Set data
        tvItemName = (TextView) findViewById(R.id.item_name);
        tvItemName.setText(curProduct.getItemName());

        itemImage = (ImageView) findViewById(R.id.item_image);
        Glide.with(this).load(curProduct.getImageUrl()).into(itemImage);

        tvStoreName = (TextView) findViewById(R.id.store_name);
        tvStoreName.setText(curProduct.getChainName());

        tvItemPrice = (TextView) findViewById(R.id.item_price);
        tvItemPrice.setText(String.format("$%.2f", curProduct.getPrice()));

//        tvItemDesc = (TextView) findViewById(R.id.item_desc);
//        tvItemDesc.setText(curProduct.getDescription());

        tvCreateNew = (TextView) findViewById(R.id.create_new_list);
        tvCreateNew.setVisibility(View.INVISIBLE);

        tvAddItem = (TextView) findViewById(R.id.add_item_to_list);
        tvAddItem.setVisibility(View.INVISIBLE);

        backToSearchbutton = (ImageButton) findViewById(R.id.back_to_search_results_button);
        backToSearchbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void showFABMenu(){
        isFABOpen=true;
        linAddItem.animate().translationY(-getResources().getDimension(R.dimen.standard_55));
        linCreateList.animate().translationY(-getResources().getDimension(R.dimen.standard_105));
        tvAddItem.setVisibility(View.VISIBLE);
        tvCreateNew.setVisibility(View.VISIBLE);
    }

    private void closeFABMenu(){
        isFABOpen=false;
        linAddItem.animate().translationY(0);
        linCreateList.animate().translationY(0);
        tvAddItem.setVisibility(View.INVISIBLE);
        tvCreateNew.setVisibility(View.INVISIBLE);
    }


    // Add the viewed item to the selected list
    @Override
    public void sendListSelection(int selectedListIndex, int quantity) {

        Properties configs = new Properties();
        try {
            configs = AuthManager.loadProperties(this, "android.resource://" + getPackageName() + "/raw/auths.json");
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        Requestor requestor = new Requestor(am, configs.getProperty("apiKey"));
        SynchronousReceiver<Integer> idReceiver = new SynchronousReceiver<>();


        try {
            ListEntry entry = new ListEntry(shoppingLists.get(selectedListIndex).getItemID(), curProduct.getItemId(), quantity, Instant.now().toEpochMilli(),false);
            requestor.postObject(entry);
            Toast.makeText(this, String.format("%d of Item added to %s", quantity, shoppingLists.get(selectedListIndex).getName()), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "An error occurred", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    // Create a new list and add the item to it
    @Override
    public void sendNewListName(String name, int quantity) {
        // Create and show a loading dialog
        Dialog loadingDialog = new Dialog(this);
        loadingDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        loadingDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // layout to display
        loadingDialog.setContentView(R.layout.dialog_loading);
        // set color transpartent
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        loadingDialog.setCancelable(false);
        loadingDialog.setCanceledOnTouchOutside(false);
        loadingDialog.show();

        Properties configs = new Properties();
        try {
            configs = AuthManager.loadProperties(this, "android.resource://" + getPackageName() + "/raw/auths.json");
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        Requestor requestor = new Requestor(am, configs.getProperty("apiKey"));
        SynchronousReceiver<Integer> idReceiver = new SynchronousReceiver<>();

        com.example.listify.data.List newList = new List(-1, name, "user filled by lambda", Instant.now().toEpochMilli());

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    requestor.postObject(newList, idReceiver, idReceiver);
                    int newListId = idReceiver.await();
                    ListEntry entry = new ListEntry(newListId, curProduct.getItemId(), quantity, Instant.now().toEpochMilli(),false);
                    requestor.postObject(entry);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ItemDetails.this, String.format("%s created and item added", name), Toast.LENGTH_LONG).show();
                            loadingDialog.cancel();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ItemDetails.this, "An error occurred", Toast.LENGTH_LONG).show();
                            loadingDialog.cancel();
                            e.printStackTrace();
                        }
                    });
                }
            }
        });
        t.start();
    }
}