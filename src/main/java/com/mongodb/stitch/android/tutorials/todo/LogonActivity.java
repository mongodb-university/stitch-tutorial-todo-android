package com.mongodb.stitch.android.tutorials.todo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;
import com.mongodb.stitch.core.auth.providers.anonymous.AnonymousCredential;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.Task;
import com.mongodb.stitch.core.auth.providers.google.GoogleCredential;

import static com.google.android.gms.auth.api.Auth.GOOGLE_SIGN_IN_API;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import com.mongodb.stitch.core.auth.providers.facebook.FacebookCredential;

public class LogonActivity extends AppCompatActivity {

    private GoogleApiClient _googleApiClient;
    private static final int GOOGLE_SIGN_IN = 421;


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
        final String googleWebClientId = getString(R.string.google_web_client_id);
        enableGoogleAuth(googleWebClientId);

        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
        if (isLoggedIn) {
            LoginManager.getInstance().logOut();
        }

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

    private void enableGoogleAuth(String googleWebClientId) {

        final GoogleSignInOptions gso =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestServerAuthCode(googleWebClientId, true).build();

        _googleApiClient = new GoogleApiClient.Builder(LogonActivity.this)
                .enableAutoManage(LogonActivity.this, connectionResult ->
                        Log.e("Stitch Auth", "Error connecting to google: " + connectionResult.getErrorMessage()))
                .addApi(GOOGLE_SIGN_IN_API, gso)
                .build();

        findViewById(R.id.google_login_button).setOnClickListener(v -> {
            if (!_googleApiClient.isConnected()) {
                _googleApiClient.connect();
            }
            GoogleSignInClient mGoogleSignInClient =
                    GoogleSignIn.getClient(LogonActivity.this, gso);

            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, GOOGLE_SIGN_IN);
        });
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            final GoogleCredential googleCredential =
                    new GoogleCredential(account.getServerAuthCode());

            TodoListActivity.client.getAuth().loginWithCredential(googleCredential).addOnCompleteListener(
                    task -> {
                        if (task.isSuccessful()) {
                            setResult(Activity.RESULT_OK);
                            finish();
                        } else {
                            Log.e("Stitch Auth", "Error logging in with Google", task.getException());
                        }
                    });

        } catch (ApiException e) {
            Log.w("GOOGLE AUTH FAILURE", "signInResult:failed code=" + e.getStatusCode());
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }

    private void enableFacebookAuth() {
        _callbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(_callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                final FacebookCredential fbCredential =
                        new FacebookCredential(AccessToken.getCurrentAccessToken().getToken());
                TodoListActivity.client.getAuth()
                        .loginWithCredential(fbCredential).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        setResult(Activity.RESULT_OK);
                        finish();
                    } else {
                        Log.e("Stitch Auth", "Error logging in with Facebook",
                                task.getException());
                    }
                });
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
        if (requestCode == GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleGoogleSignInResult(task);
            return;
        } else {
            _callbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }
}
