package de.timomeh.ribbit.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import de.timomeh.ribbit.R;
import de.timomeh.ribbit.RibbitApplication;


public class SignUpActivity extends ActionBarActivity {

    protected EditText mUsername;
    protected EditText mPassword;
    protected EditText mEmail;
    protected Button mSignUpButton;
    protected ProgressBar mProgressBar;
    protected Button mCancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mUsername = (EditText) findViewById(R.id.usernameField);
        mPassword = (EditText) findViewById(R.id.passwordField);
        mEmail = (EditText) findViewById(R.id.emailField);
        mSignUpButton = (Button) findViewById(R.id.signupButton);
        mCancelButton = (Button) findViewById(R.id.cancelButton);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mSignUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = mUsername.getText().toString();
                String password = mPassword.getText().toString();
                String email = mEmail.getText().toString();

                username = username.trim();
                password = password.trim();
                email = email.trim();

                if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(SignUpActivity.this);
                    builder.setMessage(getString(R.string.sign_up_error_message))
                            .setTitle(getString(R.string.sign_up_error_title))
                            .setPositiveButton(getString(android.R.string.ok), null);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
                else {
                    toggleProgressBar();

                    ParseUser newUser = new ParseUser();
                    newUser.setUsername(username);
                    newUser.setPassword(password);
                    newUser.setEmail(email);
                    newUser.signUpInBackground(new SignUpCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                // Success!
                                RibbitApplication.updateParseInstallation(ParseUser.getCurrentUser());

                                Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            }
                            else {
                                toggleProgressBar();

                                AlertDialog.Builder builder = new AlertDialog.Builder(SignUpActivity.this);
                                builder.setMessage(e.getMessage())
                                        .setTitle(getString(R.string.sign_up_error_title))
                                        .setPositiveButton(getString(android.R.string.ok), null);
                                AlertDialog dialog = builder.create();
                                dialog.show();
                            }
                        }
                    });
                }
            }
        });
    }

    private void toggleProgressBar() {
        int state0 = View.INVISIBLE;
        int state1 = View.VISIBLE;

        if(mProgressBar.getVisibility() == View.INVISIBLE) {
            state0 = View.VISIBLE;
            state1 = View.INVISIBLE;
        }

        mProgressBar.setVisibility(state0);
        mUsername.setVisibility(state1);
        mPassword.setVisibility(state1);
        mEmail.setVisibility(state1);
        mSignUpButton.setVisibility(state1);
    }
}
