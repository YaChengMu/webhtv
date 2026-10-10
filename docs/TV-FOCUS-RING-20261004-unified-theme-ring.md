# TV-FOCUS-RING：电视版焦点高亮统一为「主题色边框环」

- 任务 ID：`tv-focus-ring-unified`
- 日期：2026-10-04
- 车道：standard（TV 视觉契约 + 主题系统交叉）
- 来源：用户两张实机截图（关于 WebHomeTV 弹窗、追更页）+ 明确要求

## 1. 用户原始要求

> 像图片中这种本身就自带主题色的图标，焦点移动到图标上基本上看不出来，建议在焦点周围再加一圈高亮框。
> 请完善统一电视版焦点高亮效果，统一采用边框高亮色彩的这种方案，并受主题色彩设置控制。

拆成三条可验收要求：

| 编号 | 要求 | 验收方式 |
| --- | --- | --- |
| R1 | 自带主题色填充的控件，获得焦点时**必须出现边框环**，不能只靠填充色变化 | 静态契约测试 + 实机截图 |
| R2 | 电视版焦点高亮**统一为同一种机制/同一宽度**，不再出现"有的只有填充、有的白环、有的黄环" | 静态契约测试 |
| R3 | 焦点环颜色**受主题色板设置控制**，用户可以改 | 主题 token 链路断言（`tv_item_focus_ring` → `webhtv_color_focus`） |

两张截图中用户圈出的目标：关于弹窗的 `检查更新 / 加速源 / 我已知悉` 三个按钮 + 齿轮图标；追更页顶栏 `检查更新/全部已读/显示全部/导入订阅` 与卡片内 `继续看/检查/标记已读/提醒开/换源/取消追更`。

## 2. 根因（硬证据）

### 2.1 焦点态只有填充变化，且填充色与常态**字节相同**

`app/src/main/res/color/dialog_primary_button_bg.xml`

```xml
<item android:color="@color/webhtv_color_primary" android:state_focused="true" />
<item android:color="@color/webhtv_color_primary" />          <!-- 与 focused 完全相同 -->
```

`app/src/main/res/color/following_button_primary_bg.xml`

```xml
<item android:state_focused="true" android:color="@color/following_button_focus" />  <!-- = webhtv_color_focus -->
<item android:color="@color/following_accent" />                                     <!-- = webhtv_color_primary -->
```

`colors.xml` 里 `following_accent = @color/webhtv_color_primary`、`following_button_focus = @color/webhtv_color_focus`，
而 `ThemeTokens` 冻结色板中 **light/dark 两套 `colorFocus == colorPrimary`**（`#0B57D0` / `#A8C7FA`，
leanback 编译值 `#A8C7FA`）。因此这两条 selector 的 focused 与 default 解析结果**完全相同** → 视觉零差异。
这正是截图里 `继续看`（`?attr/colorPrimary` 填充）和 `我已知悉`（`dialog_primary_button_bg`）看不出焦点位置的原因。

### 2.2 主题色填充的按钮完全没有边框通道

`app/src/main/res/layout/dialog_about.xml` 的 `checkUpdate / githubProxy`：

```xml
app:backgroundTint="?attr/colorPrimary"   <!-- 填充即主题色 -->
app:cornerRadius="8dp"                    <!-- 没有任何 strokeColor / strokeWidth -->
```

`confirm` 用 `@color/dialog_primary_button_bg`，`updateSettings`（齿轮）用
`android:background="@drawable/about_primary_icon_button"`，其中 focused 填充 **硬编码 `#0B57D0`**，
既不随主题走，也没有边框环。

`app/src/main/res/layout/item_following.xml` 与 `app/src/leanback/res/layout/activity_following.xml`
的 action 按钮同样只有 `app:backgroundTint`，没有描边通道。

### 2.3 焦点环 token 本身不受主题控制

`app/src/main/res/values/colors.xml:46`

```xml
<color name="tv_item_focus_ring">#FFD166</color>   <!-- 写死黄色，主题编辑器改不动 -->
```

`?attr/tvFocusRing`（`attrs.xml` 声明、两个 flavor 的 `Theme.Base` 绑定）取值就是它。
`docs/webhtv-unified-visual-design-system-20260920.md:414` 早已把 `focus` 定义为
"TV 焦点环/焦点容器"，但代码没有接线到 `webhtv_color_focus`（FOCUS 槽，`ThemeRole.isUserSlot()==true`）。

