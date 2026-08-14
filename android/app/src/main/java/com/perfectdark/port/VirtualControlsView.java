package com.perfectdark.port;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

/**
 * VirtualControlsView — controles virtuais completos para o Perfect Dark Android port.
 *
 * Layout:
 *  - Analógico esquerdo dinâmico (floating): movimentação do personagem
 *  - Botão A, B: ações
 *  - Botão L, R: shoulder buttons (topo)
 *  - Botão Z: gatilho / mira (meio direita)
 *  - Botão Start: pausa / menu
 *
 * Mapeamento JNI:
 *   nativeStickInput(0, x, y) → analógico esquerdo (movimento)
 *   nativeButtonDown/Up(0) → A
 *   nativeButtonDown/Up(1) → B
 *   nativeButtonDown/Up(2) → Z
 *   nativeButtonDown/Up(3) → L
 *   nativeButtonDown/Up(4) → R
 *   nativeButtonDown/Up(5) → Start
 */
public class VirtualControlsView extends View {

    // ────────────────────────────────────────────────────────────────────────
    //  Constants
    // ────────────────────────────────────────────────────────────────────────

    // Button IDs (para nativeButtonDown/Up)
    private static final int BTN_A     = 0;
    private static final int BTN_B     = 1;
    private static final int BTN_Z     = 2;
    private static final int BTN_L     = 3;
    private static final int BTN_R     = 4;
    private static final int BTN_START = 5;

    // Stick IDs
    private static final int STICK_LEFT  = 0;

    // Visual alpha dos controles (0-255)
    private static final int CONTROLS_ALPHA = 160;
    private static final int PRESSED_ALPHA  = 230;

    // Cores
    private static final int COLOR_OUTER      = 0x60FFFFFF;  // anel externo
    private static final int COLOR_KNOB       = 0xA0AAAAAA;  // knob
    private static final int COLOR_BTN_NORMAL = 0x00000000;  // fundo botão (transparente)
    private static final int COLOR_BTN_PRESS  = 0xB0003366;  // fundo pressionado
    private static final int COLOR_BTN_BORDER = 0xA0FFFFFF;  // borda botão
    private static final int COLOR_TEXT       = 0xFFFFFFFF;  // texto botão
    private static final int COLOR_SHOULDER   = 0x70333333;  // fundo L/R

    // ────────────────────────────────────────────────────────────────────────
    //  Paint objects
    // ────────────────────────────────────────────────────────────────────────
    private final Paint paintOuter  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintKnob   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintBtn    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintText   = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ────────────────────────────────────────────────────────────────────────
    //  Analógico esquerdo (floating)
    // ────────────────────────────────────────────────────────────────────────
    private boolean stickActive     = false;
    private int     stickPointerId  = -1;
    private float   stickCenterX, stickCenterY;  // centro atual (pixels)
    private float   stickKnobX, stickKnobY;      // posição do knob (pixels)
    private float   stickOuterRadius;             // raio do anel externo
    private float   stickMaxRadius;              // raio máximo do knob

    // Zona onde um toque inicia o analógico: lado esquerdo da tela
    // (< 40% da largura)
    private static final float STICK_ZONE_WIDTH_RATIO = 0.40f;

    // ────────────────────────────────────────────────────────────────────────
    //  Sensibilidade
    // ────────────────────────────────────────────────────────────────────────
    private float   leftStickSensitivity = 1.0f;  // multiplicador do analógico esquerdo (lido das configurações)

    // ────────────────────────────────────────────────────────────────────────
    //  Botões — definidos em onSizeChanged
    // ────────────────────────────────────────────────────────────────────────
    private static final int NUM_BUTTONS = 6;
    private final float[]   btnCX      = new float[NUM_BUTTONS];
    private final float[]   btnCY      = new float[NUM_BUTTONS];
    private final float[]   btnRadius  = new float[NUM_BUTTONS];
    private final boolean[] btnPressed = new boolean[NUM_BUTTONS];
    private final int[]     btnPointer = new int[NUM_BUTTONS];
    private final String[]  btnLabel   = {"A", "B", "Z", "L", "R", "START"};

    // Shoulder buttons (L/R) são retangulares
    private final RectF rectL = new RectF();
    private final RectF rectR = new RectF();

    // ────────────────────────────────────────────────────────────────────────
    //  Dimensões calculadas em onSizeChanged
    // ────────────────────────────────────────────────────────────────────────
    private int   viewW, viewH;
    private float dp;  // 1dp em pixels

    // ────────────────────────────────────────────────────────────────────────
    //  Delegate JNI — usa TouchControls que já está vinculada à libpd.so
    // ────────────────────────────────────────────────────────────────────────
    private TouchControls touchDelegate;

