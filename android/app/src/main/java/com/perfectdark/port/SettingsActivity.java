package com.perfectdark.port;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.perfectdark.port.R;

public class SettingsActivity extends Activity {
    private static final String PREFS_NAME = "PerfectDarkSettings";
    
    // Video settings keys
    private static final String KEY_FULLSCREEN = "fullscreen";
    private static final String KEY_VSYNC = "vsync";
    private static final String KEY_DISPLAY_FPS = "display_fps";
    private static final String KEY_DETAIL_TEXTURES = "detail_textures";
    private static final String KEY_TEX_FILTER_2D = "tex_filter_2d";
    private static final String KEY_FRAMERATE_LIMIT = "framerate_limit";
    private static final String KEY_SCREEN_SHAKE = "screen_shake";
    
    // Audio settings keys
    private static final String KEY_DISABLE_MP_DEATH_MUSIC = "disable_mp_death_music";
    
    // Game settings keys
    private static final String KEY_UNCAP_TICKRATE = "uncap_tickrate";
    private static final String KEY_GE_MUZZLE_FLASHES = "ge_muzzle_flashes";
    private static final String KEY_FIELD_OF_VIEW = "field_of_view";
    private static final String KEY_HUD_CENTER = "hud_center";
    
    // Controls settings keys
    public static final String KEY_CONTROL_STYLE = "control_style";
    private static final String KEY_SHOW_CONTROLS = "show_controls";
    private static final String KEY_LEFT_STICK_SENSITIVITY = "left_stick_sensitivity";
    private static final String KEY_RIGHT_STICK_SENSITIVITY = "right_stick_sensitivity";
    private static final String KEY_LEFT_STICK_DEADZONE = "left_stick_deadzone";
    private static final String KEY_RIGHT_STICK_DEADZONE = "right_stick_deadzone";
    private static final String KEY_VIBRATION = "vibration";
    private static final String KEY_VIBRATION_STRENGTH = "vibration_strength";

    // Control style constants (matching constants.h)
    public static final int CONTROL_STYLE_1_1 = 0;
    public static final int CONTROL_STYLE_1_2 = 1;
    public static final int CONTROL_STYLE_1_3 = 2;
    public static final int CONTROL_STYLE_1_4 = 3;
    public static final int CONTROL_STYLE_2_1 = 4;
    public static final int CONTROL_STYLE_2_2 = 5;
    public static final int CONTROL_STYLE_2_3 = 6;
    public static final int CONTROL_STYLE_2_4 = 7;
    public static final int CONTROL_STYLE_PC  = 8; // Port / Modern Extended

    private static final int[] CONTROL_STYLE_VALUES = {
        CONTROL_STYLE_PC,
        CONTROL_STYLE_1_1,
        CONTROL_STYLE_1_2,
        CONTROL_STYLE_1_3,
        CONTROL_STYLE_1_4,
        CONTROL_STYLE_2_1,
        CONTROL_STYLE_2_2,
        CONTROL_STYLE_2_3,
        CONTROL_STYLE_2_4
    };

    private static final String[] CONTROL_STYLE_NAMES = {
        "Port / Moderno (Dois Analógicos - Recomendado)",
        "Clássico 1.1 (Padrão N64 - 1 Analógico)",
        "Clássico 1.2 (Manual N64 - 1 Analógico)",
        "Clássico 1.3 (N64 1.3)",
        "Clássico 1.4 (N64 1.4)",
        "Clássico 2.1 (Dois Controles 2.1)",
        "Clássico 2.2 (Dois Controles 2.2)",
        "Clássico 2.3 (Dois Controles 2.3)",
        "Clássico 2.4 (Dois Controles 2.4)"
    };