## 3. 最佳实践研究

| 证据等级 | 来源 | 支持的结论 | WebHTV 适用性 | 决策影响 |
| --- | --- | --- | --- | --- |
| A（平台官方） | Android TV *Focus system*，<https://developer.android.com/design/ui/tv/guides/styles/focus-system>，2026-10-04 读取 | "Focus indicators are visual devices that emphasize focused elements"；焦点必须始终明显可见 | 直接适用：本仓库正反例都在 TV | 采纳"必须可见"为验收底线 |
| A（平台官方） | Android *Optimizing Navigation for TV*：**"use uniform highlight scheme across your application"** | 一个应用必须使用统一高亮方案 | 直接适用，正是 R2 | 采纳"统一机制 + 统一宽度" |
| A（平台官方） | 同上，推荐用 Drawable State List Resources 实现 focus/selected 高亮 | 状态驱动、按状态切换视觉 | 直接适用 | 采纳 state list（`app:strokeColor` / `<stroke>`） |
| A（W3C 标准） | WCAG 2.2 SC 1.4.11 Non-text Contrast，<https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast> | 指示"是否聚焦"的视觉信息需与相邻色 ≥3:1 | 适用（本仓库文档已自设 ≥3:1 门槛） | 边框环对相邻填充必须 ≥3:1 |
| A（W3C 标准） | WCAG 2.2 SC 2.4.13 Focus Appearance，<https://www.w3.org/WAI/WCAG22/Understanding/focus-appearance.html> | 焦点指示至少相当于 2 CSS px 周长的面积，且焦点/非焦点像素 ≥3:1 | 适用 | 3dp 环宽满足面积要求；**纯色相填充变化被视为不达标** |
| B（成熟实现） | Material 3 *States* / Compose `RippleConfiguration.Focus.InsetRing`，<https://m3.material.io/foundations/interaction/states/applying-states> | 聚焦用"环"（ring）而非仅换填充，是 M3 的既定做法 | 适用（本项目已用 Material 1.14.0） | 采纳 ring 方案 |

结论：官方与标准都要求"统一 + 可见 + 高对比"。**填充色 == 焦点色**这类实现同时违反 2.4.13 与本仓库自设门槛，
必须改为边框环。无可用的"不做改动"选项。

## 4. 现有实现与调用链

| 位置 | 现状 |
| --- | --- |
| `app/src/main/res/values/attrs.xml` | 声明 `tvFocusRing` / `tvCurrentRing` / `tvNormalStroke` |
| `app/src/leanback/res/values/styles.xml`、`app/src/mobile/res/values/styles.xml` | `Theme.Base` 绑定三者到 `@color/tv_item_*` |
| `app/src/main/res/values/colors.xml` | `tv_item_focus_ring = #FFD166`（不随主题） |
| `theme/ThemeRole.java` | `FOCUS` 是用户可编辑槽（`isUserSlot()`），`colorOf()` → `tokens.colorFocus()` |
| `theme/ThemeController.java` | `colorResourceOf(FOCUS) = R.color.webhtv_color_focus`；`bindTheme/bindDialog` |
| `theme/ThemeBinder.java` | 只改写"精确等于基线角色色"的颜色；`bindButton` 已支持 `strokeColor` 通道 |
| `theme/ThemeResolver.java` | 由用户 seed 重新生成 on-色并做对比度校验 |

已有先例可复用：`app/src/main/res/color/dialog_outlined_button_stroke.xml` 就是
"stateful color list + `app:strokeColor`"，并在 `adapter_custom_csp.xml` 中用于 `MaterialButton`。

## 5. 方案比较与决策

| 方案 | 一致性 | 可见性保证 | 主题控制 | 回归风险 | 工作量 |
| --- | --- | --- | --- | --- | --- |
| A. 不改动 | — | 不保证（现状即缺陷） | 不满足 R3 | 0 | 0 |
| B. 全树运行时统一挂环（`ThemeBinder` 遍历 focusable 逐帧设 foreground） | 最高 | 高 | 中 | **高**：会覆盖既有 foreground（`selectableItemBackground`、`selector_tmdb_media_focus`），全站视觉需逐屏回归 | 大 |
| C. **状态化边框环 + 主题 token（采纳）** | 高（同一机制、同一宽度、同一语义规则） | 满足（环色取"填充的配对 on-色"，色板对比度门保证 ≥4.5:1） | 满足（全部走 `ThemeRole`） | 低（只新增描边通道，不动既有填充/文字链路） | 小 |

