package com.perfectdark.port;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

/**
 * MainMenuActivity — Tela inicial do Perfect Dark Android Port.
 * Inicia o MusicService (menu.mp3) ao entrar.
 * Para a música ao iniciar o jogo ou sair do app.
 * A música continua tocando ao abrir Configurações (serviço não é parado).
 */
public class MainMenuActivity extends AppCompatActivity {

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
