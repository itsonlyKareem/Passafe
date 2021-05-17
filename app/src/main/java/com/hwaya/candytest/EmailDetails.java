package com.hwaya.candytest;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.KeyguardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class EmailDetails extends AppCompatActivity {
    LinearLayout back;
    EditText email, emailSubject, emailBody;
    Button save;
    LinearLayout emailLayout;
    List<EmailModel> Email;
    private int AuthenticationFlag = 0;
    BiometricPrompt biometricPrompt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_details);
        getSupportActionBar().hide();

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,WindowManager.LayoutParams.FLAG_SECURE);

        back = findViewById(R.id.backFromEmail);
        email = findViewById(R.id.EmailText);
        emailSubject = findViewById(R.id.EmailSubject);
        emailBody = findViewById(R.id.EmailBody);
        save = findViewById(R.id.SaveEmail);
        emailLayout = findViewById(R.id.LayoutEmail);

        emailLayout.setVisibility(View.INVISIBLE);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Email.clear();
                Email.add(new EmailModel(email.getText().toString(), emailSubject.getText().toString(), emailBody.getText().toString()));
                saveEmail(Email);
                Intent intent = new Intent(EmailDetails.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        if (loadEmail().size() == 1) {
            email.setText(loadEmail().get(0).email);
            emailSubject.setText(loadEmail().get(0).subject);
            emailBody.setText(loadEmail().get(0).body);
        } else {
            email.setText(null);
            emailSubject.setText(null);
            emailBody.setText(null);
        }


        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EmailDetails.this,MainActivity.class);
                startActivity(intent);
                finish();
            }
        });


    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onResume() {
        super.onResume();
        emailLayout.setVisibility(View.INVISIBLE);
        authenticateFingerPrint();
        if (AuthenticationFlag == 1) {
            AuthenticationFlag = 0;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                AuthenticationFlag = 1;
                emailLayout.setVisibility(View.VISIBLE);
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
                    emailLayout.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                }
            });
        }


    }

    public void saveEmail (List<EmailModel> list) {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("Email",0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString("email",json);
        editor.apply();
    }

    public List<EmailModel> loadEmail () {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("Email",0);
        Gson gson = new Gson();
        String json = sharedPreferences.getString("email",null);
        Type type = new TypeToken<ArrayList<EmailModel>>() {}.getType();
        Email = gson.fromJson(json,type);
        if (Email == null) {
            Email = new ArrayList<>();
        }

        return Email;
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(EmailDetails.this,MainActivity.class);
        startActivity(intent);
        finish();
    }
}