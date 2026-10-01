package com.bellamiecreative.iphonestylekeyboard;

import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.Locale;

public class KeyboardService extends InputMethodService {
    private LinearLayout root;
    private boolean symbols = false;
    private boolean shifted = false;
    private boolean kurdish = false;
    private boolean capsLock = false;
    private PopupWindow emojiPopup;
    private PopupWindow longPressPopup;

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

    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        shifted = false;
        capsLock = false;
        symbols = false;
    }

    private int dp(float v) {
        return (int)(v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private boolean darkMode() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    private int rootColor() { return darkMode() ? Color.rgb(20,20,22) : Color.rgb(218,220,224); }
    private int keyColor() { return darkMode() ? Color.rgb(58,58,60) : Color.WHITE; }
    private int keyTextColor() { return darkMode() ? Color.WHITE : Color.rgb(25,25,27); }
    private int actionKeyColor() { return darkMode() ? Color.rgb(78,78,80) : Color.rgb(174,179,185); }

    private void buildKeyboard() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(4), dp(5), dp(4), dp(4));
        root.setBackgroundColor(rootColor());
        addHintRow();

        String[][] rows = symbols ? SYM : (kurdish ? KU : EN);
        for (int r = 0; r < rows.length; r++) addRow(rows[r], r);
        addBottomRow();
    }

    private void addHintRow() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), 0, dp(8), 0);
        TextView status = new TextView(this);
        status.setText(symbols ? "123  •  #+=  •  ABC" : (kurdish ? "کوردی" : "English"));
        status.setTextSize(12);
        status.setTextColor(darkMode() ? Color.rgb(205,205,210) : Color.rgb(90,90,95));
        status.setGravity(Gravity.CENTER);
        row.addView(status, new LinearLayout.LayoutParams(0, dp(22), 1f));
        root.addView(row, new LinearLayout.LayoutParams(-1, dp(24)));
    }

    private void addRow(String[] keys, int rowIndex) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        int side = (!symbols && rowIndex == 1) ? 15 : 0;
        row.setPadding(dp(side), dp(2), dp(side), dp(2));
        for (String key : keys) {
            Button b = makeKey(key);
            float weight = (key.equals("shift") || key.equals("⌫")) ? 1.38f : 1f;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(42), weight);
            lp.setMargins(dp(2),0,dp(2),0);
            row.addView(b, lp);
        }
        root.addView(row, new LinearLayout.LayoutParams(-1, dp(47)));
    }

    private Button makeKey(String key) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(key.equals("shift") ? "⇧" : key);
        b.setTextSize(key.length() > 1 ? 15 : 21);
        b.setTextColor(key.equals("shift") && shifted ? Color.rgb(0,122,255) : keyTextColor());
        b.setGravity(Gravity.CENTER);
        b.setPadding(0,0,0,0);
        b.setMinHeight(0);
        b.setMinWidth(0);
        b.setIncludeFontPadding(false);
        b.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        b.setBackground(makeKeyBackground(key));
        b.setOnClickListener(v -> press(key));
        if (!key.equals("⌫") && !key.equals("shift") && !key.equals("space")
                && !key.equals("🌐") && !key.equals("😊") && key.length() == 1) {
            b.setOnLongClickListener(v -> { showLongPress(key, b); return true; });
        }
        return b;
    }

    private GradientDrawable makeKeyBackground(String key) {
        boolean action = key.equals("shift") || key.equals("⌫");
        boolean bottom = key.equals("123") || key.equals("ABC") || key.equals("😊")
                || key.equals("🌐") || key.equals("space") || key.equals("return")
                || key.equals("done") || key.equals("go") || key.equals("next")
                || key.equals("search") || key.equals("send");
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(6));
        bg.setColor(action || bottom ? actionKeyColor() : keyColor());
        if (!darkMode() && !action && !bottom) bg.setStroke(dp(0.5f), Color.rgb(198,200,204));
        return bg;
    }

    private void addBottomRow() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0,dp(2),0,0);
        String[] bottom = {symbols ? "ABC" : "123", "😊", "🌐", "space", editorActionLabel()};
        float[] weights = {1.15f,0.95f,0.95f,4.2f,1.55f};
        for (int i=0;i<bottom.length;i++) {
            Button b = makeKey(bottom[i]);
            b.setTextSize(bottom[i].equals("space") ? 14 : 15);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,dp(43),weights[i]);
            lp.setMargins(dp(2),0,dp(2),0);
            row.addView(b,lp);
        }
        root.addView(row,new LinearLayout.LayoutParams(-1,dp(48)));
    }

    private String editorActionLabel() {
        EditorInfo info = getCurrentInputEditorInfo();
        if (info == null) return "return";
        int a = info.imeOptions & EditorInfo.IME_MASK_ACTION;
        if (a == EditorInfo.IME_ACTION_DONE) return "done";
        if (a == EditorInfo.IME_ACTION_GO) return "go";
        if (a == EditorInfo.IME_ACTION_NEXT) return "next";
        if (a == EditorInfo.IME_ACTION_SEARCH) return "search";
        if (a == EditorInfo.IME_ACTION_SEND) return "send";
        return "return";
    }

    private void press(String key) {
        haptic();
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        try {
            if (key.equals("⌫")) {
                CharSequence selected = ic.getSelectedText(0);
                if (selected != null && selected.length() > 0) ic.commitText("",1);
                else ic.deleteSurroundingText(1,0);
                return;
            }
            if (key.equals("shift")) {
            if (shifted && !capsLock) capsLock = true;
            else if (capsLock) capsLock = false;
            else shifted = true;
            rebuild(); return;
        }
            if (key.equals("123")) { symbols=true; shifted=false; rebuild(); return; }
            if (key.equals("ABC")) { symbols=false; shifted=false; rebuild(); return; }
            if (key.equals("🌐")) { kurdish=!kurdish; symbols=false; shifted=false; capsLock=false; rebuild(); return; }
            if (key.equals("😊")) { showEmojiPopup(); return; }
            if (key.equals("space")) { ic.commitText(" ",1); return; }

            if (key.equals("return") || key.equals("done") || key.equals("go") || key.equals("next")
                    || key.equals("search") || key.equals("send")) {
                int action = getEditorAction();
                if (action != EditorInfo.IME_ACTION_NONE) ic.performEditorAction(action);
                else sendEnter(ic);
                return;
            }

            String out = key;
            if (!symbols && !kurdish && shifted) {
                out = key.toUpperCase(Locale.ROOT);
                shifted=false;
                rebuild();
            }
            ic.commitText(out,1);
        } catch (RuntimeException ignored) {
            // Never let one bad editor connection crash the keyboard service.
        }
    }

    private int getEditorAction() {
        EditorInfo info = getCurrentInputEditorInfo();
        if (info == null) return EditorInfo.IME_ACTION_NONE;
        int a = info.imeOptions & EditorInfo.IME_MASK_ACTION;
        return a == EditorInfo.IME_ACTION_NONE ? EditorInfo.IME_ACTION_NONE : a;
    }

    private void sendEnter(InputConnection ic) {
        try {
            ic.sendKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER));
            ic.sendKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER));
        } catch (RuntimeException ignored) {
        }
    }

    private void rebuild() {
        buildKeyboard();
        setInputView(root);
    }


    private void showEmojiPopup() {
        if (root == null) return;
        final String[] emojis = {"😀","😂","🤣","😊","😍","🥰","😘","😎","😭","😡","👍","👎","👏","🙏","❤️","🔥","🎉","✨","💯","✅","❌","⭐","🌹","☀️","🌙","🍎","🍕","☕","⚽","🎵","🚗","✈️","🏠","💙","💚","💛","💜","🤍","🤝","🙌","💪","👀"};
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(8), dp(8), dp(8), dp(8));
        box.setBackgroundColor(darkMode() ? Color.rgb(35,35,38) : Color.WHITE);
        LinearLayout row = null;
        int count = 0;
        for (String emoji : emojis) {
            if (count % 7 == 0) {
                row = new LinearLayout(this);
                row.setGravity(Gravity.CENTER);
                box.addView(row, new LinearLayout.LayoutParams(-1, dp(48)));
            }
            Button b = new Button(this);
            b.setText(emoji);
            b.setTextSize(22);
            b.setAllCaps(false);
            b.setPadding(0,0,0,0);
            b.setMinHeight(0);
            b.setMinWidth(0);
            b.setBackgroundColor(Color.TRANSPARENT);
            final String value = emoji;
            b.setOnClickListener(v -> {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    try { ic.commitText(value, 1); } catch (RuntimeException ignored) {}
                }
                if (emojiPopup != null) emojiPopup.dismiss();
            });
            row.addView(b, new LinearLayout.LayoutParams(0, dp(46), 1f));
            count++;
        }
        emojiPopup = new PopupWindow(box, -1, dp(260), true);
        emojiPopup.setOutsideTouchable(true);
        emojiPopup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        emojiPopup.setElevation(dp(8));
        try {
            emojiPopup.showAtLocation(root, Gravity.BOTTOM, 0, dp(235));
        } catch (RuntimeException ignored) {
            emojiPopup.dismiss();
        }
    }

    private void showLongPress(String key, Button anchor) {
        String alternatives = null;
        switch (key) {
            case "a": alternatives = "áàäâãå"; break;
            case "e": alternatives = "éèëê"; break;
            case "i": alternatives = "íìïî"; break;
            case "o": alternatives = "óòöôõ"; break;
            case "u": alternatives = "úùüû"; break;
            case "n": alternatives = "ñ"; break;
            case "c": alternatives = "ç"; break;
            case "s": alternatives = "ß"; break;
            case "y": alternatives = "ýÿ"; break;
            case ".": alternatives = "…"; break;
            case "-": alternatives = "–—"; break;
            default: return;
        }
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setPadding(dp(6), dp(4), dp(6), dp(4));
        box.setBackgroundColor(darkMode() ? Color.rgb(45,45,48) : Color.WHITE);
        for (int i = 0; i < alternatives.length(); i++) {
            String alt = String.valueOf(alternatives.charAt(i));
            Button b = makeKey(alt);
            b.setTextSize(20);
            b.setOnLongClickListener(null);
            final String value = alt;
            b.setOnClickListener(v -> {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    try { ic.commitText(value, 1); } catch (RuntimeException ignored) {}
                }
                if (longPressPopup != null) longPressPopup.dismiss();
            });
            box.addView(b, new LinearLayout.LayoutParams(dp(48), dp(48)));
        }
        longPressPopup = new PopupWindow(box, -2, dp(56), true);
        longPressPopup.setOutsideTouchable(true);
        longPressPopup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        longPressPopup.setElevation(dp(8));
        try {
            int[] loc = new int[2];
            anchor.getLocationOnScreen(loc);
            longPressPopup.showAtLocation(root, Gravity.TOP | Gravity.START, Math.max(0, loc[0] - dp(10)), Math.max(0, loc[1] - dp(60)));
        } catch (RuntimeException ignored) {
            longPressPopup.dismiss();
        }
    }

    private void haptic() {
        try {
            Vibrator v = (Vibrator)getSystemService(VIBRATOR_SERVICE);
            if (v == null || !v.hasVibrator()) return;
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createOneShot(10,VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(10);
            }
        } catch (SecurityException ignored) {
            // Vibration is optional; typing must continue if unavailable.
        } catch (RuntimeException ignored) {
        }
    }
}