    private static final String[] CONTROL_STYLE_DESCRIPTIONS = {
        "Modo Port: movimentação moderna e fluida com dois analógicos e mira precisa.",
        "Modo Clássico 1.1: analógico move para frente/trás e gira a câmera. Botões C fazem strafe e olham para cima/baixo.",
        "Modo Clássico 1.2: analógico mira/gira e olha. Botões C movem e fazem strafe.",
        "Modo Clássico 1.3: variação clássica N64 invertida.",
        "Modo Clássico 1.4: variação clássica N64 alternativa.",
        "Modo Clássico 2.1: dois analógicos estilo N64 (Controle 1 move/gira, Controle 2 mira/strafe).",
        "Modo Clássico 2.2: dois analógicos estilo N64 (Controle 1 mira, Controle 2 move/strafe).",
        "Modo Clássico 2.3: dois analógicos estilo N64 alternativo.",
        "Modo Clássico 2.4: dois analógicos estilo N64 alternativo."
    };

    // Video settings UI components
    private CheckBox checkFullscreen;
    private CheckBox checkVsync;
    private CheckBox checkDisplayFPS;
    private CheckBox checkDetailTextures;
    private CheckBox checkTexFilter2D;
    private SeekBar seekFramerateLimit;
    private SeekBar seekScreenShake;
    
    // Audio settings UI components
    private CheckBox checkDisableMpDeathMusic;
    
    // Game settings UI components
    private CheckBox checkUncapTickrate;
    private CheckBox checkGeMuzzleFlashes;
    private SeekBar seekFieldOfView;
    private SeekBar seekHudCenter;
    
    // Controls settings UI components
    private Spinner spinnerControlStyle;
    private TextView textControlStyleDescription;
    private CheckBox checkShowControls;
    private SeekBar seekLeftStickSensitivity;
    private SeekBar seekRightStickSensitivity;
    private SeekBar seekLeftStickDeadzone;
    private SeekBar seekRightStickDeadzone;
    private CheckBox checkVibration;
    private SeekBar seekVibrationStrength;
    
    private Button btnSave;
    private Button btnCancel;

    private SharedPreferences prefs;

    // Native method declarations
    public native void nativeApplySettings(
        boolean fullscreen, boolean vsync, boolean displayFPS, boolean detailTextures, boolean texFilter2D,
        int framerateLimit, float screenShake,
        boolean disableMpDeathMusic,
        boolean uncapTickrate, boolean geMuzzleFlashes, int fieldOfView, int hudCenter,
        boolean showControls, float leftStickSensitivity, float rightStickSensitivity,
        float leftStickDeadzone, float rightStickDeadzone, boolean vibration, float vibrationStrength,
        boolean texDumpEnabled, boolean texLoadEnabled,
        int controlStyle
    );

    static {
        System.loadLibrary("pd");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        initViews();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        // Video settings
        checkFullscreen = findViewById(R.id.checkFullscreen);
        checkVsync = findViewById(R.id.checkVsync);
        checkDisplayFPS = findViewById(R.id.checkDisplayFPS);
        checkDetailTextures = findViewById(R.id.checkDetailTextures);
        checkTexFilter2D = findViewById(R.id.checkTexFilter2D);
        seekFramerateLimit = findViewById(R.id.seekFramerateLimit);
        seekScreenShake = findViewById(R.id.seekScreenShake);
        
        // Audio settings
        checkDisableMpDeathMusic = findViewById(R.id.checkDisableMpDeathMusic);
        
        // Game settings
        checkUncapTickrate = findViewById(R.id.checkUncapTickrate);
        checkGeMuzzleFlashes = findViewById(R.id.checkGeMuzzleFlashes);
        seekFieldOfView = findViewById(R.id.seekFieldOfView);
        seekHudCenter = findViewById(R.id.seekHudCenter);
        
        // Controls settings
        spinnerControlStyle = findViewById(R.id.spinnerControlStyle);
        textControlStyleDescription = findViewById(R.id.textControlStyleDescription);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            R.layout.item_control_style_spinner,
            CONTROL_STYLE_NAMES
        );
        adapter.setDropDownViewResource(R.layout.item_control_style_dropdown);
        spinnerControlStyle.setAdapter(adapter);

        checkShowControls = findViewById(R.id.checkShowControls);
        seekLeftStickSensitivity = findViewById(R.id.seekLeftStickSensitivity);
        seekRightStickSensitivity = findViewById(R.id.seekRightStickSensitivity);
        seekLeftStickDeadzone = findViewById(R.id.seekLeftStickDeadzone);
        seekRightStickDeadzone = findViewById(R.id.seekRightStickDeadzone);
        checkVibration = findViewById(R.id.checkVibration);
        seekVibrationStrength = findViewById(R.id.seekVibrationStrength);
        
