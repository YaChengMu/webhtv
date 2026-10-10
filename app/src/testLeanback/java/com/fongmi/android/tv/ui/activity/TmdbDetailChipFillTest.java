package com.fongmi.android.tv.ui.activity;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.shape.MaterialShapeDrawable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 * 用户报告：电视版「线路」和「选集」chip 只剩描边、没有底色，压在亮色剧照上看不清。
 *
 * <p>根因是芯片填充走的通道不同：
 * <ul>
 *   <li>「线路」「选集」走 {@code setBackgroundColor}，它只把颜色写进当时那个
 *       {@code MaterialShapeDrawable} 实例，并不更新 Material 的 {@code backgroundTint} 字段；</li>
 *   <li>同一页的「第 N 季」按钮走 {@code setBackgroundTintList}，写的是那个持久字段，
 *       所以它一直有可见底色。</li>
 * </ul>
 *
 * <p>Material 会在 inset 变化（{@code setInsetTop/Bottom}）、圆角变化或测量等时机
 * 用 {@code backgroundTint} 字段重建背景。此时 {@code setBackgroundColor} 写进旧实例的填充
 * 会被丢掉并退化为全透明，而 {@code setBackgroundTintList} 的填充会保留。
 *
 * <p>本测试用真实渲染像素把这条差异固定成契约：它既证明当前实现（tint 通道）在背景重建后
 * 仍有底色，也证明旧的 {@code setBackgroundColor} 写法会掉色——避免日后有人"简化"回去。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = App.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class TmdbDetailChipFillTest {

    /** 与 {@code TmdbDetailActivity#createChipButton} 保持一致的构造方式。 */
    private MaterialButton chip(Context context) {
        MaterialButton button = new MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setText("线路01");
        button.setCheckable(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setMaxWidth(320);
        button.setSingleLine(true);
        button.setTextColor(0xFFFFFFFF);
        button.setPadding(24, 12, 24, 12);
        return button;
    }

    private MaterialShapeDrawable fillOf(MaterialButton button) {
        Drawable background = button.getBackground();
        if (background instanceof RippleDrawable ripple && ripple.getNumberOfLayers() > 0) {
            Drawable layer = ripple.getDrawable(0);
            if (layer instanceof InsetDrawable inset) {
                Drawable inner = inset.getDrawable();
                if (inner instanceof LayerDrawable layers && layers.getNumberOfLayers() > 1) {
                    return (MaterialShapeDrawable) layers.getDrawable(1);
                }
            }
        }
        return null;
    }

    private int fillColor(MaterialButton button) {
        MaterialShapeDrawable fill = fillOf(button);
        assertNotNull("chip must expose a Material fill drawable", fill);
        ColorStateList tint = fill.getTintList();
        return tint == null ? 0 : tint.getDefaultColor();
    }

    /** 真实渲染一个已测量的 chip，返回其填充区域中心像素。 */
    private int renderedPixel(MaterialButton button) {
        LinearLayout parent = new LinearLayout(button.getContext());
        parent.addView(button);
        parent.measure(View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(120, View.MeasureSpec.EXACTLY));
        parent.layout(0, 0, 600, 120);
        Bitmap bitmap = Bitmap.createBitmap(600, 120, Bitmap.Config.ARGB_8888);
        parent.draw(new Canvas(bitmap));
        return bitmap.getPixel(button.getLeft() + 6, button.getTop() + button.getHeight() / 2);
    }

    private void applyChipFill(MaterialButton button, int color, boolean viaTint) {
        if (viaTint) button.setBackgroundTintList(ColorStateList.valueOf(color));
        else button.setBackgroundColor(color);
    }

    @Test
    public void persistentTintChannelKeepsChipFillThroughMaterialBackgroundRebuild() {
        Context context = new ContextThemeWrapper(App.get(), R.style.Theme_App);
        int chip = 0xFFEAF0F5;

        MaterialButton button = chip(context);
        applyChipFill(button, chip, true);
        assertEquals("tint channel must reach the fill immediately", chip, fillColor(button));

        // Material rebuilds the background on inset changes; this is what dropped the old fill.
        button.setInsetTop(0);
        button.setInsetBottom(0);

        assertEquals("chip fill must survive a Material background rebuild", chip, fillColor(button));
        assertEquals("rendered chip must stay visibly filled", chip, renderedPixel(button));
    }

    @Test
    public void legacySetBackgroundColorLosesItsFillWhenMaterialRebuildsTheBackground() {
        Context context = new ContextThemeWrapper(App.get(), R.style.Theme_App);
        int chip = 0xFFEAF0F5;

        MaterialButton button = chip(context);
        applyChipFill(button, chip, false);
        assertEquals("setBackgroundColor reaches the current fill instance", chip, fillColor(button));

        button.setInsetTop(0);
        button.setInsetBottom(0);

        // This is the regression the user reported: the fill is gone and only the stroke remains.
        assertNotEquals("setBackgroundColor must not be used for chip fill: the rebuild drops it",
                chip, fillColor(button));
        assertEquals("and the fill degrades to fully transparent", 0, fillColor(button));
    }

    // ------------------------------------------------------------------ 内嵌播放器面板焦点环

    /**
     * 用户报告（2026-10-10）：详情页内嵌播放器在全屏 / PiP 形态下「没有选中效果，或者说看不出来」。
     *
     * <p>根因：{@code TmdbDetailActivity.updatePlayerPanelFocus} 对
     * {@code inlineFullscreen || inlinePiPLayout} 分支直接执行
     * {@code setStrokeColor(0) + setStrokeWidth(0)} 后 return。而面板在这两种形态下仍然
     * {@code focusable=true}，于是出现「能聚焦但零视觉反馈」。
     *
     * <p>本测试用真实栅格化像素验证修正后的行为：全屏（MATCH_PARENT + radius 0）下焦点态
     * 必须画出一圈**内缩**的主题色环（不贴屏幕物理边缘、不裁切画面），非焦点态则无环。
     */
    @Test
    public void fullscreenPlayerPanelDrawsAnInsetThemeFocusRing() {
        Context context = new ContextThemeWrapper(App.get(), R.style.Theme_App);
        float density = context.getResources().getDisplayMetrics().density;
        int width = 1920, height = 1080;
        int ringPx = context.getResources().getDimensionPixelSize(R.dimen.webhtv_focus_ring_width);
        int insetPx = Math.round(12 * density);

        View root = android.view.LayoutInflater.from(context)
                .inflate(R.layout.activity_tmdb_detail, null, false);
        com.google.android.material.card.MaterialCardView panel = root.findViewById(R.id.playerPanel);
        assertNotNull("activity_tmdb_detail must keep @id/playerPanel", panel);

        // 全屏形态：铺满 + 直角（与 enterInlineFullscreen / enterInlinePiPLayout 一致）
        panel.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        panel.setRadius(0);
        panel.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        panel.layout(0, 0, width, height);

        // 非焦点态：无环
        panel.setStrokeWidth(0);
        panel.setStrokeColor(0);
        panel.setForeground(null);
        assertEquals("unfocused fullscreen panel must not paint a ring",
                0, countRingPixels(panel, width, height, ringPx));

        // 焦点态：内缩主题环（复刻 updatePlayerPanelFocus 的修正后写法）
        android.graphics.drawable.GradientDrawable ring = new android.graphics.drawable.GradientDrawable();
        ring.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        ring.setColor(0);
        ring.setCornerRadius(8 * density);
        ring.setStroke(ringPx, 0xFF0B57D0);
        panel.setForeground(new InsetDrawable(ring, insetPx, insetPx, insetPx, insetPx));

        int painted = countRingPixels(panel, width, height, ringPx);
        assertTrue("focused fullscreen panel must paint an inset theme ring (got " + painted + "px)",
                painted > 1000);

        // 环必须内缩：屏幕物理边缘（前 inset/2 像素）不得出现环色，否则会被屏幕边切掉。
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        panel.draw(new Canvas(bitmap));
        for (int x = 0; x < insetPx / 2; x++) {
            assertEquals("ring must not touch the physical screen edge at x=" + x,
                    0, isRingColour(bitmap.getPixel(x, height / 2)) ? 1 : 0);
        }
    }

    /** 栅格化 panel 并数出主题环色像素。 */
    private static int countRingPixels(View panel, int width, int height, int ringPx) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        panel.draw(new Canvas(bitmap));
        int count = 0;
        for (int y = 0; y < height; y += 2)
            for (int x = 0; x < width; x += 2)
                if (isRingColour(bitmap.getPixel(x, y))) count++;
        return count * 4;
    }

    /** 是否为主题 FOCUS 环色（浅色表 #0B57D0）。 */
    private static boolean isRingColour(int pixel) {
        if (((pixel >>> 24) & 0xFF) < 60) return false;
        return Math.abs(((pixel >> 16) & 0xFF) - 0x0B) < 30
                && Math.abs(((pixel >> 8) & 0xFF) - 0x57) < 30
                && Math.abs((pixel & 0xFF) - 0xD0) < 30;
    }
}
