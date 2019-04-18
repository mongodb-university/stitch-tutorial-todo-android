package com.mongodb.stitch.android.tutorials.todo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import com.mongodb.stitch.android.core.auth.StitchUser;
import com.mongodb.stitch.core.auth.providers.anonymous.AnonymousCredential;
import com.mongodb.stitch.core.auth.providers.facebook.FacebookCredential;

public class LogonActivity extends AppCompatActivity {

    private CallbackManager _callbackManager;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.logon);

        setupLogin();
    }

    private void setupLogin() {
        setContentView(R.layout.logon);
        enableAnonymousAuth();

        // 5. Make sure that the current user is logged off whenever
        // the login activity is shown.

        enableFacebookAuth();
    }

    private void enableAnonymousAuth() {
        findViewById(R.id.anon_login_button).setOnClickListener(ignored ->
                TodoListActivity.client.getAuth().loginWithCredential(new AnonymousCredential())
                        .addOnSuccessListener(user -> {
                            Toast.makeText(LogonActivity.this,
                                    "Logged in Anonymously. ID: " + user.getId(),
                                    Toast.LENGTH_LONG).show();
                            setResult(Activity.RESULT_OK);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Log.d("Stitch Auth", "error logging in", e);
                            Toast.makeText(LogonActivity.this, "Failed to log in Anonymously. " +
                                            "Did you enable Anonymous Auth in your Stitch backend and copy " +
                                            "your Stitch App ID to strings.xml?",
                                    Toast.LENGTH_LONG).show();
                        }));
    }

    private void enableFacebookAuth() {

        // 1. Initialize the CallbackManager


        // 2. Register the CallbackManager with the LoginManager instance
        LoginManager.getInstance().registerCallback(_callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {

                // 3. On successful login, obtain the Facebook credential and
                // pass it to Stitch via the loginWithCredential() method.

            }

            @Override
            public void onCancel() {
                Toast.makeText(LogonActivity.this, "Facebook logon was " +
                        "cancelled.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(final FacebookException exception) {
                Toast.makeText(LogonActivity.this, "Failed to logon with " +
                        "Facebook. Result: " + exception.toString(), Toast.LENGTH_LONG).show();

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 4. Handle the result that Facebook returns to us

    }
}