package com.hwaya.candytest;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.biometrics.BiometricPrompt;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class MainActivity extends AppCompatActivity implements BillingProcessor.IBillingHandler {

    Spinner spinner;
    RecyclerView recyclerView;
    EntriesAdapter entriesAdapter;
    List<EntryModel> entries = new ArrayList<>();
    EditText searchText;
    ImageView newEntry, EmailDetails, sendToEmail;
    List<EmailModel> Email = new ArrayList<>();
    File file;
    String json;
    private int AuthenticationFlag = 0;
    BiometricPrompt biometricPrompt;
    LinearLayout homeLayout;
    FirebaseAnalytics mFirebaseAnalytics;
    BillingProcessor bp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,WindowManager.LayoutParams.FLAG_SECURE);

        final String[] categories = getResources().getStringArray(R.array.categories);
        spinner = findViewById(R.id.spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.hint, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        spinner.setAdapter(adapter);

        recyclerView = findViewById(R.id.recycler_entries);
        entries = loadEntries();
        json = new Gson().toJson(entries);
        File path = this.getFilesDir();
        file = new File(path, "entries.json");
        try {
            writeToFile(file, json);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_VIEW.equals(action) && type != null) {
            if ("application/json".equals(type)) {
                Uri uri = intent.getData();
                String fileContent = readTextFile(uri);
                System.out.println(fileContent);
                try {
                    JSONArray array = new JSONArray(fileContent);
                    List<EntryModel> list = new Gson().fromJson(String.valueOf(array), new TypeToken<ArrayList<EntryModel>>() {
                    }.getType());
                    List<EntryModel> listTemp = new ArrayList<>();
                    for (int i = 0; i < list.size(); i++) {
                        if (!loadEntries().contains(list.get(i))) {
                            listTemp.add(list.get(i)); // Unique List.
                        }
                    }
                    if (listTemp.size() > 0) {
                        new AlertDialog.Builder(this)
                                .setTitle("Warning!")
                                .setMessage("Some of the entries to be added are already present on this device, Do you want to overwrite them?\nIf no, only unique entries will be added")
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        EntriesAdapter entriesAdapter1 = new EntriesAdapter(list);
                                        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                                        recyclerView.setAdapter(entriesAdapter1);
                                        saveEntry(list);
                                        finish();
                                        startActivity(new Intent(MainActivity.this, MainActivity.class));
                                    }
                                })
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        entries.clear();
                                        entries.addAll(listTemp);
                                        EntriesAdapter entriesAdapter1 = new EntriesAdapter(entries);
                                        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                                        recyclerView.setAdapter(entriesAdapter1);
                                        saveEntry(entries);
                                        finish();
                                        startActivity(new Intent(MainActivity.this, MainActivity.class));
                                    }
                                }).setCancelable(false)
                                .setIcon(R.drawable.logo3)
                                .show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else if (type.startsWith("image/")) {
                // Handle single image being sent
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                // Handle multiple images being sent
            }
        } else {
            // Handle other intents, such as being started from the home screen
            System.out.println("nothing");
        }


        sendToEmail = findViewById(R.id.sendEmail);
        sendToEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (loadEmail().isEmpty()) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage("You must first set e-mail, subject and body before sending sharing your passwords")
                            .setTitle("Warning!")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent1 = new Intent(MainActivity.this, EmailDetails.class);
                                    startActivity(intent1);
                                    finish();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .setIcon(R.drawable.logo3)
                            .show();
                } else {
                    SendMail(getApplicationContext());
                }

            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        entriesAdapter = new EntriesAdapter(loadEntries());
        recyclerView.setAdapter(entriesAdapter);


        List<EntryModel> listWebsite = new ArrayList<>();
        List<EntryModel> listApp = new ArrayList<>();
        List<EntryModel> listEmail = new ArrayList<>();
        List<EntryModel> listOther = new ArrayList<>();

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (categories[position]) {
                    case "All": {
                        recyclerView.setAdapter(null);
                        entriesAdapter = new EntriesAdapter(loadEntries());
                        entriesAdapter.notifyDataSetChanged();
                        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                        recyclerView.setAdapter(entriesAdapter);
                    }
                    break;
                    case "Website": {
                        List<EntryModel> listTemp = new ArrayList<>();

                        for (int i = 0; i < loadEntries().size(); i++) {
                            if (loadEntries().get(i).getCategory().equals("Website")) {
                                listTemp.add(loadEntries().get(i));
                            }
                        }

                        listWebsite.clear();
                        listWebsite.addAll(listTemp);

                        recyclerView.setAdapter(null);
                        entriesAdapter = new EntriesAdapter(listWebsite);
                        entriesAdapter.notifyDataSetChanged();
                        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                        recyclerView.setAdapter(entriesAdapter);
                    }
                    break;
                    case "App": {
                        List<EntryModel> listTemp = new ArrayList<>();

                        for (int i = 0; i < loadEntries().size(); i++) {
                            if (loadEntries().get(i).getCategory().equals("App")) {
                                listTemp.add(loadEntries().get(i));
                            }
                        }

                        listApp.clear();
                        listApp.addAll(listTemp);

                        recyclerView.setAdapter(null);
                        entriesAdapter = new EntriesAdapter(listApp);
                        entriesAdapter.notifyDataSetChanged();
                        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                        recyclerView.setAdapter(entriesAdapter);
                    }
                    break;
                    case "Email": {
                        List<EntryModel> listTemp = new ArrayList<>();

                        for (int i = 0; i < loadEntries().size(); i++) {
                            if (loadEntries().get(i).getCategory().equals("Email")) {
                                listTemp.add(loadEntries().get(i));
                            }
                        }

                        listEmail.clear();
                        listEmail.addAll(listTemp);

                        recyclerView.setAdapter(null);
                        entriesAdapter = new EntriesAdapter(listEmail);
                        entriesAdapter.notifyDataSetChanged();
                        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                        recyclerView.setAdapter(entriesAdapter);

                    }
                    break;
                    case "Other": {
                        List<EntryModel> listTemp = new ArrayList<>();

                        for (int i = 0; i < loadEntries().size(); i++) {
                            if (loadEntries().get(i).getCategory().equals("Other")) {
                                listTemp.add(loadEntries().get(i));
                            }
                        }
                        listOther.clear();
                        listOther.addAll(listTemp);

                        recyclerView.setAdapter(null);
                        entriesAdapter = new EntriesAdapter(listOther);
                        entriesAdapter.notifyDataSetChanged();
                        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                        recyclerView.setAdapter(entriesAdapter);

                    }
                    break;

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        searchText = findViewById(R.id.searchText);
        searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (TextUtils.isEmpty(searchText.getText())) {
                    recyclerView.setAdapter(null);
                    recyclerView.setAdapter(entriesAdapter);
                } else {
                    if (spinner.getSelectedItem().equals("All")) {
                        List<EntryModel> tempList = new ArrayList<>();
                        recyclerView.setAdapter(null);
                        String newText = searchText.getText().toString();
                        for (int i = 0; i < entries.size(); i++) {
                            if (entries.get(i).getName().contains(newText)) {
                                tempList.add(entries.get(i));
                            }
                        }
                        EntriesAdapter tempAdapter = new EntriesAdapter(tempList);
                        recyclerView.setAdapter(tempAdapter);
                    } else if (spinner.getSelectedItem().equals("Website")) {
                        List<EntryModel> tempList = new ArrayList<>();
                        recyclerView.setAdapter(null);
                        String newText = searchText.getText().toString();
                        for (int i = 0; i < entries.size(); i++) {
                            if (entries.get(i).getCategory().equals("Website") && entries.get(i).getName().contains(newText)) {
                                tempList.add(entries.get(i));
                            }
                        }
                        EntriesAdapter tempAdapter = new EntriesAdapter(tempList);
                        recyclerView.setAdapter(tempAdapter);
                    } else if (spinner.getSelectedItem().equals("App")) {
                        List<EntryModel> tempList = new ArrayList<>();
                        recyclerView.setAdapter(null);
                        String newText = searchText.getText().toString();
                        for (int i = 0; i < entries.size(); i++) {
                            if (entries.get(i).getCategory().equals("App") && entries.get(i).getName().contains(newText)) {
                                tempList.add(entries.get(i));
                            }
                        }
                        EntriesAdapter tempAdapter = new EntriesAdapter(tempList);
                        recyclerView.setAdapter(tempAdapter);
                    } else if (spinner.getSelectedItem().equals("Email")) {
                        List<EntryModel> tempList = new ArrayList<>();
                        recyclerView.setAdapter(null);
                        String newText = searchText.getText().toString();
                        for (int i = 0; i < entries.size(); i++) {
                            if (entries.get(i).getCategory().equals("Email") && entries.get(i).getName().contains(newText)) {
                                tempList.add(entries.get(i));
                            }
                        }
                        EntriesAdapter tempAdapter = new EntriesAdapter(tempList);
                        recyclerView.setAdapter(tempAdapter);
                    } else if (spinner.getSelectedItem().equals("Other")) {
                        List<EntryModel> tempList = new ArrayList<>();
                        recyclerView.setAdapter(null);
                        String newText = searchText.getText().toString();
                        for (int i = 0; i < entries.size(); i++) {
                            if (entries.get(i).getCategory().equals("Other") && entries.get(i).getName().contains(newText)) {
                                tempList.add(entries.get(i));
                            }
                        }
                        EntriesAdapter tempAdapter = new EntriesAdapter(tempList);
                        recyclerView.setAdapter(tempAdapter);
                    }


                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        newEntry = findViewById(R.id.newEntry);
        newEntry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), EntryDetails.class);
                startActivity(intent);
                finish();
            }
        });

        EmailDetails = findViewById(R.id.email_details);
        EmailDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, EmailDetails.class);
                startActivity(intent);
            }
        });


        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        homeLayout = findViewById(R.id.layoutHome);

        // Firebase analytics handling here
        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Bundle bundle = new Bundle();
        bundle.putString("image_name", "kareem");
        bundle.putString("full_text", "text");
        firebaseAnalytics.logEvent("share_image", bundle);

        // Billing System handling here
