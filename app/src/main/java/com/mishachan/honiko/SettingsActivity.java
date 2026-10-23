package com.mishachan.honiko;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.mishachan.honiko.data.ApiClient;
import com.mishachan.honiko.data.Prefs;

import org.json.JSONObject;

public class SettingsActivity extends AppCompatActivity {

    private EditText serverField;
    private EditText tokenField;
    private TextView statusView;
    private Button saveButton;
    private Button testButton;
    private Button cancelButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        serverField = findViewById(R.id.server_url);
        tokenField = findViewById(R.id.token);
        statusView = findViewById(R.id.test_status);
        saveButton = findViewById(R.id.save);
        testButton = findViewById(R.id.test);
        cancelButton = findViewById(R.id.cancel);

        serverField.setText(Prefs.serverUrl(this));
        tokenField.setText(Prefs.token(this));

        saveButton.setOnClickListener(v -> {
            Prefs.save(this, serverField.getText().toString(), tokenField.getText().toString());
            Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
            finish();
        });

        testButton.setOnClickListener(v -> testConnection());
        cancelButton.setOnClickListener(v -> finish());

        serverField.requestFocus();
    }

    private void testConnection() {
        setTesting(true);
        statusView.setVisibility(View.VISIBLE);
        statusView.setTextColor(getColor(R.color.muted));
        statusView.setText(R.string.testing);

        ApiClient.testServer(serverField.getText().toString(), tokenField.getText().toString(),
                new ApiClient.Callback<JSONObject>() {
                    @Override
                    public void onSuccess(JSONObject health) {
                        if (isFinishing()) {
                            return;
                        }
                        setTesting(false);
                        statusView.setTextColor(getColor(R.color.text));
                        statusView.setText(getString(R.string.test_ok,
                                health.optInt("videos", 0),
                                health.optString("version", "?")));
                    }

                    @Override
                    public void onFailure(Exception error) {
                        if (isFinishing()) {
                            return;
                        }
                        setTesting(false);
                        statusView.setTextColor(getColor(R.color.accent));
                        String detail = error.getMessage();
                        statusView.setText(getString(R.string.test_failed,
                                detail == null ? "" : detail));
                    }
                });
    }

    private void setTesting(boolean testing) {
        saveButton.setEnabled(!testing);
        testButton.setEnabled(!testing);
        cancelButton.setEnabled(!testing);
    }
}
