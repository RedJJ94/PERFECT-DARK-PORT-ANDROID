package com.perfectdark.port;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Launcher that ensures the Perfect Dark ROM exists in
 * /perfect dark as pd.ntsc-final.z64 before starting SDL.
 */
public class LauncherActivity extends AppCompatActivity {
    private static final String ROM_FILE_NAME = "pd.ntsc-final.z64";
    // Primary: NTSC-U Rev 1 (v1.1) .z64 (recommended)
    private static final String MD5_NTSC_V11 = "e03b088b6ac9e0080440efed07c1e40f";
    // Secondary: NTSC-U v1.0 .z64 (not recommended, but optionally allowed)
    private static final String MD5_NTSC_V10 = "7f4171b0c8d17815be37913f535e4e93";
    
    private static final String PREFS_NAME = "RomValidationPrefs";
    private static final String KEY_ROM_VALIDATED = "rom_validated";
    private static final String KEY_ROM_MD5 = "rom_md5";

    private View missingRomView;
    private TextView infoText;
    private Button pickRomButton;

    private final ActivityResultLauncher<String[]> romPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onRomPicked);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        missingRomView = findViewById(R.id.missingRomContainer);
        infoText = findViewById(R.id.infoText);
        pickRomButton = findViewById(R.id.pickRomButton);

        pickRomButton.setOnClickListener(v -> openRomPicker());

        ensureDataDir();

        if (romExists()) {
            File target = new File(new File(Environment.getExternalStorageDirectory(), "perfect dark"), ROM_FILE_NAME);
            
            // Check if ROM was already validated
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            boolean wasValidated = prefs.getBoolean(KEY_ROM_VALIDATED, false);
            String savedMd5 = prefs.getString(KEY_ROM_MD5, null);
            
            if (wasValidated && savedMd5 != null) {
                // Verify the saved MD5 still matches the current file
                try {
                    String currentMd5 = computeMd5(target);
                    if (savedMd5.equalsIgnoreCase(currentMd5)) {
                        // ROM unchanged, skip verification
                        startGame();
                        return;
                    }
                } catch (Exception e) {
                    // If hash check fails, fall through to full verification
                }
            }
            
            // Full verification needed
            int hashStatus = checkRomHash(target);
            if (hashStatus == 0) {
                // Save validation state for v1.1
                saveRomValidation(target, MD5_NTSC_V11);
                startGame();
            } else if (hashStatus == 1) {
                // Save validation state for v1.0
                saveRomValidation(target, MD5_NTSC_V10);
                showV10WarningDialog(target);
            } else {
                showHashMismatchDialog(target);
            }
        } else {
            showMissingRomUi();
        }
    }

    private void ensureDataDir() {
        File perfectDarkDir = new File(Environment.getExternalStorageDirectory(), "perfect dark");
        if (!perfectDarkDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            perfectDarkDir.mkdirs();
        }
    }

    private boolean romExists() {
        File target = new File(new File(Environment.getExternalStorageDirectory(), "perfect dark"), ROM_FILE_NAME);
        return target.exists() && target.length() > 0;
    }

    private void showMissingRomUi() {
        missingRomView.setVisibility(View.VISIBLE);
        infoText.setText("ROM not found. Select your Perfect Dark NTSC (z64) ROM to proceed.\nIt will be copied to /perfect dark as " + ROM_FILE_NAME + ".");
    }

    private void openRomPicker() {
        // Use SAF OpenDocument so we can persist read permission if supported.
        romPicker.launch(new String[]{"application/octet-stream", "*/*"});
    }

    private void onRomPicked(@Nullable Uri uri) {
        if (uri == null) {
            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Take persistable permission so we can read during copy
        final int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
        try {
            getContentResolver().takePersistableUriPermission(uri, flags);
        } catch (Exception ignored) {
            // Not critical; some providers don't support persistable perms
        }

        try {
            copyRomToAppData(uri);
        } catch (IOException e) {
            Toast.makeText(this, "Failed to copy ROM: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        if (romExists()) {
            File target = new File(new File(Environment.getExternalStorageDirectory(), "perfect dark"), ROM_FILE_NAME);
            int hashStatus = checkRomHash(target);
            if (hashStatus == 0) {
                // Save validation state for v1.1
                saveRomValidation(target, MD5_NTSC_V11);
                Toast.makeText(this, "ROM verified — starting game", Toast.LENGTH_SHORT).show();
                startGame();
            } else if (hashStatus == 1) {
                // Save validation state for v1.0
                saveRomValidation(target, MD5_NTSC_V10);
                showV10WarningDialog(target);
            } else {
                showHashMismatchDialog(target);
            }
        } else {
            Toast.makeText(this, "ROM copy failed", Toast.LENGTH_LONG).show();
        }
    }

    private void copyRomToAppData(Uri sourceUri) throws IOException {
        File perfectDarkDir = new File(Environment.getExternalStorageDirectory(), "perfect dark");
        if (!perfectDarkDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            perfectDarkDir.mkdirs();
        }

        File target = new File(perfectDarkDir, ROM_FILE_NAME);

        try (InputStream in = getContentResolver().openInputStream(sourceUri);
             FileOutputStream out = new FileOutputStream(target)) {
            if (in == null) throw new IOException("Unable to open selected file");
            byte[] buf = new byte[8192];
            int read;
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
            out.flush();
        }
    }

    private void startGame() {
        // Hand off to SDL/MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        // Forward any multiplayer or other extras
        if (getIntent().getExtras() != null) {
            intent.putExtras(getIntent().getExtras());
        }
        // Ensure we don’t come back here when user quits the game
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    // Returns: 0 = matches v1.1 (recommended), 1 = matches v1.0 (allowed), -1 = mismatch/error
    private int checkRomHash(File file) {
        try {
            String md5 = computeMd5(file);
            if (MD5_NTSC_V11.equalsIgnoreCase(md5)) return 0;
            if (MD5_NTSC_V10.equalsIgnoreCase(md5)) return 1;
            return -1;
        } catch (Exception e) {
            Toast.makeText(this, "Hash check failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return -1;
        }
    }

    private void saveRomValidation(File file, String md5) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_ROM_VALIDATED, true);
        editor.putString(KEY_ROM_MD5, md5);
        editor.apply();
    }

    private void showHashMismatchDialog(File target) {
        String computed;
        try {
            computed = computeMd5(target);
        } catch (Exception e) {
            computed = "<error>";
        }

        new AlertDialog.Builder(this)
                .setTitle("Wrong ROM version")
                .setMessage("Expected NTSC-U v1.1 ROM (md5: " + MD5_NTSC_V11 + ")\nAlso allowed (not recommended): v1.0 (md5: " + MD5_NTSC_V10 + ")\n\nGot: " + computed + "\n\nPick a different .z64 ROM?")
                .setPositiveButton("Pick another", (d, w) -> {
                    // Remove the copied file to avoid confusion
                    try { //noinspection ResultOfMethodCallIgnored
                        target.delete();
                    } catch (Exception ignored) {}
                    showMissingRomUi();
                })
                .setNegativeButton("Proceed anyway", (d, w) -> startGame())
                .setCancelable(false)
                .show();
    }

    private void showV10WarningDialog(File target) {
        new AlertDialog.Builder(this)
                .setTitle("NTSC v1.0 detected")
                .setMessage("You selected NTSC-U v1.0 (not recommended).\nThe port targets v1.1; some content may not work.\n\nProceed with v1.0 or pick a different ROM?")
                .setPositiveButton("Proceed", (d, w) -> startGame())
                .setNegativeButton("Pick another", (d, w) -> {
                    try { //noinspection ResultOfMethodCallIgnored
                        target.delete();
                    } catch (Exception ignored) {}
                    showMissingRomUi();
                })
                .setCancelable(false)
                .show();
    }

    private String computeMd5(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] buffer = new byte[8192];
        int read;
        try (InputStream in = new java.io.FileInputStream(file);
             DigestInputStream din = new DigestInputStream(in, md)) {
            while ((read = din.read(buffer)) != -1) {
                // digest updated via DigestInputStream
            }
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