//        bp = new BillingProcessor(this, );
//        bp.initialize();


    }


    private String readTextFile(Uri uri) {
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));
            String line = "";
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            System.out.println(builder.toString());
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return builder.toString();
    }


    ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();

            switch (direction) {
                case ItemTouchHelper.LEFT:
                    EntryModel deletedRecord = new EntryModel(entries.get(position).username, entries.get(position).password, entries.get(position).category, entries.get(position).name, entries.get(position).website, entries.get(position).notes);
                    entries.remove(position);
                    entriesAdapter.notifyItemRemoved(position);
                    saveEntry(entries);
                    Snackbar.make(recyclerView, "Item removed successfully", Snackbar.LENGTH_LONG)
                            .setAction("UNDO", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    entries.add(position, deletedRecord);
                                    entriesAdapter.notifyItemInserted(position);
                                    saveEntry(entries);
                                }
                            }).show();

                    break;
            }

        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            new RecyclerViewSwipeDecorator.Builder(getApplicationContext(), c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    .addSwipeLeftBackgroundColor(0xFF2962FF)
                    .addSwipeLeftActionIcon(R.drawable.delete)
                    .create()
                    .decorate();
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onResume() {
        super.onResume();
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        authenticateFingerPrint();
        homeLayout.setVisibility(View.INVISIBLE);
        if (AuthenticationFlag == 1 || !keyguardManager.isDeviceSecure()) {
            homeLayout.setVisibility(View.VISIBLE);
            AuthenticationFlag = 0;
        }
//        homeLayout.setVisibility(View.INVISIBLE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                AuthenticationFlag = 1;
                homeLayout.setVisibility(View.VISIBLE);
            }
        }