采纳 **C**。理由：C 是"最小且足够"的改动——只补上缺失的描边通道并把环色接线到主题角色，
不触碰任何既有填充、文字色、布局与测量；同时对每个填充家族都给出有对比度保证的环色。

### 统一规范（R1/R2/R3 的落地定义）

1. **机制**：焦点态一律由 **边框环** 表达（`MaterialButton` 走 `app:strokeColor`+`app:strokeWidth`，
   普通 `shape` 走 `<stroke>`）。填充色变化可以保留，但不得作为唯一焦点线索。
2. **宽度**：唯一定义 `@dimen/webhtv_focus_ring_width = 3dp`（与既有 `?attr/tvFocusRing` 3dp 规范一致）。
3. **颜色**：环色 = **该元素填充色的配对 on-色**（由主题色板生成，天然满足对比度门）：

| 焦点态填充 | 环色 |
| --- | --- |
| `webhtv_color_primary` / `webhtv_color_focus` | `@color/webhtv_color_on_primary` |
| `webhtv_color_secondary_container` | `@color/webhtv_color_on_secondary_container` |
| `webhtv_color_error` | `@color/webhtv_color_on_error` |
| `webhtv_color_error_container` | `@color/webhtv_color_on_error_container` |
| 透明 / 表面（图标按钮） | `?attr/tvFocusRing`（= FOCUS 槽） |

4. **主题控制**：`tv_item_focus_ring` 由 `#FFD166` 改为 `@color/webhtv_color_focus`，
   于是 `?attr/tvFocusRing` 的全部既有消费者自动变成"主题设置可控"。

### 非目标（明确不做，附理由）

- 播放器视频层焦点形状（`shape_video_focused`、`shape_subtitle_focused`、`shape_vod_focused`、
  `shape_live_focused`、`shape_keyboard_focused`、`selector_control_sheet_button` 等）保持白色不变。
  理由：它们叠在视频画面上而非应用表面，仓库已用
  `ThemeControllerContractTest` / `NativeEnhancedPlaybackStyleFocusTest` 固化"播放器焦点与通用焦点
  角色隔离"的契约；本次报告不涉及，改动会引入播放器可读性回归。
- 不改 `ThemeBinder` 的遍历/改写规则（不扩大运行时可写颜色面）。

## 6. 最小实施步骤

1. `app/src/main/res/values/colors.xml`：`tv_item_focus_ring` → `@color/webhtv_color_focus`。
2. `app/src/main/res/values/webhtv_dimens.xml`：新增 `webhtv_focus_ring_width = 3dp`。
3. 新增 `app/src/main/res/color/focus_ring_primary.xml`、`focus_ring_secondary.xml`、
   `focus_ring_error.xml`、`focus_ring_error_container.xml`（focused/pressed → 配对 on-色，其余 transparent）。
4. `app/src/main/res/drawable/about_primary_icon_button.xml`：focused/pressed 改为
   `?attr/colorPrimary` 填充 + 3dp `?attr/colorOnPrimary` 环（去掉写死 `#0B57D0`）。
5. `app/src/main/res/layout/dialog_about.xml`：`checkUpdate`/`githubProxy`/`confirm` 增加描边通道。
6. `app/src/main/res/layout/item_following.xml`：7 个 action 按钮增加描边通道（TV 实际使用的 item 布局）。
7. `app/src/leanback/res/layout/activity_following.xml`：4 个顶栏按钮增加描边通道。
8. 更新 `NativeEnhancedPlaybackStyleFocusTest` 中写死 `#FFD166` 的断言为新的主题接线。
9. 新增 `TvFocusRingContractTest` 固化 R1/R2/R3。
10. 更新 `docs/webhtv-unified-visual-design-system-20260920.md` 的焦点规范条目。

## 7. 验证计划（风险比例最小）

