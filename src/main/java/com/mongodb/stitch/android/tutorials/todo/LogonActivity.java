package com.mongodb.stitch.android.tutorials.todo;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;
import com.mongodb.stitch.core.auth.providers.anonymous.AnonymousCredential;

public class LogonActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.logon);
        setupLogin();
    }

    private void setupLogin() {
        setContentView(R.layout.logon);
        enableAnonymousAuth();
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
}
