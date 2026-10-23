package com.mishachan.honiko;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.mishachan.honiko.data.ApiClient;
import com.mishachan.honiko.data.Library;
import com.mishachan.honiko.data.Prefs;
import com.mishachan.honiko.ui.RowsFragment;

public class MainActivity extends FragmentActivity {

    private RowsFragment rowsFragment;
    private View rowsContainer;
    private View loadingView;
    private View errorView;
    private TextView statusView;
    private TextView errorText;
    private Button refreshButton;
    private Button settingsButton;
    private Button retryButton;

    private Library library;
    private boolean loading;
    private boolean fullScreenLoad;
    private boolean firstLoad = true;
    private long lastLoadedAt;
    private String lastBase;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rowsContainer = findViewById(R.id.rows_container);
        loadingView = findViewById(R.id.loading);
        errorView = findViewById(R.id.error);
        statusView = findViewById(R.id.status);
        errorText = findViewById(R.id.error_text);
        refreshButton = findViewById(R.id.refresh);
        settingsButton = findViewById(R.id.settings);
        retryButton = findViewById(R.id.retry);
        Button errorSettings = findViewById(R.id.error_settings);

        rowsFragment = (RowsFragment) getSupportFragmentManager()
                .findFragmentById(R.id.rows_container);
        if (rowsFragment == null) {
            rowsFragment = new RowsFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.rows_container, rowsFragment)
                    .commitNow();
        }

        refreshButton.setOnClickListener(v -> load(false));
        settingsButton.setOnClickListener(v -> openSettings());
        retryButton.setOnClickListener(v -> load(true));
        errorSettings.setOnClickListener(v -> openSettings());

        lastBase = Prefs.serverUrl(this);
        load(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String base = Prefs.serverUrl(this);
        if (!base.equals(lastBase)) {
            lastBase = base;
            load(true);
            return;
        }
        if (library != null && System.currentTimeMillis() - lastLoadedAt > 15000) {
            load(false);
        }
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void load(boolean fullScreen) {
        if (loading) {
            return;
        }
        loading = true;
        fullScreenLoad = fullScreen;
        lastBase = Prefs.serverUrl(this);
        refreshButton.setEnabled(false);
        statusView.setText(R.string.refreshing);
        if (fullScreen) {
            showLoading();
        }

        ApiClient.loadLibrary(this, new ApiClient.Callback<Library>() {
            @Override
            public void onSuccess(Library value) {
                loading = false;
                refreshButton.setEnabled(true);
                library = value;
                lastLoadedAt = System.currentTimeMillis();
                statusView.setText(getString(R.string.status_format,
                        value.videoCount, Prefs.hostLabel(MainActivity.this)));
                rowsFragment.show(value);

                if (value.isEmpty() || rowsFragment.isEmpty()) {
                    showEmpty();
                    firstLoad = false;
                    return;
                }

                showRows();
                if (firstLoad || fullScreenLoad) {
                    firstLoad = false;
                    focusRows();
                }
            }

            @Override
            public void onFailure(Exception error) {
                loading = false;
                refreshButton.setEnabled(true);
                statusView.setText(R.string.connection_failed);
                if (fullScreenLoad || library == null || rowsFragment.isEmpty()) {
                    showError(describe(error));
                } else {
                    showRows();
                    Toast.makeText(MainActivity.this, R.string.refresh_failed,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showLoading() {
        loadingView.setVisibility(View.VISIBLE);
        errorView.setVisibility(View.GONE);
        rowsContainer.setVisibility(View.INVISIBLE);
    }

    private void showRows() {
        errorView.setVisibility(View.GONE);
        loadingView.setVisibility(View.GONE);
        rowsContainer.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        errorText.setText(message);
        errorView.setVisibility(View.VISIBLE);
        loadingView.setVisibility(View.GONE);
        rowsContainer.setVisibility(View.INVISIBLE);
        retryButton.post(retryButton::requestFocus);
    }

    private void showEmpty() {
        showError(getString(R.string.no_videos));
    }

    private void focusRows() {
        View root = rowsFragment.getView();
        if (root == null) {
            return;
        }
        root.post(() -> {
            View target = findFocusable(root);
            if (target != null) {
                target.requestFocus();
            }
        });
    }

    private View findFocusable(View view) {
        if (view.getVisibility() != View.VISIBLE) {
            return null;
        }
        if (view.isFocusable()) {
            return view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findFocusable(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private String describe(Exception error) {
        if (error instanceof ApiClient.ApiException) {
            int code = ((ApiClient.ApiException) error).code;
            if (code == 401 || code == 403) {
                return getString(R.string.unauthorized);
            }
        }
        String detail = error.getMessage();
        if (detail == null) {
            detail = "";
        }
        if (detail.isEmpty()) {
            return getString(R.string.connection_failed) + "\n\n"
                    + getString(R.string.server_error_hint);
        }
        return getString(R.string.connection_failed) + "\n\n"
                + getString(R.string.server_error_hint) + "\n\n" + detail;
    }
}
