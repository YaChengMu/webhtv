# dev3 同步 beta 与代码评审记录（2026-09-18）

## 目标

同步远端 `beta` 最新代码，评审当前任务的未提交改动及同步引入的已提交改动，完成针对性验证与复评后提交、推送，并创建合入 `beta` 的拉取请求。

## 同步基线

- 工作分支：`dev3`
- 同步前提交：`d014ea2a21620d221145a845be93776ac8a21b0e`
- 同步的远端 `beta`：`58f53536fd998a77a060718d478fb08c5933607d`
- 同步方式：快进合并
- 同步结果：成功，无冲突

## 改动内容

### 当前任务改动

- 将站点健康报表对话框调整为接近全屏的自适应尺寸。
- 横屏保留 `24dp` 外边距，竖屏保留 `16dp` 外边距。
- 宽高均保留最小值保护，避免极端屏幕尺寸产生无效窗口参数。
- 增加源码约束测试，覆盖横竖屏边距、宽高计算、装饰视图边距及旧比例尺寸移除。

### 从 beta 同步的改动

- 前台崩溃时恢复显示自定义故障恢复界面，而不是静默终止。
- 增加对应的崩溃恢复配置测试。

## 评审结论

### 第一轮评审

- 站点健康报表尺寸改动范围集中，没有改变报表数据、筛选、排序或清理行为。
- 尺寸计算使用现有 `ResUtil` 屏幕尺寸与 dp 转换能力，横竖屏策略明确。
- `Math.max(1, ...)` 可避免异常环境下产生零或负尺寸。
- 同步自 `beta` 的崩溃恢复改动仅恢复前台崩溃的自定义恢复页面，并有对应测试约束。
- 未发现需要修改的问题。

### 验证

执行：

```text
./gradlew :app:testMobileArm64_v8aDebugUnitTest \
  --tests com.fongmi.android.tv.setting.SiteHealthReportSourceTest \
  --tests com.fongmi.android.tv.ui.activity.CrashActivityDetailsTest
```

结果：`BUILD SUCCESSFUL`，87 个任务中 6 个执行、81 个为最新状态。

同时执行 `git diff --check`，通过。

### 第二轮复评

- 重新核对相对 `origin/beta` 的完整任务差异，改动仍仅涉及站点健康报表窗口尺寸及其测试。
- 测试覆盖当前任务和本次同步的崩溃恢复行为。
- 未发现正确性、兼容性、性能或作用域问题。
- 复评通过，可以提交并创建合入 `beta` 的拉取请求。

## 回滚方式

如需回滚当前任务，可还原站点健康报表对话框原有比例尺寸，并删除本次新增的尺寸约束测试；从 `beta` 同步的提交不属于当前任务回滚范围。

## 本轮同步复评补充（2026-09-18）

### 同步结果

- 已将 `origin/beta` 合入 `dev3`，合并提交为 `5791c9c713853ffdf1ff6550472468f0afd7faed`。
- 合并过程由 `ort` 策略自动完成，没有未解决冲突。
- `origin/beta` 已是当前 `HEAD` 的祖先。

### 复评发现与修复

1. 站点健康报表的“最近失败”排序只统计搜索、详情、解析和播放阶段，遗漏本轮新增的首页与分类阶段。现已把 `row.home.lastFailAt` 和 `row.category.lastFailAt` 纳入排序，并增加源码约束测试。
2. HLS 兼容规则会缓存 `RuleConfig` 编译结果，但保存用户广告规则或切换默认规则启用状态时只失效 `RuleConfig`，没有同步失效 `HlsRuleConfig`。现已在 `UserAdRuleStore.save` 与 `DisabledDefaultRuleStore.save` 中同步调用 `HlsRuleConfig.invalidate()`，避免规则变更后继续使用旧的 HLS 编译缓存，并增加回归测试。

### 最终验证

- Mobile arm64-v8a 定向单元测试：`BUILD SUCCESSFUL`，87 个 Gradle actionable tasks，5 executed、82 up-to-date。
- Leanback arm64-v8a 定向单元测试：`BUILD SUCCESSFUL`，87 个 Gradle actionable tasks，6 executed、81 up-to-date。
- 覆盖测试：`SiteHealthReportSourceTest`、`SiteHealthReportDialogSourceTest`、`HlsRuleConfigTest`、`MpvHlsAdblockGateTest`、`ExoParserAdblockGateTest`、`M3u8LegacyFallbackTest`、`HlsAdblockPipelineTest`。
- `git diff --check` 通过。
- `task_guard.sh check` 通过。
- 合并后的构建仅保留既有 deprecated API、unchecked operation 和 32 位原生库提示，没有新增编译或测试失败。

### 复评结论

同步内容与 `dev3` 当前站点健康统计、HLS 去广告兼容逻辑可以共存。本轮发现的两个缓存/排序遗漏均已修复并由 Mobile 与 Leanback 两个变体的定向测试覆盖，可以进入提交、推送及创建合入 `beta` 的拉取请求阶段。
