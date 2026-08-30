package com.perfectdark.port;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;


/**
 * MainMenuActivity — Tela inicial do Perfect Dark Android Port.
 * Inicia o MusicService (menu.mp3) ao entrar.
 * Para a música ao iniciar o jogo ou sair do app.
 * A música continua tocando ao abrir Configurações (serviço não é parado).
 */
public class MainMenuActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1001;

    private MaterialButton btnStart;
    private MaterialButton btnMultiplayer;
    private MaterialButton btnSettings;
    private MaterialButton btnExit;

    private static final String PREFS_NET = "NetplayPrefs";
    private static final String KEY_LAST_IP = "last_join_ip";
    private static final String KEY_LAST_PORT = "last_join_port";

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        btnStart       = findViewById(R.id.btnStart);
        btnMultiplayer = findViewById(R.id.btnMultiplayer);
        btnSettings    = findViewById(R.id.btnSettings);
        btnExit        = findViewById(R.id.btnExit);

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

        // MULTIPLAYER → abre opções de Host e Join
        btnMultiplayer.setOnClickListener(v ->
            animateButton(v, this::showMultiplayerDialog)
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

    // ── Multiplayer ─────────────────────────────────────────────────────────────

    private void showMultiplayerDialog() {
        String[] options = {"Criar Sala (Host Game)", "Entrar em Sala (Join Game)"};
        new AlertDialog.Builder(this)
                .setTitle("Multiplayer (Netplay)")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showHostDialog();
                    } else {
                        showJoinDialog();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showHostDialog() {
        String ipSummary = getDeviceIpSummary();
        String primaryIp = getPrimaryIpAddress();

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 10);

        // Título IP
        android.widget.TextView ipTitle = new android.widget.TextView(this);
        ipTitle.setText("Seu Endereço IP (Passe para os amigos entrarem):");
        ipTitle.setTextSize(13f);
        ipTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        ipTitle.setTextColor(0xFF00E5FF);
        layout.addView(ipTitle);

        // Bloco/Card com o IP detectado
        android.widget.TextView ipBox = new android.widget.TextView(this);
        ipBox.setText(ipSummary);
        ipBox.setTextSize(14f);
        ipBox.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        ipBox.setTextColor(0xFFFFFFFF);
        ipBox.setBackgroundColor(0xFF222222);
        ipBox.setPadding(24, 18, 24, 18);
        android.widget.LinearLayout.LayoutParams boxParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        boxParams.setMargins(0, 10, 0, 12);
        ipBox.setLayoutParams(boxParams);
        layout.addView(ipBox);

        // Botão para Copiar IP
        com.google.android.material.button.MaterialButton btnCopyIp = new com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle);
        btnCopyIp.setText("📋 Copiar IP (" + primaryIp + ")");
        btnCopyIp.setTextColor(0xFFFFD700);
        btnCopyIp.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("IP Perfect Dark", primaryIp);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "IP " + primaryIp + " copiado para a área de transferência!", Toast.LENGTH_SHORT).show();
            }
        });
        layout.addView(btnCopyIp);

        // Porta
        android.widget.TextView portLabel = new android.widget.TextView(this);
        portLabel.setText("Porta UDP do Servidor:");
        portLabel.setPadding(0, 16, 0, 0);
        layout.addView(portLabel);

        android.widget.EditText inputPort = new android.widget.EditText(this);
        inputPort.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        inputPort.setText("27100");
        layout.addView(inputPort);

        // Dica explicativa
        android.widget.TextView hintText = new android.widget.TextView(this);
        hintText.setText("💡 Dica: Seus amigos devem estar conectados no mesmo Wi-Fi ou Ponto de Acesso (Hotspot) do seu celular e inserir este IP na opção 'Entrar em Sala'.");
        hintText.setTextSize(12f);
        hintText.setTextColor(0xFFAAAAAA);
        hintText.setPadding(0, 12, 0, 8);
        layout.addView(hintText);

        new AlertDialog.Builder(this)
                .setTitle("Criar Sala (Host)")
                .setView(layout)
                .setPositiveButton("Iniciar Sala", (dialog, which) -> {
                    int port = 27100;
                    try {
                        port = Integer.parseInt(inputPort.getText().toString().trim());
                    } catch (Exception ignored) {}
                    launchGameWithNet(1, null, port);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private String getDeviceIpSummary() {
        StringBuilder sb = new StringBuilder();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                String name = iface.getName().toLowerCase();
                String displayName = iface.getDisplayName().toLowerCase();

                String label = "Rede Local";
                if (name.contains("wlan") || displayName.contains("wlan") || displayName.contains("wi-fi")) {
                    label = "Wi-Fi";
                } else if (name.contains("ap") || name.contains("rndis") || name.contains("tether")) {
                    label = "Ponto de Acesso (Hotspot)";
                } else if (name.contains("eth")) {
                    label = "Ethernet";
                } else if (name.contains("tun") || name.contains("zt") || name.contains("tailscale")) {
                    label = "VPN / ZeroTier";
                }

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        if (sb.length() > 0) sb.append("\n");
                        sb.append(label).append(" (").append(iface.getName()).append("): ").append(addr.getHostAddress());
                    }
                }
            }
        } catch (Exception ignored) {}

        if (sb.length() == 0) {
            return "127.0.0.1 (Sem conexão Wi-Fi/Hotspot ativa)";
        }
        return sb.toString();
    }

    private String getPrimaryIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }

    private void showJoinDialog() {
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NET, MODE_PRIVATE);
        String lastIp = prefs.getString(KEY_LAST_IP, "127.0.0.1");
        int lastPort = prefs.getInt(KEY_LAST_PORT, 27100);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        android.widget.TextView ipLabel = new android.widget.TextView(this);
        ipLabel.setText("Endereço IP / Hostname do Servidor:");
        layout.addView(ipLabel);

        android.widget.EditText inputIp = new android.widget.EditText(this);
        inputIp.setText(lastIp);
        inputIp.setHint("Ex: 192.168.1.100 ou meu-servidor.net");
        layout.addView(inputIp);

        android.widget.TextView portLabel = new android.widget.TextView(this);
        portLabel.setText("Porta UDP:");
        portLabel.setPadding(0, 20, 0, 0);
        layout.addView(portLabel);

        android.widget.EditText inputPort = new android.widget.EditText(this);
        inputPort.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        inputPort.setText(String.valueOf(lastPort));
        layout.addView(inputPort);

        new AlertDialog.Builder(this)
                .setTitle("Entrar em Sala (Join)")
                .setView(layout)
                .setPositiveButton("Conectar", (dialog, which) -> {
                    String ip = inputIp.getText().toString().trim();
                    int port = 27100;
                    try {
                        port = Integer.parseInt(inputPort.getText().toString().trim());
                    } catch (Exception ignored) {}

                    if (ip.isEmpty()) {
                        ip = "127.0.0.1";
                    }

                    // Salva último IP e Porta
                    prefs.edit()
                            .putString(KEY_LAST_IP, ip)
                            .putInt(KEY_LAST_PORT, port)
                            .apply();

                    launchGameWithNet(2, ip, port);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void launchGameWithNet(int mode, String ip, int port) {
        stopMusicService();
        Intent intent = new Intent(MainMenuActivity.this, LauncherActivity.class);
        intent.putExtra("extra_net_mode", mode);
        if (ip != null) {
            intent.putExtra("extra_net_ip", ip);
        }
        intent.putExtra("extra_net_port", port);
        startActivity(intent);
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
        View[] buttons = {btnStart, btnMultiplayer, btnSettings, btnExit};
        long[] delays  = {200, 300, 400, 500};
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