- 便宜且决定性：`./gradlew :app:testLeanbackDebugUnitTest --tests '*Focus*' --tests '*FollowingUi*'`（静态契约）。
- 编译门：`scripts/build_arm64_debug_install.sh`（覆盖安装，不清数据），在 dev1 模拟器 `192.168.50.3:5555`。
- 实机：关于弹窗 用 D-pad 依次聚焦 3 个按钮 + 齿轮，确认环可见；追更页顶栏与卡片按钮确认环可见。
- 主题控制：切换主题色板（改动 FOCUS 槽）后确认环色跟随变化。

## 7.1 实机验收记录与缺陷修正（2026-10-04）

首次实机截图已确认 R1 成立（3dp 环随焦点出现/消失），但像素级对比度计算暴露出本方案自身的
**两处配对错误**——环色必须配对“焦点态**实际**填充色”，而不是控件常态填充色：

| 控件 | 焦点态填充 | 首版环色 | 对比度 | 修正后环色 | 对比度 |
| --- | --- | --- | --- | --- | --- |
| `readAll` / `filter` / `alistImport` / 卡片 `read` | FOCUS `#A8C7FA` | `on_secondary_container` `#E2E2E6` | **1.33:1 ❌** | `on_primary` `#062E6F` | 6.09:1 ✅ |
| `delete`（`colorErrorContainer` 填充） | `#93000A` | `on_error` `#690005` | **1.40:1 ❌** | `on_error_container` `#FFDAD6` | 6.41:1 ✅ |

根因：`following_button_primary_bg` / `following_button_secondary_bg` 在 `state_focused` 时都把填充换成
`following_button_focus`（= FOCUS 色 = primary），所以这两个家族的环必须按 primary 配对；
`?attr/colorErrorContainer` 则必须配 `on_error_container` 而不是 `on_error`。

修正：`colors.xml` 用 `focus_ring_on_error_container` 取代 `focus_ring_on_error`；
`activity_following.xml` 的 readAll/filter/alistImport 与 `item_following.xml` 的 read 改引用
`@color/focus_ring_primary`；`focus_ring_error.xml` 改用 `focus_ring_on_error_container`。
并在 `TvFocusRingContractTest` 新增 `everyRingColourKeepsNonTextContrastAgainstItsFocusFill`，
对 light / night / leanback 三套生效 token 逐对计算 WCAG 对比度，把“环色必须与焦点填充 ≥3:1”
固化成回归测试（而非依赖人眼）。

### 实机证据（leanback，dev1 `192.168.50.3:5555`）

| 项 | 结果 |
| --- | --- |
| 焦点环出现/消失跟随焦点 | ✅ 聚焦时按钮外侧出现 6px(=3dp) 环带，失焦后消失 |
| `检查更新 check` | 环 `(28,64,123)` vs 填充 `(174,202,248)` = **6.09:1** |
| `全部已读 readAll`（修正后） | 环 `(28,64,123)` vs 填充 `(174,202,248)` = **6.09:1**（修正前 1.30:1） |
| `取消追更 delete`（修正后） | 环 `(252,219,215)` vs 填充 `(155,23,32)` = **6.41:1**（修正前 1.40:1） |
| 契约测试 | `TvFocusRingContractTest` 7/7、`NativeEnhancedPlaybackStyleFocusTest` 14/14、`FollowingUiSourceTest` 15/15，共 36 项全绿 |

## 8. 回滚

全部为资源与测试文件的新增/取值替换，无 SQL、无协议、无持久化格式变更。
回滚锚点：`git revert <本任务提交>` 或 `git reset --hard <提交前 HEAD>`；
无需要回滚的运行时数据。

---

## 9. 追加：应用表面焦点环统一（2026-10-09）

### 9.1 用户原始要求

> TV端上面两行按钮是 蓝色边框，下面的是 白色边框，边框粗细也貌似不一样，请统一风格并受到主题色彩控制

两张实机截图（`/tmp/orca-paste-1791555196409-*.png`、`/tmp/orca-paste-1791555204528-*.png`）：
同一电视首页上，`搜索/历史` 两行功能按钮是**蓝色**边框，`最近观看` 内容卡片是**白色**边框，
且两边粗细不同。

### 9.2 根因（像素级硬证据）

