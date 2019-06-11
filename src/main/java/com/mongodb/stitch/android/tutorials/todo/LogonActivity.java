package com.mongodb.stitch.android.tutorials.todo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.Task;
import com.mongodb.stitch.core.auth.providers.anonymous.AnonymousCredential;
import com.mongodb.stitch.core.auth.providers.google.GoogleCredential;

import static com.google.android.gms.auth.api.Auth.GOOGLE_SIGN_IN_API;

public class LogonActivity extends AppCompatActivity {

    private GoogleApiClient _googleApiClient;
    private static final int GOOGLE_SIGN_IN = 421;

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
        // 1. Create a new GoogleSignInOptions object by calling build() on a
        //    new GoogleSignInOptions.Builder object.
        final GoogleSignInOptions gso =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestServerAuthCode(googleWebClientId, true).build();


        // 2. Initialize the _googleApiClient
        _googleApiClient = new GoogleApiClient.Builder(LogonActivity.this)
                .enableAutoManage(LogonActivity.this, connectionResult ->
                        Log.e("Stitch Auth", "Error connecting to google: " + connectionResult.getErrorMessage()))
                .addApi(GOOGLE_SIGN_IN_API, gso)
                .build();

        // 3. Create an onclick listener for the google_login_button
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
            // 5. Create a GoogleSignInAccount from the task result.
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // 5a. Create a GoogleCredential from the account.
            final GoogleCredential googleCredential =
                    new GoogleCredential(account.getServerAuthCode());

            // 5b. Authenticate against Stitch. If the task is successful, set the result to
            //      Activity.RESULT_OK and end this activity, returning control to the TodoListActivity
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


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 4. Handle the result that Google sends back to us
        if (requestCode == GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleGoogleSignInResult(task);
            return;
        }
    }
}