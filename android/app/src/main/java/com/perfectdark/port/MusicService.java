package com.perfectdark.port;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;

import androidx.annotation.Nullable;

/**
 * MusicService — Toca menu.mp3 em loop no fundo.
 * Persiste entre MainMenuActivity e SettingsActivity.
 * Para automaticamente quando o serviço é destruído (startGame / sair).
 */
public class MusicService extends Service {

    private MediaPlayer mediaPlayer;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.menu);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.setVolume(1.0f, 1.0f);
                mediaPlayer.start();
            }
        } else if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
        // START_NOT_STICKY: não recriar se o sistema matar (não é essencial)
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Não é um bound service
    }
}
