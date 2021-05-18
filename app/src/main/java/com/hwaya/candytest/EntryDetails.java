package com.hwaya.candytest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.w3c.dom.Text;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

public class EntryDetails extends AppCompatActivity {
    TextView titleDetails;
    EditText name_details;
    EditText username_details;
    EditText password_details;
    EditText website_details;
    Spinner category_details;
    EditText notes_details;
    Button saveButton;
    LinearLayout back;
    ImageView copyUser, copyPass, copyWebsite;
    LinearLayout entryLayout;
    int position;
    List<EntryModel> entries = new ArrayList<>();
    BiometricPrompt biometricPrompt;
    private int AuthenticationFlag = 0;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_details);
        getSupportActionBar().hide();

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,WindowManager.LayoutParams.FLAG_SECURE);


        Intent intent = getIntent();
        titleDetails = findViewById(R.id.title_details);
        name_details = findViewById(R.id.name_details);
        username_details = findViewById(R.id.username_details);
        password_details = findViewById(R.id.Password_details);
        website_details = findViewById(R.id.website_details);
        category_details = findViewById(R.id.spinner_details);
        notes_details = findViewById(R.id.notes_details);
        saveButton = findViewById(R.id.saveButton);
        back = findViewById(R.id.back);
        copyUser = findViewById(R.id.copyUser);
        copyPass = findViewById(R.id.copyPass);
        copyWebsite = findViewById(R.id.copyWebsite);
        entryLayout = findViewById(R.id.layoutDetails);

        titleDetails.setText(intent.getStringExtra("Name"));
        name_details.setText(intent.getStringExtra("Name"));
        username_details.setText(intent.getStringExtra("Username"));
        password_details.setText(intent.getStringExtra("Password"));
        website_details.setText(intent.getStringExtra("Website"));
        notes_details.setText(intent.getStringExtra("Notes"));
        position = intent.getIntExtra("position",-1);
        entryLayout.setVisibility(View.INVISIBLE);




        final String[] categories = getResources().getStringArray(R.array.categoriesDetails);
        String categoryChoice = intent.getStringExtra("Category");

        int selection=0;
        if (categoryChoice != null) {
            for (int i=0;i<categories.length;i++) {
                if (intent.getStringExtra("Category").equals(categories[i])) {
                    selection = i;
                }
            }
            Collections.swap(Arrays.asList(categories),selection,0);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.hint, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        category_details.setAdapter(adapter);

        // To go back to the main page.
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent1 = new Intent(EntryDetails.this, MainActivity.class);
                startActivity(intent1);
                finish();
            }
        });

        // To copy the Username to the clipboard.
        copyUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("User",username_details.getText());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(EntryDetails.this, "Copied to clipboard.", Toast.LENGTH_SHORT).show();
            }
        });

        // To copy the Password to the clipboard.
        copyPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Password",password_details.getText());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(EntryDetails.this, "Copied to clipboard.", Toast.LENGTH_SHORT).show();
            }
        });

        // To copy the Website to the clipboard.
        copyWebsite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Website",website_details.getText());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(EntryDetails.this, "Copied to clipboard.", Toast.LENGTH_SHORT).show();
            }
        });

        // To Save the new entry or to update an already existing one
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(name_details.getText().toString())) {
                    name_details.setError("Name is required");
                }
                else if (TextUtils.isEmpty(username_details.getText().toString())) {
                    username_details.setError("UserName is required");
                }
               else if (TextUtils.isEmpty(password_details.getText().toString())) {
                    password_details.setError("Password is required");
                }
               else {
                   if (position == -1) {

                       Toast.makeText(getApplicationContext(), "Saved successfully", Toast.LENGTH_SHORT).show();
                       entries.clear();
                       entries.addAll(loadEntries());
                       entries.add(new EntryModel(username_details.getText().toString(),
                               password_details.getText().toString(),
                               category_details.getSelectedItem().toString(),
                               name_details.getText().toString(),
                               website_details.getText().toString(),
                               notes_details.getText().toString()));
                       
                       saveEntry(entries);
                   } else {
                       List<EntryModel> ListTemp = new ArrayList<>();
                       ListTemp.addAll(loadEntries());
                       ListTemp.remove(position);
                       ListTemp.add(position,new EntryModel(username_details.getText().toString(),
                               password_details.getText().toString(),
                               category_details.getSelectedItem().toString(),
                               name_details.getText().toString(),
                               website_details.getText().toString(),
                               notes_details.getText().toString()));

                       saveEntry(ListTemp);
                   }

                    Intent intent1 = new Intent(EntryDetails.this,MainActivity.class);
                    startActivity(intent1);
                    finish();
                }


            }
        });


    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onResume() {
        super.onResume();
        entryLayout.setVisibility(View.INVISIBLE);
        authenticateFingerPrint();
        if (AuthenticationFlag == 1) {
            entryLayout.setVisibility(View.VISIBLE);
            AuthenticationFlag = 0;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                AuthenticationFlag = 1;
                entryLayout.setVisibility(View.VISIBLE);
            }
        }
    }

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
                    entryLayout.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                }
            });
        }


    }

    public List<EntryModel> loadEntries () {
        SharedPreferences preferences = getApplicationContext().getSharedPreferences("Entries", 0);
        Gson gson = new Gson();
        String json = preferences.getString("entry",null);
        Type type = new TypeToken<ArrayList<EntryModel>> () {
        }.getType();
        entries = gson.fromJson(json,type);
        if (entries == null) {
            entries = new ArrayList<>();
        }

        return entries;
    }

    public void saveEntry (List<EntryModel> list) {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("Entries",0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString("entry",json);
        editor.apply();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(EntryDetails.this,MainActivity.class);
        startActivity(intent);
        finish();
    }
}