截图逐像素采样（`960x1280`，物理 1920x1080）：

| 控件 | 实测环色 | 实测厚度 |
| --- | --- | --- |
| `搜索` 按钮（`shape_item_focused`） | `(13,77,201)` ≈ `#0B57D0`（= day primary） | ≈2px 游程 |
| `最近观看` 卡片（`shape_vod_focused`） | `(243,242,248)` ≈ 白 | ≈4px 游程（含光晕） |

代码侧根因：应用表面的焦点环分裂成三族，各写各的取值。

| 家族 | 原取值 | 问题 |
| --- | --- | --- |
| `shape_item_focused` / `shape_item_round_focused` | `1.5dp ?attr/colorPrimary` | 蓝；`colorPrimary` 在 TV 上同时驱动约 100 个布局的聚焦文字色，不能兼作焦点环语义 |
| `shape_vod_focused` / `shape_vod_oval_focused` / `shape_keyboard_focused` / `shape_search_hot_word_focused` / `shape_chip_*_focused` | `1.5dp`~`2dp @color/white` | 白；完全不随主题走 |
| `shape_config_history_item_focused` / `shape_site_item_*` / `shape_group_button_focused` | `2dp` | 宽度又不同 |

`@color/white` 是**与调色板无关**的常量，因此用户改主题的「焦点色」槽时这些边框纹丝不动；
宽度也从 1.5dp 到 3dp 散布，正是「粗细也貌似不一样」。

### 9.3 判据：宿主决定环色来源

统一不能只看「都改成 `?attr/tvFocusRing`」——**宿主背景决定环色必须来自哪里**，
否则会引入新的「焦点看不见」缺陷。逐对计算 WCAG 对比度后确定边界：

| 宿主 | 背景（两张表相同？） | `?attr/tvFocusRing`（day 深蓝） | 白色 |
| --- | --- | --- | --- |
| 调色板表面 / 对话框面板 | 跟随调色板 | 5.2–7.9:1 ✅ | 1.2–1.3:1（day 浅底）❌ |
| 固定深色玻璃（`shape_dialog_glass_panel` 等） | 恒定深色 | **1.91:1** ❌ | 12.20:1 ✅ |
| 固定浅色面板（`shape_exit_confirm_dialog`、`shape_ad_stats_content`） | 恒定浅色 | **1.6–2.8:1**（night 浅蓝）❌ | 1.03–1.29:1 ❌ |
| 视频画面 / 透明控制条 | 不可预测 | 无保证 ❌ | 有保证 ✅ |

因此本次统一拆成两类：

- **统一到主题环**（宿主是调色板表面）：`shape_item_focused`、`shape_item_round_focused`、
  `shape_item_selected`、`shape_vod_focused`、`shape_vod_oval_focused`、`shape_keyboard_focused`、
  `shape_search_hot_word_focused` → `@dimen/webhtv_focus_ring_width` + `?attr/tvFocusRing`；
  自带主题色填充的三族（`shape_config_history_item_focused`、`shape_site_item_focused`、
  `shape_site_item_selected`、`shape_group_button_focused`）→ 环色取焦点态填充的配对角色
  （`?attr/colorOnPrimary` / `?attr/colorPrimary`），沿用 §「统一规范」的配对规则。
- **只统一宽度**（宿主固定明暗或视频层，环色保持与调色板无关）：`shape_chip_focused`、
  `shape_chip_round_focused`、`shape_live_focused`、`shape_video_focused`、
  `shape_subtitle_focused`、`shape_subtitle_pressed`、`selector_ad_stats_item`、
  `selector_search_scope_item`、`selector_exit_confirm_primary`、`selector_exit_confirm_secondary`
  → 宽度一律 `@dimen/webhtv_focus_ring_width`。

`selector_exit_confirm_*` 与 `selector_ad_stats_item` 保持字面量环色还有一条独立理由：
它们由 `TvFixedDarkSurfaceContrastTest` 的
`theFixedPanelsAreStillPaletteIndependentAndDark` 固化——面板固定浅色 + 按钮文字恒为白，
改走调色板会让夜间表的白字对比度掉到 1.72:1。

### 9.4 变更清单

