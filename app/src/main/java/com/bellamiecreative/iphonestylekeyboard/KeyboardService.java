package com.bellamiecreative.iphonestylekeyboard;

import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class KeyboardService extends InputMethodService {
    private LinearLayout root;
    private boolean symbols = false;
    private boolean shifted = false;
    private boolean kurdish = false;

    private final String[][] EN = {
        {"q","w","e","r","t","y","u","i","o","p"},
        {"a","s","d","f","g","h","j","k","l"},
        {"shift","z","x","c","v","b","n","m","⌫"}
    };
    private final String[][] KU = {
        {"ض","ص","ث","ق","ف","غ","ع","ه","خ","ح"},
        {"ش","س","ی","ب","ل","ا","ت","ن","م"},
        {"shift","ک","گ","ڤ","ڕ","د","پ","ۆ","ژ","⌫"}
    };
    private final String[][] SYM = {
        {"1","2","3","4","5","6","7","8","9","0"},
        {"-","/",":",";","(",")","$","&","@","\""},
        {"#","%","*","+","=","!","?",",",".","⌫"}
    };

    @Override public View onCreateInputView() {
        buildKeyboard();
        return root;
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private void buildKeyboard() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(4), dp(5), dp(4), dp(5));
        root.setBackgroundColor(Color.rgb(220, 223, 228));

        String[][] rows = symbols ? SYM : (kurdish ? KU : EN);
        for (int r = 0; r < rows.length; r++) addRow(rows[r], r);
        addBottomRow();
    }

    private void addRow(String[] keys, int rowIndex) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(dp(rowIndex == 1 && !symbols ? 14 : 0), dp(2), dp(rowIndex == 1 && !symbols ? 14 : 0), dp(2));
        for (String key : keys) {
            Button b = makeKey(key);
            float weight = (key.equals("shift") || key.equals("⌫")) ? 1.45f : 1f;
            row.addView(b, new LinearLayout.LayoutParams(0, dp(43), weight));
        }
        root.addView(row, new LinearLayout.LayoutParams(-1, dp(48)));
    }

    private Button makeKey(String key) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(key.equals("shift") ? (shifted ? "⇧" : "⇧") : key);
        b.setTextSize(key.length() > 1 ? 15 : 21);
        b.setTextColor(Color.rgb(25,25,25));
        b.setGravity(Gravity.CENTER);
        b.setPadding(0,0,0,0);
        b.setMinHeight(0);
        b.setMinWidth(0);
        b.setBackgroundColor(Color.WHITE);
        b.setOnClickListener(v -> press(key));
        return b;
    }

    private void addBottomRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        String[] bottom = {symbols ? "ABC" : "123", "😊", "🌐", "space", "return"};
        float[] weights = {1.2f, 1.05f, 1.05f, 4.2f, 1.6f};
        for (int i=0;i<bottom.length;i++) {
            Button b = makeKey(bottom[i]);
            b.setTextSize(bottom[i].equals("space") ? 14 : 16);
            row.addView(b, new LinearLayout.LayoutParams(0, dp(45), weights[i]));
        }
        root.addView(row, new LinearLayout.LayoutParams(-1, dp(51)));
    }

    private void press(String key) {
        haptic();
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        if (key.equals("⌫")) {
            ic.deleteSurroundingText(1, 0);
        } else if (key.equals("shift")) {
            shifted = !shifted;
            buildKeyboard();
            setInputView(root);
        } else if (key.equals("123")) {
            symbols = true;
            buildKeyboard();
            setInputView(root);
        } else if (key.equals("ABC")) {
            symbols = false;
            buildKeyboard();
            setInputView(root);
        } else if (key.equals("🌐")) {
            kurdish = !kurdish;
            symbols = false;
            shifted = false;
            buildKeyboard();
            setInputView(root);
        } else if (key.equals("😊")) {
            ic.commitText("😊", 1);
        } else if (key.equals("space")) {
            ic.commitText(" ", 1);
        } else if (key.equals("return")) {
            ic.sendKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER));
            ic.sendKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER));
        } else {
            String out = key;
            if (!symbols && shifted) {
                out = key.toUpperCase(Locale.ROOT);
                shifted = false;
                buildKeyboard();
                setInputView(root);
            }
            ic.commitText(out, 1);
        }
    }

    private void haptic() {
        Vibrator v = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        if (v == null || !v.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE));
        else v.vibrate(12);
    }
}