        // Buttons
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void loadSettings() {
        // Video settings
        checkFullscreen.setChecked(prefs.getBoolean(KEY_FULLSCREEN, true));
        checkVsync.setChecked(prefs.getBoolean(KEY_VSYNC, true));
        checkDisplayFPS.setChecked(prefs.getBoolean(KEY_DISPLAY_FPS, false));
        checkDetailTextures.setChecked(prefs.getBoolean(KEY_DETAIL_TEXTURES, true));
        checkTexFilter2D.setChecked(prefs.getBoolean(KEY_TEX_FILTER_2D, true));
        seekFramerateLimit.setProgress(prefs.getInt(KEY_FRAMERATE_LIMIT, 60));
        seekScreenShake.setProgress(prefs.getInt(KEY_SCREEN_SHAKE, 10));
        
        // Audio settings
        checkDisableMpDeathMusic.setChecked(prefs.getBoolean(KEY_DISABLE_MP_DEATH_MUSIC, false));
        
        // Game settings
        checkUncapTickrate.setChecked(prefs.getBoolean(KEY_UNCAP_TICKRATE, false));
        checkGeMuzzleFlashes.setChecked(prefs.getBoolean(KEY_GE_MUZZLE_FLASHES, false));
        seekFieldOfView.setProgress(prefs.getInt(KEY_FIELD_OF_VIEW, 60));
        seekHudCenter.setProgress(prefs.getInt(KEY_HUD_CENTER, 0));
        
        // Controls settings - Estilo de Controle
        int savedStyle = prefs.getInt(KEY_CONTROL_STYLE, CONTROL_STYLE_PC);
        int selectedIndex = 0;
        for (int i = 0; i < CONTROL_STYLE_VALUES.length; i++) {
            if (CONTROL_STYLE_VALUES[i] == savedStyle) {
                selectedIndex = i;
                break;
            }
        }
        spinnerControlStyle.setSelection(selectedIndex);
        if (textControlStyleDescription != null && selectedIndex < CONTROL_STYLE_DESCRIPTIONS.length) {
            textControlStyleDescription.setText(CONTROL_STYLE_DESCRIPTIONS[selectedIndex]);
        }

        checkShowControls.setChecked(prefs.getBoolean(KEY_SHOW_CONTROLS, true));
        seekLeftStickSensitivity.setProgress(prefs.getInt(KEY_LEFT_STICK_SENSITIVITY, 50));
        seekRightStickSensitivity.setProgress(prefs.getInt(KEY_RIGHT_STICK_SENSITIVITY, 50));
        seekLeftStickDeadzone.setProgress(prefs.getInt(KEY_LEFT_STICK_DEADZONE, 10));
        seekRightStickDeadzone.setProgress(prefs.getInt(KEY_RIGHT_STICK_DEADZONE, 10));
        checkVibration.setChecked(prefs.getBoolean(KEY_VIBRATION, true));
        seekVibrationStrength.setProgress(prefs.getInt(KEY_VIBRATION_STRENGTH, 5));
    }