| 文件 | 变更 |
| --- | --- |
| `shape_item_focused.xml` / `shape_item_round_focused.xml` / `shape_item_selected.xml` | `1.5dp ?attr/colorPrimary` → `@dimen/webhtv_focus_ring_width` + `?attr/tvFocusRing` |
| `shape_vod_focused.xml` / `shape_vod_oval_focused.xml` | `1.5dp/2dp @color/white` → token + `?attr/tvFocusRing` |
| `shape_keyboard_focused.xml` / `shape_search_hot_word_focused.xml` | 同上 |
| `shape_config_history_item_focused.xml` / `shape_site_item_focused.xml` / `shape_site_item_selected.xml` / `shape_group_button_focused.xml` | `2dp` → token（环色配对规则不变） |
| `shape_chip_focused.xml` / `shape_chip_round_focused.xml` / `shape_live_focused.xml` / `shape_video_focused.xml` / `shape_subtitle_focused.xml` / `shape_subtitle_pressed.xml` | `1.5dp` → token（环色保持白色，附宿主对比度理由） |
| `selector_ad_stats_item.xml` / `selector_search_scope_item.xml` | `2dp` → token |
| `selector_exit_confirm_primary.xml` / `selector_exit_confirm_secondary.xml` | `2dp` → token |
| `TvFocusRingContractTest.java` | 新增 `appSurfaceFocusRingsShareOneWidthTokenAndAThemeColour`、`homeFunctionButtonsAndContentCardsShareTheSameRingSpec` |
| `TvAppSurfaceFocusRingDeviceTest.java`（新增） | 像素级运行时证据（见 §9.5） |
| `InterfaceEntryInteractionTest.java` | 把写死 `2dp` 的断言改为断言唯一宽度 token |

### 9.5 验证证据

**像素级运行时证据**（`TvAppSurfaceFocusRingDeviceTest`，Robolectric `GraphicsMode.NATIVE`
真实栅格化后逐像素测量，6/6 通过）：

| 断言 | 结果 |
| --- | --- |
| 首页功能按钮与内容卡片的实测环**厚度**相同 | ✅ |
| 首页功能按钮与内容卡片的实测环**颜色**相同 | ✅ |
| 6 个应用表面 selector 的实测厚度 == `@dimen/webhtv_focus_ring_width` | ✅ |
| 实测环色 == 主题 FOCUS 槽（`webhtv_color_focus`） | ✅ |
| day 表与 night 表渲染出**不同**环色（真正的主题可控） | ✅ |
| 环完全不透明（焦点可见） | ✅ |
| 度量方法能区分细环与粗环（变异检验，防空断言） | ✅ |

**变异检验（证明测试真的能抓到用户报告的缺陷）**：把 `shape_vod_focused.xml` 改回
`1.5dp @color/white`（即修复前的状态）后重跑，**3 项断言转红**：
`appSurfaceRingsFollowTheThemePaletteAcrossDayAndNight`、
`homeFunctionButtonsAndContentCardsRenderTheSameRing`、
`appSurfaceRingsRenderInTheThemeFocusColour`。恢复后重新全绿。

**回归门**：

| 检查 | 结果 |
| --- | --- |
| `:app:testLeanbackArm64_v8aDebugUnitTest` | 4154 tests / 0 failures |
| `:app:testMobileArm64_v8aDebugUnitTest` | 4980 tests / 0 failures |
| `scripts/check_ui_tokens.sh` | `hex_drawables=0`、`violations=1`（= 基线，仅既存 `item_following.xml`） |
| `:app:assembleLeanbackArm64_v8aDebug` | BUILD SUCCESSFUL |

**未完成的实机复核**：dev1 模拟器（`192.168.50.3:5555`）在本轮全程离线
（`ping` 100% 丢包、ARP FAILED、5555/5557/5559/5561 全部 closed），因此本轮未能做
D-pad 逐项截图复核。同一批 drawable 的上一轮（§7.1）已在同一设备上做过实机验收，
且本轮的像素级栅格化证据与变异检验已覆盖用户报告的三个具体现象（蓝/白、粗细、主题跟随）。

## 10. 回滚（本轮追加）

全部为资源取值替换与测试新增，无 SQL、无协议、无持久化格式变更。
回滚锚点：`git revert <本任务提交>` 或 `git reset --hard <提交前 HEAD>`。