    // ────────────────────────────────────────────────────────────────────────
    //  Constructor
    // ────────────────────────────────────────────────────────────────────────
    public VirtualControlsView(Context context) {
        super(context);
        init();
    }

    public VirtualControlsView(Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VirtualControlsView(Context context, android.util.AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        dp = getContext().getResources().getDisplayMetrics().density;
        touchDelegate = new TouchControls(getContext());

        // Carrega configurações de sensibilidade
        loadSensitivitySettings();

        // Importante: interceptar toques antes do SDL
        setFocusable(true);
        setFocusableInTouchMode(true);

        // Pintura do anel externo do analógico
        paintOuter.setColor(COLOR_OUTER);
        paintOuter.setStyle(Paint.Style.STROKE);
        paintOuter.setStrokeWidth(3 * dp);

        // Pintura do knob
        paintKnob.setColor(COLOR_KNOB);
        paintKnob.setStyle(Paint.Style.FILL);

        // Pintura dos botões (fill)
        paintBtn.setStyle(Paint.Style.FILL);

        // Borda dos botões
        paintBorder.setColor(COLOR_BTN_BORDER);
        paintBorder.setStyle(Paint.Style.STROKE);
        paintBorder.setStrokeWidth(2 * dp);

        // Texto dos botões
        paintText.setColor(COLOR_TEXT);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));

        // Inicializa pointer IDs dos botões
        for (int i = 0; i < NUM_BUTTONS; i++) {
            btnPointer[i] = -1;
        }