//        if (!bp.handleActivityResult(requestCode,resultCode,data)) {
//            super.onActivityResult(requestCode, resultCode, data);
//            if (resultCode == RESULT_OK) {
//                bp.subscribe(this,)
//            }
//        }
    }

//    @Override
//    protected void onDestroy() {
//        if (bp != null) {
//            bp.release();
//        }
//        super.onDestroy();
//    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    public void authenticateFingerPrint() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        PackageManager packageManager = this.getPackageManager();
        Executor executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt.Builder(this)
                .setDescription("Please touch the fingerprint sensor")
                .setTitle("Authentication")
                .setNegativeButton("Login with PIN code", this.getMainExecutor(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
//                        authenticateFingerPrint();
                        if (keyguardManager.isDeviceSecure()) {
                            Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(null,null);
                            startActivityForResult(intent, 1);
                        }
                    }
                })
                .build();



        if (AuthenticationFlag == 0) {
            biometricPrompt.authenticate(new CancellationSignal(), executor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    authenticateFingerPrint();
                }

                @Override
                public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                    super.onAuthenticationHelp(helpCode, helpString);
                }

                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    homeLayout.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                }
            });
        }


    }

    public List<EntryModel> loadEntries() {
        SharedPreferences preferences = getApplicationContext().getSharedPreferences("Entries", 0);
        Gson gson = new Gson();
        String json = preferences.getString("entry", null);
        Type type = new TypeToken<ArrayList<EntryModel>>() {
        }.getType();
        entries = gson.fromJson(json, type);
        if (entries == null) {
            entries = new ArrayList<>();
        }

        return entries;
    }

    public void saveEntry(List<EntryModel> list) {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("Entries", 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString("entry", json);
        editor.apply();
    }

    private void writeToFile(File file, String data) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(data.getBytes());
        fos.close();

    }


    public List<EmailModel> loadEmail() {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("Email", 0);
        Gson gson = new Gson();
        String json = sharedPreferences.getString("email", null);
        Type type = new TypeToken<ArrayList<EmailModel>>() {
        }.getType();
        Email = gson.fromJson(json, type);
        if (Email == null) {
            Email = new ArrayList<>();
        }

        return Email;
    }

    private void SendMail(Context context) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/json");
        String directory = (String.valueOf(context.getFilesDir()));
        File file = new File(directory + File.separator + "entries.json");
        Uri uri = FileProvider.getUriForFile(context, "com.hwaya.candytest.fileprovider", file);
        intent.putExtra(Intent.EXTRA_EMAIL, loadEmail().get(0).getEmail());
        intent.putExtra(Intent.EXTRA_SUBJECT, loadEmail().get(0).getSubject());
        intent.putExtra(Intent.EXTRA_TEXT, loadEmail().get(0).getBody());
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);

    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    public void onProductPurchased(String productId, TransactionDetails details) {

    }

    @Override
    public void onPurchaseHistoryRestored() {

    }

    @Override
    public void onBillingError(int errorCode, Throwable error) {

    }

    @Override
    public void onBillingInitialized() {

    }
}