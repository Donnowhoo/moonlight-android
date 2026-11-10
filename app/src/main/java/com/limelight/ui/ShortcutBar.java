package com.limelight.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.limelight.R;
import com.limelight.binding.input.ClipboardText;
import com.limelight.binding.input.ShortcutController;
import com.limelight.preferences.PreferenceConfiguration;
import java.util.HashMap;
import java.util.Map;

/** In-activity, nonfocusable overlay. It never requests a system overlay permission. */
public final class ShortcutBar extends LinearLayout {
    private final Activity activity;
    private final FrameLayout parent;
    private final ShortcutController controller;
    private final SharedPreferences preferences;
    private final PreferenceConfiguration config;
    private final Map<Integer, Button> modifiers = new HashMap<>();
    private final LinearLayout body;
    private final Button fold;
    private final Button progress;
    private final Button release;
    private boolean collapsed;
    private float positionX, positionY;
    private final ViewTreeObserver.OnGlobalLayoutListener layoutListener = this::place;
    private AlertDialog confirmation;
    private ClipboardText confirmedText;

    public ShortcutBar(Activity activity, FrameLayout parent, ShortcutController controller,
                       PreferenceConfiguration config, Runnable keyboard) {
        super(activity);
        this.activity = activity; this.parent = parent; this.controller = controller; this.config = config;
        preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        collapsed = preferences.getBoolean("shortcut_bar_collapsed", false);
        positionX = preferences.getFloat("shortcut_bar_x", 0.05f);
        positionY = preferences.getFloat("shortcut_bar_y", 0.9f);
        positionX = finitePosition(positionX, 0.05f);
        positionY = finitePosition(positionY, 0.9f);
        setOrientation(HORIZONTAL);
        setClickable(true);
        setFocusable(false); setFocusableInTouchMode(false);
        setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        setBackgroundColor(Color.rgb(30, 33, 38));
        setAlpha(config.shortcutBarOpacity / 100f);
        Button handle = button("↕", R.string.shortcut_move);
        addView(handle);
        handle.setOnTouchListener(new View.OnTouchListener() {
            private float x, y, startX, startY;
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        x = event.getRawX(); y = event.getRawY(); startX = getX(); startY = getY();
                        controller.stopRepeat(); return true;
                    case MotionEvent.ACTION_MOVE:
                        Rect area = area();
                        positionX = ratio(startX + event.getRawX() - x - area.left, area.width() - getWidth());
                        positionY = ratio(startY + event.getRawY() - y - area.top, area.height() - getHeight());
                        place(); return true;
                    case MotionEvent.ACTION_UP:
                        save(); view.performClick(); return true;
                    case MotionEvent.ACTION_CANCEL: save(); return true;
                    default: return true;
                }
            }
        });
        body = new LinearLayout(activity); body.setOrientation(HORIZONTAL); body.setFocusable(false);
        fold = button(collapsed ? "▸" : "▾", R.string.shortcut_collapse);
        fold.setOnClickListener(v -> {
            controller.releaseAll();
            collapsed = !collapsed; body.setVisibility(collapsed ? GONE : VISIBLE);
            refresh();
            fold.setText(collapsed ? "▸" : "▾"); controller.stopRepeat(); save(); post(this::place);
        });
        addView(fold);
        release = button(getResources().getString(R.string.shortcut_release), R.string.shortcut_release);
        release.setOnClickListener(v -> controller.releaseAll()); addView(release);
        release.setVisibility(collapsed ? GONE : VISIBLE);
        HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setFocusable(false); scroll.setFillViewport(false);
        scroll.addView(body);
        addView(scroll, new LinearLayout.LayoutParams(0, dp(48), 1));
        body.setVisibility(collapsed ? GONE : VISIBLE);
        add(R.string.shortcut_keyboard, () -> { controller.userInput(); keyboard.run(); });
        if (group("edit")) {
            int[] labels = {R.string.shortcut_copy, R.string.shortcut_paste, R.string.shortcut_cut,
                    R.string.shortcut_undo, R.string.shortcut_redo, R.string.shortcut_select_all, R.string.shortcut_save};
            int[] keys = {0x43, 0x56, 0x58, 0x5A, 0x59, 0x41, 0x53};
            for (int i = 0; i < keys.length; i++) { final int key = keys[i]; add(labels[i], () -> controller.chord(0xA2, key)); }
        }
        if (group("modifiers")) {
            for (int key : new int[]{0xA2, 0xA0, 0xA4}) {
                Button b = button(modifierName(key), R.string.shortcut_modifier_help);
                b.setOnClickListener(v -> controller.cycle(key)); body.addView(b); modifiers.put(key, b);
            }
        }
        if (group("navigation")) {
            String[] labels = {"Esc", "Tab", "Enter", "Delete", "F2", "←", "↑", "↓", "→"};
            int[] keys = {0x1B, 0x09, 0x0D, 0x2E, 0x71, 0x25, 0x26, 0x28, 0x27};
            for (int i = 0; i < keys.length; i++) {
                final int key = keys[i]; Button b = button(labels[i], 0); body.addView(b);
                if (i < 5) b.setOnClickListener(v -> controller.tap(key));
                else b.setOnTouchListener((v, event) -> {
                    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                        v.getParent().requestDisallowInterceptTouchEvent(true); controller.repeat(key);
                    } else if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                        controller.stopRepeat(); v.getParent().requestDisallowInterceptTouchEvent(false);
                        if (event.getActionMasked() == MotionEvent.ACTION_UP) v.performClick();
                    }
                    return true;
                });
            }
        }
        if (group("korean")) {
            add(R.string.shortcut_hangul, () -> controller.tap(0x15));
            add(R.string.shortcut_hanja, () -> controller.tap(0x19));
        }
        if (group("system")) {
            Button altTab = button("Alt+Tab", 0); altTab.setOnClickListener(v -> controller.altTab()); body.addView(altTab);
            Button win = button("Win", 0); win.setOnClickListener(v -> controller.tap(0x5B)); body.addView(win);
            add(R.string.shortcut_right_click, controller::rightClick);
        }
        if (group("clipboard")) add(R.string.shortcut_type_clipboard, this::clipboard);
        add(R.string.shortcut_help_title, () -> {
            controller.releaseAll();
            new AlertDialog.Builder(activity).setTitle(R.string.shortcut_help_title)
                    .setMessage(R.string.shortcut_help).setPositiveButton(android.R.string.ok, null).show();
        });
        progress = button("", R.string.shortcut_cancel_typing);
        progress.setVisibility(GONE); progress.setOnClickListener(v -> controller.userInput()); addView(progress);
        parent.addView(this, new FrameLayout.LayoutParams(dp(720), dp(48)));
        parent.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
        post(this::place);
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN || event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
            controller.overlayTouch();
        }
        return super.dispatchTouchEvent(event);
    }
    private boolean group(String value) { return config.shortcutGroups.contains(value); }
    private Button button(String text, int description) {
        Button button = new Button(activity);
        button.setText(text); button.setTextSize(14); button.setTextColor(Color.WHITE);
        button.setAllCaps(false); button.setFocusable(false); button.setFocusableInTouchMode(false);
        button.setMinWidth(dp(48)); button.setMinimumWidth(dp(48));
        button.setMinHeight(dp(48)); button.setMinimumHeight(dp(48));
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, dp(48)));
        button.setContentDescription(description == 0 ? text : getResources().getString(description));
        return button;
    }
    private void add(int label, Runnable action) {
        Button b = button(getResources().getString(label), label); b.setOnClickListener(v -> action.run()); body.addView(b);
    }
    private static float ratio(float value, int size) { return size <= 0 ? 0 : Math.max(0, Math.min(1, value / size)); }
    private static float finitePosition(float value, float fallback) {
        return Float.isNaN(value) || Float.isInfinite(value) ? fallback : Math.max(0, Math.min(1, value));
    }
    private static String modifierName(int key) { return key == 0xA2 ? "Ctrl" : key == 0xA0 ? "Shift" : "Alt"; }
    public void refresh() {
        StringBuilder state = new StringBuilder();
        for (Map.Entry<Integer, Button> entry : modifiers.entrySet()) {
            int mode = controller.mode(entry.getKey());
            entry.getValue().setText(modifierName(entry.getKey()) + (mode == 1 ? " ①" : mode == 2 ? " 🔒" : ""));
            entry.getValue().setTextColor(mode == 0 ? Color.WHITE : Color.YELLOW);
            if (mode != 0) state.append(modifierName(entry.getKey())).append(mode == 1 ? "① " : "🔒 ");
        }
        release.setText(state.length() == 0 ? getResources().getString(R.string.shortcut_release) : state.toString());
        release.setVisibility(collapsed ? GONE : VISIBLE);
    }
    public void progress(int sent, int total) {
        progress.setVisibility(total != 0 && sent < total ? VISIBLE : GONE);
        progress.setText(getResources().getString(R.string.shortcut_typing_progress, sent, total));
    }
    private void save() {
        preferences.edit().putFloat("shortcut_bar_x", positionX).putFloat("shortcut_bar_y", positionY)
                .putBoolean("shortcut_bar_collapsed", collapsed).apply();
    }
    private Rect area() {
        Rect visible = new Rect(); parent.getWindowVisibleDisplayFrame(visible);
        int[] location = new int[2]; parent.getLocationOnScreen(location);
        visible.offset(-location[0], -location[1]);
        if (!visible.intersect(0, 0, parent.getWidth(), parent.getHeight()) || visible.isEmpty()) {
            visible.set(0, 0, parent.getWidth(), parent.getHeight());
        }
        return visible;
    }
    private void place() {
        if (parent.getWidth() == 0) return;
        Rect area = area();
        int width = Math.min(area.width(), collapsed ? dp(96) : dp(720));
        ViewGroup.LayoutParams params = getLayoutParams();
        if (params.width != width) { params.width = width; setLayoutParams(params); }
        setX(area.left + Math.max(0, area.width() - width) * positionX);
        setY(area.top + Math.max(0, area.height() - dp(48)) * positionY);
    }
    private void toast(int resource) { Toast.makeText(activity, resource, Toast.LENGTH_LONG).show(); }
    private void clipboard() {
        controller.userInput();
        ClipboardManager manager = (ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = manager.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0 || clip.getItemAt(0).getText() == null) {
            toast(R.string.shortcut_clipboard_empty); return;
        }
        final ClipboardText text;
        try { text = ClipboardText.parse(clip.getItemAt(0).getText().toString()); }
        catch (IllegalArgumentException e) { toast(R.string.shortcut_clipboard_invalid); return; }
        if (controller.modifiers() != 0) { toast(R.string.shortcut_release_modifiers); return; }
        if (text.multiline) {
            confirmation = new AlertDialog.Builder(activity).setTitle(R.string.shortcut_type_clipboard)
                    .setMessage(R.string.shortcut_multiline_confirm)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> confirmedText = text).create();
            confirmation.show();
        } else if (!controller.type(text)) toast(R.string.shortcut_release_modifiers);
    }
    /** Dialog focus must return before typing starts. Losing focus itself always cancels the queue. */
    public void focusRestored() {
        if (confirmedText != null) {
            ClipboardText text = confirmedText; confirmedText = null;
            if (!controller.type(text)) toast(R.string.shortcut_release_modifiers);
        }
    }
    public void hideForLifecycle() {
        confirmedText = null;
        if (confirmation != null) { confirmation.dismiss(); confirmation = null; }
        setVisibility(GONE);
    }
    public void dispose() {
        hideForLifecycle();
        parent.getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);
        parent.removeView(this);
    }
}