    private void setupListeners() {
        spinnerControlStyle.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < CONTROL_STYLE_DESCRIPTIONS.length) {
                    textControlStyleDescription.setText(CONTROL_STYLE_DESCRIPTIONS[position]);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
                applySettingsToNative();
                Toast.makeText(SettingsActivity.this, "Configurações salvas e aplicadas!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = prefs.edit();
        
        // Video settings
        editor.putBoolean(KEY_FULLSCREEN, checkFullscreen.isChecked());
        editor.putBoolean(KEY_VSYNC, checkVsync.isChecked());
        editor.putBoolean(KEY_DISPLAY_FPS, checkDisplayFPS.isChecked());
        editor.putBoolean(KEY_DETAIL_TEXTURES, checkDetailTextures.isChecked());
        editor.putBoolean(KEY_TEX_FILTER_2D, checkTexFilter2D.isChecked());
        editor.putInt(KEY_FRAMERATE_LIMIT, seekFramerateLimit.getProgress());
        editor.putInt(KEY_SCREEN_SHAKE, seekScreenShake.getProgress());
        
        // Audio settings
        editor.putBoolean(KEY_DISABLE_MP_DEATH_MUSIC, checkDisableMpDeathMusic.isChecked());
        
        // Game settings
        editor.putBoolean(KEY_UNCAP_TICKRATE, checkUncapTickrate.isChecked());
        editor.putBoolean(KEY_GE_MUZZLE_FLASHES, checkGeMuzzleFlashes.isChecked());
        editor.putInt(KEY_FIELD_OF_VIEW, seekFieldOfView.getProgress());
        editor.putInt(KEY_HUD_CENTER, seekHudCenter.getProgress());
        
        // Controls settings - Estilo de Controle
        int selectedPosition = spinnerControlStyle.getSelectedItemPosition();
        int styleValue = CONTROL_STYLE_PC;
        if (selectedPosition >= 0 && selectedPosition < CONTROL_STYLE_VALUES.length) {
            styleValue = CONTROL_STYLE_VALUES[selectedPosition];
        }
        editor.putInt(KEY_CONTROL_STYLE, styleValue);

        editor.putBoolean(KEY_SHOW_CONTROLS, checkShowControls.isChecked());
        editor.putInt(KEY_LEFT_STICK_SENSITIVITY, seekLeftStickSensitivity.getProgress());
        editor.putInt(KEY_RIGHT_STICK_SENSITIVITY, seekRightStickSensitivity.getProgress());
        editor.putInt(KEY_LEFT_STICK_DEADZONE, seekLeftStickDeadzone.getProgress());
        editor.putInt(KEY_RIGHT_STICK_DEADZONE, seekRightStickDeadzone.getProgress());
        editor.putBoolean(KEY_VIBRATION, checkVibration.isChecked());
        editor.putInt(KEY_VIBRATION_STRENGTH, seekVibrationStrength.getProgress());
        
        editor.apply();
    }
    
    private void applySettingsToNative() {
        // Convert SeekBar values to appropriate float/int values for native code
        float screenShakeValue = seekScreenShake.getProgress() / 10.0f;
        
        int selectedPosition = spinnerControlStyle.getSelectedItemPosition();
        int styleValue = CONTROL_STYLE_PC;
        if (selectedPosition >= 0 && selectedPosition < CONTROL_STYLE_VALUES.length) {
            styleValue = CONTROL_STYLE_VALUES[selectedPosition];
        }

        nativeApplySettings(
            checkFullscreen.isChecked(),
            checkVsync.isChecked(),
            checkDisplayFPS.isChecked(),
            checkDetailTextures.isChecked(),
            checkTexFilter2D.isChecked(),
            seekFramerateLimit.getProgress(),
            screenShakeValue,
            checkDisableMpDeathMusic.isChecked(),
            checkUncapTickrate.isChecked(),
            checkGeMuzzleFlashes.isChecked(),
            seekFieldOfView.getProgress(),
            seekHudCenter.getProgress(),
            true,  // showControls - placeholder
            1.0f,  // leftStickSensitivity - placeholder
            1.0f,  // rightStickSensitivity - placeholder
            0.0f,  // leftStickDeadzone - placeholder
            0.0f,  // rightStickDeadzone - placeholder
            true,  // vibration - placeholder
            0.5f,  // vibrationStrength - placeholder
            false, // texDumpEnabled
            false, // texLoadEnabled
            styleValue
        );
    }

    // Static getter methods for other activities
    public static int getControlStyle(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_CONTROL_STYLE, CONTROL_STYLE_PC);
    }

    public static boolean getShowControls(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_SHOW_CONTROLS, true);
    }

    public static int getLeftStickSensitivity(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_LEFT_STICK_SENSITIVITY, 50);
    }

    public static int getRightStickSensitivity(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_RIGHT_STICK_SENSITIVITY, 50);
    }

    public static boolean getVibration(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_VIBRATION, true);
    }
}
