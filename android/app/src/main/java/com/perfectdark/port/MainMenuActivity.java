package com.perfectdark.port;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;


/**
 * MainMenuActivity — Tela inicial do Perfect Dark Android Port.
 * Inicia o MusicService (menu.mp3) ao entrar.
 * Para a música ao iniciar o jogo ou sair do app.
 * A música continua tocando ao abrir Configurações (serviço não é parado).
 */
public class MainMenuActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1001;

    private MaterialButton btnStart;
    private MaterialButton btnSettings;
    private MaterialButton btnExit;

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        btnStart    = findViewById(R.id.btnStart);
        btnSettings = findViewById(R.id.btnSettings);
        btnExit     = findViewById(R.id.btnExit);

        checkAndRequestStoragePermissions();
        setupListeners();
        animateEntrance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Inicia (ou retoma) a música ao entrar/voltar para o menu
        startMusicService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Não para a música aqui: ela deve continuar nas Configurações
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão concedida
            } else {
                // Permissão negada - o app ainda funcionará com armazenamento interno
            }
        }
    }

    // ── Permissões ───────────────────────────────────────────────────────────────

    private void checkAndRequestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ precisa de MANAGE_EXTERNAL_STORAGE para acesso completo
            if (!android.os.Environment.isExternalStorageManager()) {
                new AlertDialog.Builder(this)
                        .setTitle("Permissão de Armazenamento")
                        .setMessage("O Perfect Dark precisa de acesso ao armazenamento para salvar configurações, saves de jogo e a ROM. Os arquivos serão salvos na pasta 'perfect dark' no armazenamento interno.")
                        .setPositiveButton("Conceder", (dialog, which) -> {
                            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancelar", null)
                        .setCancelable(false)
                        .show();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-10 usa permissões de runtime normais
            String[] permissions = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };

            boolean needsPermission = false;
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    needsPermission = true;
                    break;
                }
            }

            if (needsPermission) {
                ActivityCompat.requestPermissions(this, permissions, STORAGE_PERMISSION_REQUEST_CODE);
            }
        }
    }

    // ── Listeners ──────────────────────────────────────────────────────────────

    private void setupListeners() {

        // INICIAR → para a música, depois vai ao LauncherActivity (verifica ROM)
        btnStart.setOnClickListener(v ->
            animateButton(v, () -> {
                stopMusicService(); // Música para ao entrar no jogo
                Intent intent = new Intent(MainMenuActivity.this, LauncherActivity.class);
                startActivity(intent);
            })
        );

        // CONFIGURAÇÕES → música continua tocando
        btnSettings.setOnClickListener(v ->
            animateButton(v, () -> {
                Intent intent = new Intent(MainMenuActivity.this, SettingsActivity.class);
                startActivity(intent);
            })
        );

        // SAIR → para a música e fecha o app
        btnExit.setOnClickListener(v ->
            new AlertDialog.Builder(this)
                    .setTitle("Sair")
                    .setMessage("Tem certeza que deseja sair?")
                    .setPositiveButton("Sim", (d, w) -> {
                        stopMusicService(); // Música para ao sair
                        finishAffinity();
                        System.exit(0);
                    })
                    .setNegativeButton("Não", null)
                    .show()
        );
    }

    // ── Música ─────────────────────────────────────────────────────────────────

    private void startMusicService() {
        Intent musicIntent = new Intent(this, MusicService.class);
        startService(musicIntent);
    }

    private void stopMusicService() {
        Intent musicIntent = new Intent(this, MusicService.class);
        stopService(musicIntent);
    }

    // ── Animações ──────────────────────────────────────────────────────────────

    private void animateEntrance() {
        View[] buttons = {btnStart, btnSettings, btnExit};
        long[] delays  = {200, 350, 500};
        for (int i = 0; i < buttons.length; i++) {
            View btn = buttons[i];
            long delay = delays[i];
            btn.setAlpha(0f);
            btn.setTranslationY(24f);
            btn.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(350)
                    .setStartDelay(delay)
                    .start();
        }
    }

    private void animateButton(View view, Runnable action) {
        view.animate()
                .scaleX(0.93f).scaleY(0.93f)
                .setDuration(90)
                .withEndAction(() ->
                    view.animate()
                            .scaleX(1f).scaleY(1f)
                            .setDuration(90)
                            .withEndAction(action)
                            .start()
                ).start();
    }
}
