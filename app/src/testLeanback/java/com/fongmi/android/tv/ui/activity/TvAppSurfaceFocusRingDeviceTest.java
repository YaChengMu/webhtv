package com.fongmi.android.tv.ui.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;

import androidx.appcompat.view.ContextThemeWrapper;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

/**
 * 电视端「应用表面」焦点环的像素级证据（Robolectric 真实栅格化）。
 *
 * <p>用户报告（两张实机截图）：
 * <blockquote>TV端上面两行按钮是 蓝色边框，下面的是 白色边框，边框粗细也貌似不一样，
 * 请统一风格并受到主题色彩控制</blockquote>
 *
 * <p>静态契约测试（{@code TvFocusRingContractTest}）只能证明 XML 文本写对了。本测试把
 * drawable 真正栅格化到 {@link Bitmap}，再逐像素量出**实际**的描边厚度与颜色——也就是
 * 用户肉眼看到的那两条边框。因此它能抓出「XML 看着对、渲染出来其实不同」的情况，
 * 例如 1.5dp 与 3dp 在不同密度下取整到不同像素数。
 *
 * <p>断言分三条：
 * <ol>
 *   <li>首页两族焦点环（功能按钮 vs 内容卡片）的实测厚度与颜色必须字节相同；</li>
 *   <li>全部应用表面焦点环的实测厚度必须等于 {@code @dimen/webhtv_focus_ring_width}；</li>
 *   <li>环色必须等于主题 FOCUS 槽，并在浅色/深色两张色板下各自跟随变化（真正的主题可控性）。</li>
 * </ol>
 *
 * <p>说明：{@code ?attr/tvFocusRing} 是编译期静态资源，用户档案覆写在运行期由
 * {@code ThemeBinder} 作用于视图树（另由 {@code TvFocusRingResolutionDeviceTest} 覆盖）。
 * 本测试证明的是「环色来自主题色板」——同一 drawable 在 day / night 两张表下解析出不同颜色。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = App.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class TvAppSurfaceFocusRingDeviceTest {

    /** 首页功能按钮行（搜索/历史）：adapter_func -> selector_item -> shape_item_focused。 */
    private static final int BUTTON_SELECTOR = R.drawable.selector_item;
    /** 首页内容卡片行（最近观看/更新推荐）：adapter_vod -> selector_vod -> shape_vod_focused。 */
    private static final int CARD_SELECTOR = R.drawable.selector_vod;

    /** 全部「应用表面」焦点环（宿主为调色板表面或对话框面板）。 */
    private static final int[] APP_SURFACE_RINGS = {
            R.drawable.selector_item,
            R.drawable.selector_item_round,
            R.drawable.selector_vod,
            R.drawable.selector_vod_oval,
            R.drawable.selector_keyboard,
            R.drawable.selector_search_hot_word,
    };

    /** 渲染尺寸：足够大，四条边都不会被裁掉。 */
    private static final int SIZE = 120;

    /** 判定「这个像素属于环」的 alpha 门槛（排除抗锯齿边缘的半透明过渡）。 */
    private static final int OPAQUE = 200;

    @After
    public void tearDown() {
        RuntimeEnvironment.setQualifiers("");
    }

    private static Context themedContext() {
        return new ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_App);
    }

    /** 取 selector 的 focused 分支（这些 selector 的 state_focused 都是第一项）。 */
    private static Drawable focusedDrawable(Context context, int resId) {
        Drawable drawable = context.getDrawable(resId);
        assertTrue(resId + " 必须能解析出 drawable", drawable != null);
        if (drawable instanceof StateListDrawable states) {
            states.setState(new int[]{android.R.attr.state_focused});
            Drawable current = states.getCurrent();
            assertTrue(resId + " 的 focused 状态必须解析出一个 drawable", current != null);
            return current;
        }
        return drawable;
    }

    /** 把 focused 分支栅格化。 */
    private static Bitmap render(Context context, int resId) {
        Drawable drawable = focusedDrawable(context, resId);
        Bitmap bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, SIZE, SIZE);
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * 实测环的厚度（像素）：从图像中部左侧起，数连续的不透明像素个数。
     * 矩形/圆形的左边框中点都是竖直的直线段，所以这是一段干净的游程。
     */
    private static int ringThicknessPx(Bitmap bitmap) {
        int y = SIZE / 2;
        int x = 0;
        while (x < SIZE && alpha(bitmap, x, y) < OPAQUE) x++;
        int run = 0;
        while (x < SIZE && alpha(bitmap, x, y) >= OPAQUE) {
            run++;
            x++;
        }
        assertTrue("中部左侧必须能找到一段不透明的焦点环（实际为 0）", run > 0);
        return run;
    }

    /** 实测环的颜色：取环游程中点那个像素。 */
    private static int ringColour(Bitmap bitmap) {
        int y = SIZE / 2;
        int x = 0;
        while (x < SIZE && alpha(bitmap, x, y) < OPAQUE) x++;
        int run = 0;
        int start = x;
        while (x < SIZE && alpha(bitmap, x, y) >= OPAQUE) {
            run++;
            x++;
        }
        assertTrue("中部左侧必须能找到一段不透明的焦点环（实际为 0）", run > 0);
        return bitmap.getPixel(start + run / 2, y);
    }

    private static int alpha(Bitmap bitmap, int x, int y) {
        return (bitmap.getPixel(x, y) >>> 24) & 0xFF;
    }

    private static int expectedWidthPx(Context context) {
        return context.getResources().getDimensionPixelSize(R.dimen.webhtv_focus_ring_width);
    }

    /** 主题 FOCUS 槽：{@code ?attr/tvFocusRing} 的取值来源。 */
    private static int focusSlot(Context context) {
        TypedValue value = new TypedValue();
        assertTrue("主题必须声明 tvFocusRing",
                context.getTheme().resolveAttribute(R.attr.tvFocusRing, value, true));
        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return value.data;
        }
        return context.getColor(value.resourceId);
    }

    private static String name(Context context, int resId) {
        return context.getResources().getResourceEntryName(resId);
    }

    // ------------------------------------------------------------------ R1 两族同款

    /**
     * 用户报告的核心：首页上面两行按钮是蓝框、下面的内容是白框，粗细也不同。
     * 两族实测的厚度与颜色必须完全一致。
     */
    @Test
    public void homeFunctionButtonsAndContentCardsRenderTheSameRing() {
        Context context = themedContext();
        Bitmap button = render(context, BUTTON_SELECTOR);
        Bitmap card = render(context, CARD_SELECTOR);

        assertEquals("首页功能按钮与内容卡片的焦点环厚度必须相同（用户报告粗细不一样）",
                ringThicknessPx(button), ringThicknessPx(card));
        assertEquals("首页功能按钮与内容卡片的焦点环颜色必须相同（用户报告蓝色/白色两种）",
                ringColour(button), ringColour(card));
    }

    @Test
    public void everyAppSurfaceRingRendersAtTheSingleWidthToken() {
        Context context = themedContext();
        int expected = expectedWidthPx(context);
        for (int resId : APP_SURFACE_RINGS) {
            int measured = ringThicknessPx(render(context, resId));
            assertTrue(name(context, resId) + " 的实测焦点环厚度 " + measured
                            + "px 必须等于 @dimen/webhtv_focus_ring_width (" + expected + "px)",
                    Math.abs(measured - expected) <= 1);
        }
    }

    // ------------------------------------------------------------------ R2 主题可控

    @Test
    public void appSurfaceRingsRenderInTheThemeFocusColour() {
        Context context = themedContext();
        int slot = focusSlot(context);
        assertEquals("焦点环色必须来自 webhtv_color_focus，而不是别的角色",
                context.getColor(R.color.webhtv_color_focus), slot);

        for (int resId : new int[]{BUTTON_SELECTOR, CARD_SELECTOR}) {
            assertEquals(name(context, resId) + " 的实测焦点环颜色必须等于主题 FOCUS 槽",
                    slot, ringColour(render(context, resId)));
        }
    }

    /**
     * 真正的「受到主题色彩控制」：同一个 drawable 在浅色表与深色表下必须渲染出不同的环色。
     * 只看 XML 文本无法证明这一点，只有栅格化后取像素才能证明。
     */
    @Test
    public void appSurfaceRingsFollowTheThemePaletteAcrossDayAndNight() {
        Context day = themedContext();
        int dayColour = ringColour(render(day, CARD_SELECTOR));
        int daySlot = focusSlot(day);

        RuntimeEnvironment.setQualifiers("+night");
        Context night = themedContext();
        int nightColour = ringColour(render(night, CARD_SELECTOR));
        int nightSlot = focusSlot(night);

        assertEquals("浅色表下环色必须等于浅色 FOCUS 槽", daySlot, dayColour);
        assertEquals("深色表下环色必须等于深色 FOCUS 槽", nightSlot, nightColour);
        assertNotEquals("浅色表与深色表的 FOCUS 槽本身就不同，测试前提必须成立", daySlot, nightSlot);
        assertNotEquals("换色板后焦点环颜色必须跟着变，否则主题控制无效", dayColour, nightColour);

        // 回归钉子：修复前这里写死 @color/white，两张表都会渲染成同一个白色。
        assertNotEquals("焦点环不允许再渲染成与调色板无关的固定白色",
                0xFFFFFFFF, nightColour);
    }

    /** 环必须完全不透明，否则焦点不可见。 */
    @Test
    public void focusedRingRendersFullyOpaque() {
        Context context = themedContext();
        for (int resId : new int[]{BUTTON_SELECTOR, CARD_SELECTOR}) {
            int colour = ringColour(render(context, resId));
            assertEquals(name(context, resId) + " 的焦点环必须完全不透明，否则焦点不可见",
                    0xFF, (colour >>> 24) & 0xFF);
        }
    }

    /**
     * 变异检验：本测试的度量方法必须真的能区分「细环」与「粗环」。
     * 直接渲染 1.5dp 与 3dp 两种描边，实测厚度必须不同——否则上面的断言就是空断言。
     */
    @Test
    public void theMeasurementCanActuallyTellThinRingsFromThickOnes() {
        Context context = themedContext();
        float density = context.getResources().getDisplayMetrics().density;

        Bitmap thin = renderStroke(context, Math.round(1.5f * density));
        Bitmap thick = renderStroke(context, Math.round(3f * density));

        int thinPx = ringThicknessPx(thin);
        int thickPx = ringThicknessPx(thick);
        assertTrue("度量方法必须能区分细环与粗环（实测 thin=" + thinPx + "px thick=" + thickPx + "px）",
                thickPx > thinPx);
    }

    /** 造一个与生产 drawable 同构的描边，用于度量方法的变异检验。 */
    private static Bitmap renderStroke(Context context, int widthPx) {
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        shape.setCornerRadius(0f);
        shape.setStroke(widthPx, 0xFFFFFFFF);
        Bitmap bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        shape.setBounds(0, 0, SIZE, SIZE);
        shape.draw(canvas);
        return bitmap;
    }
}