        // View não opaca — precisa de fundo transparente
        setBackgroundColor(Color.TRANSPARENT);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Layout — calcula posições dos controles
    // ────────────────────────────────────────────────────────────────────────
    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        viewW = w;
        viewH = h;
        layoutControls();
    }

    private void layoutControls() {
        float w = viewW;
        float h = viewH;

        // ── Analógico: raios
        stickOuterRadius = Math.min(w, h) * 0.095f;
        stickMaxRadius   = stickOuterRadius * 0.55f;

        // Centro default do stick (usado apenas para referência visual quando inativo)
        stickCenterX = w * 0.14f;
        stickCenterY = h * 0.78f;
        stickKnobX   = stickCenterX;
        stickKnobY   = stickCenterY;

        // ── Botões A e B (canto inferior direito)
        float btnR   = Math.min(w, h) * 0.065f;
        float btnA_X = w * 0.88f;
        float btnA_Y = h * 0.68f;
        float btnB_X = w * 0.78f;
        float btnB_Y = h * 0.76f;

        btnCX[BTN_A] = btnA_X;  btnCY[BTN_A] = btnA_Y;  btnRadius[BTN_A] = btnR;
        btnCX[BTN_B] = btnB_X;  btnCY[BTN_B] = btnB_Y;  btnRadius[BTN_B] = btnR;

        // ── Botão Z (gatilho - meio direita)
        float btnZ_X = w * 0.92f;
        float btnZ_Y = h * 0.50f;
        float btnZR  = btnR * 0.90f;
        btnCX[BTN_Z] = btnZ_X;  btnCY[BTN_Z] = btnZ_Y;  btnRadius[BTN_Z] = btnZR;

        // ── Botão Start (centro-baixo)
        float startR = btnR * 0.70f;
        btnCX[BTN_START] = w * 0.50f;
        btnCY[BTN_START] = h * 0.88f;
        btnRadius[BTN_START] = startR;

        // ── Shoulder L (topo esquerdo)
        float shW = w * 0.14f;
        float shH = dp * 36f;
        float shY = dp * 8f;
        rectL.set(dp * 8f, shY, dp * 8f + shW, shY + shH);

        // ── Shoulder R (topo direito)
        rectR.set(w - dp * 8f - shW, shY, w - dp * 8f, shY + shH);

        // Centros dos shoulder (para pointer tracking)
        btnCX[BTN_L] = rectL.centerX();  btnCY[BTN_L] = rectL.centerY();
        btnCX[BTN_R] = rectR.centerX();  btnCY[BTN_R] = rectR.centerY();
        btnRadius[BTN_L] = 0;  // usa rect
        btnRadius[BTN_R] = 0;

        // Tamanho do texto adaptado
        paintText.setTextSize(btnR * 0.65f);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Desenho
    // ────────────────────────────────────────────────────────────────────────
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Analógico
        drawAnalogStick(canvas);

        // Botões circulares: A, B, Z, Start
        drawCircleButton(canvas, BTN_A);
        drawCircleButton(canvas, BTN_B);
        drawCircleButton(canvas, BTN_Z);
        drawCircleButton(canvas, BTN_START);

        // Shoulder buttons retangulares: L e R
        drawShoulderButton(canvas, BTN_L, rectL);
        drawShoulderButton(canvas, BTN_R, rectR);
    }

    private void drawAnalogStick(Canvas canvas) {
        float cx = stickActive ? stickCenterX : viewW * 0.14f;
        float cy = stickActive ? stickCenterY : viewH * 0.78f;
        float kx = stickActive ? stickKnobX : cx;
        float ky = stickActive ? stickKnobY : cy;

        // Anel externo
        paintOuter.setAlpha(stickActive ? PRESSED_ALPHA : CONTROLS_ALPHA);
        canvas.drawCircle(cx, cy, stickOuterRadius, paintOuter);

        // Anel interno (guia)
        paintOuter.setAlpha((stickActive ? PRESSED_ALPHA : CONTROLS_ALPHA) / 2);
        canvas.drawCircle(cx, cy, stickOuterRadius * 0.5f, paintOuter);

        // Knob
        paintKnob.setAlpha(stickActive ? PRESSED_ALPHA : CONTROLS_ALPHA);
        canvas.drawCircle(kx, ky, stickOuterRadius * 0.38f, paintKnob);
    }

    private void drawCircleButton(Canvas canvas, int id) {
        float cx = btnCX[id];
        float cy = btnCY[id];
        float r  = btnRadius[id];
        boolean pressed = btnPressed[id];

        // Fundo
        paintBtn.setColor(pressed ? COLOR_BTN_PRESS : COLOR_BTN_NORMAL);
        paintBtn.setAlpha(pressed ? PRESSED_ALPHA : CONTROLS_ALPHA);
        canvas.drawCircle(cx, cy, r, paintBtn);

        // Borda
        paintBorder.setAlpha(pressed ? PRESSED_ALPHA : CONTROLS_ALPHA);
        canvas.drawCircle(cx, cy, r, paintBorder);

        // Label
        paintText.setAlpha(CONTROLS_ALPHA + 60);
        // Ajuste de tamanho para START
        float origSize = paintText.getTextSize();
        if (id == BTN_START) {
            paintText.setTextSize(r * 0.90f);
        }
        // Centralizar verticalmente
        float textY = cy - (paintText.descent() + paintText.ascent()) / 2f;
        canvas.drawText(btnLabel[id], cx, textY, paintText);
        if (id == BTN_START) {
            paintText.setTextSize(origSize);
        }
    }

    private void drawShoulderButton(Canvas canvas, int id, RectF rect) {
        boolean pressed = btnPressed[id];
        float   corner  = dp * 8f;

        // Fundo
        paintBtn.setColor(pressed ? COLOR_BTN_PRESS : COLOR_SHOULDER);
        paintBtn.setAlpha(pressed ? PRESSED_ALPHA : CONTROLS_ALPHA);
        canvas.drawRoundRect(rect, corner, corner, paintBtn);

        // Borda
        paintBorder.setAlpha(pressed ? PRESSED_ALPHA : CONTROLS_ALPHA);
        canvas.drawRoundRect(rect, corner, corner, paintBorder);

        // Label
        paintText.setAlpha(CONTROLS_ALPHA + 60);
        float origSize = paintText.getTextSize();
        paintText.setTextSize(dp * 16f);
        float textY = rect.centerY() - (paintText.descent() + paintText.ascent()) / 2f;
        canvas.drawText(btnLabel[id], rect.centerX(), textY, paintText);
        paintText.setTextSize(origSize);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Processamento de Touch
    // ────────────────────────────────────────────────────────────────────────
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action      = event.getActionMasked();
        int pIndex      = event.getActionIndex();
        int pId         = event.getPointerId(pIndex);
        float px        = event.getX(pIndex);
        float py        = event.getY(pIndex);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                handleDown(px, py, pId);
                break;

            case MotionEvent.ACTION_MOVE:
                handleMove(event);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                handleUp(pId);
                break;

            case MotionEvent.ACTION_CANCEL:
                resetAll();
                break;
        }

        invalidate();
        return true;
    }

    private void handleDown(float x, float y, int pId) {
        android.util.Log.d("VirtualControls", "handleDown: x=" + x + " y=" + y + " pId=" + pId);
        
        // 1) Verifica shoulder L
        if (rectL.contains(x, y) && btnPointer[BTN_L] == -1) {
            pressButton(BTN_L, pId);
            return;
        }
        // 2) Verifica shoulder R
        if (rectR.contains(x, y) && btnPointer[BTN_R] == -1) {
            pressButton(BTN_R, pId);
            return;
        }
        // 3) Verifica botões circulares (A, B, Z, Start)
        for (int id : new int[]{BTN_A, BTN_B, BTN_Z, BTN_START}) {
            if (btnPointer[id] == -1 && isInCircle(x, y, btnCX[id], btnCY[id], btnRadius[id])) {
                pressButton(id, pId);
                return;
            }
        }
        // 4) Analógico esquerdo — zona esquerda da tela (excluindo área do botão L)
        if (x < viewW * STICK_ZONE_WIDTH_RATIO && !stickActive && !rectL.contains(x, y)) {
            stickActive    = true;
            stickPointerId = pId;
            stickCenterX   = x;
            stickCenterY   = y;
            stickKnobX     = x;
            stickKnobY     = y;
            nativeStickInput(STICK_LEFT, 0, 0);
            return;
        }
    }

    private void handleMove(MotionEvent event) {
        for (int i = 0; i < event.getPointerCount(); i++) {
            int   pId = event.getPointerId(i);
            float x   = event.getX(i);
            float y   = event.getY(i);

            // Ignora pointers que estão em botões (evita ativar analógico acidentalmente)
            boolean isButtonPointer = false;
            for (int id = 0; id < NUM_BUTTONS; id++) {
                if (btnPointer[id] == pId) {
                    isButtonPointer = true;
                    break;
                }
            }
            if (isButtonPointer) {
                continue;
            }

            // Analógico esquerdo
            if (pId == stickPointerId && stickActive) {
                float dx  = x - stickCenterX;
                float dy  = y - stickCenterY;
                float len = (float) Math.sqrt(dx * dx + dy * dy);

                if (len > stickOuterRadius) {
                    dx = dx / len * stickOuterRadius;
                    dy = dy / len * stickOuterRadius;
                }
                stickKnobX = stickCenterX + dx;
                stickKnobY = stickCenterY + dy;

                float nx = dx / stickOuterRadius * leftStickSensitivity;
                float ny = -dy / stickOuterRadius * leftStickSensitivity;  // Inverte Y (Android Y+ é para baixo, jogo espera Y+ para cima)
                nativeStickInput(STICK_LEFT, nx, ny);
            }

            // Botões pressionados: verificar se o dedo saiu da área (drag release)
            for (int id : new int[]{BTN_A, BTN_B, BTN_Z, BTN_START}) {
                if (btnPointer[id] == pId) {
                    boolean still = isInCircle(x, y, btnCX[id], btnCY[id], btnRadius[id] * 1.3f);
                    if (!still) {
                        releaseButton(id);
                    }
                }
            }
            if (btnPointer[BTN_L] == pId && !rectL.contains(x, y)) {
                releaseButton(BTN_L);
            }
            if (btnPointer[BTN_R] == pId && !rectR.contains(x, y)) {
                releaseButton(BTN_R);
            }
        }
    }

    private void handleUp(int pId) {
        // Analógico
        if (pId == stickPointerId) {
            stickActive    = false;
            stickPointerId = -1;
            stickKnobX     = stickCenterX;
            stickKnobY     = stickCenterY;
            nativeStickInput(STICK_LEFT, 0, 0);
        }

        // Botões
        for (int id = 0; id < NUM_BUTTONS; id++) {
            if (btnPointer[id] == pId) {
                releaseButton(id);
            }
        }
    }

    private void pressButton(int id, int pId) {
        btnPressed[id] = true;
        btnPointer[id] = pId;
        nativeButtonDown(id);
    }

    private void releaseButton(int id) {
        if (btnPressed[id]) {
            btnPressed[id] = false;
            btnPointer[id] = -1;
            nativeButtonUp(id);
        }
    }

    private void resetAll() {
        stickActive    = false;
        stickPointerId = -1;
        nativeStickInput(STICK_LEFT, 0, 0);

        for (int id = 0; id < NUM_BUTTONS; id++) {
            releaseButton(id);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Utilitários
    // ────────────────────────────────────────────────────────────────────────
    private boolean isInCircle(float x, float y, float cx, float cy, float r) {
        float dx = x - cx;
        float dy = y - cy;
        return dx * dx + dy * dy <= r * r;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  JNI — delegado para TouchControls (JNI binding: Java_com_perfectdark_port_TouchControls_*)
    // ────────────────────────────────────────────────────────────────────────
    private void nativeStickInput(int stick, float x, float y) {
        touchDelegate.nativeStickInput(stick, x, y);
    }

    private void nativeButtonDown(int button) {
        touchDelegate.nativeButtonDown(button);
    }

    private void nativeButtonUp(int button) {
        touchDelegate.nativeButtonUp(button);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Carregar configurações de sensibilidade
    // ────────────────────────────────────────────────────────────────────────
    private void loadSensitivitySettings() {
        int leftSens = SettingsActivity.getLeftStickSensitivity(getContext());
        
        // Converte valores (0-100) para multiplicadores (0.0-2.0)
        leftStickSensitivity = leftSens / 50.0f;
        
        android.util.Log.d("VirtualControls", "Sensibilidade carregada: left=" + leftStickSensitivity);
    }

    public void reloadSensitivitySettings() {
        loadSensitivitySettings();
    }
}
