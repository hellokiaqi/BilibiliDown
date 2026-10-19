# 下载指定 UP 主的全部视频

以 UP 主 `39597640`（拣尽南枝）为例，仓库提供三种方式，按推荐程度排列。

## 1. 前置条件

- 已安装 Java 8 或更高版本（`release` 目录下双击脚本可直接运行）。
- 网络可访问 B 站。
- 若视频需要登录/会员权限，请先登录账号（未登录时列表接口更容易被限流）。

## 2. 方式一：UP 全部视频列表（推荐，图形界面）

1. 启动程序。
2. 在主页输入框粘贴 `https://space.bilibili.com/39597640`，或直接输入数字 `39597640`。
3. 点击 **「UP 全部视频」**，会打开 **「UP 视频列表」** 标签页。
4. 列表会自动加载该 UP 的全部公开投稿：
   - 默认按发布日期倒序排列，点表头可改排序；
   - 可用「年份」下拉框筛选；
   - 勾选状态在切换年份时不会丢失，「全选/清空」作用于当前筛选结果。
5. 选择清晰度后点击 **「下载所选」**，程序会逐个解析并加入下载队列，完成后自动切到「下载页」。

### 关于 B 站的接口限流

投稿列表接口对请求频率非常敏感，连续请求可能返回 `-799`/`412` 等风控错误。程序的处理方式：

- 分页之间自动保持间隔，并遵守 `bilibili.download.period.between.query` 配置（毫秒）；
- 命中风控时自动退避重试，并回退到旧接口再试一次；
- 仍然失败时**保留已加载的数据**，在状态栏说明原因，并启用 **「继续加载」** 按钮，稍后点一下即可从断点继续，不必从头再来。

> 提示：如果频繁触发风控，先登录账号、把 `bilibili.download.period.between.query` 调大（例如 `1000`），或等几分钟后再点「继续加载」。

## 3. 方式二：`?full=1` 链接（走 arc/search 解析器）

在主页输入框粘贴：

```
https://space.bilibili.com/39597640/video?full=1
```

这会强制使用 `URL4UPAllFullParser`（`x/space/wbi/arc/search`），每页 50 条，用「下一页」继续翻页。
注意：该解析器会为每个视频额外请求一次详情，视频较多时较慢，也更容易触发限流；批量下载建议用方式一。

## 4. 方式三：一键下载（batchDownload.config）

适合“定期/无人值守”地批量下载。

1. 菜单栏 → `配置` → `一键下载配置` → 选择 `batchDownload.config`。
2. 菜单栏 → `操作` → `一键下载`。

配置文件位置（任选其一，程序目录优先）：

- `config/batchDownload.config`
- `release/config/batchDownload.config`

内容示例：

```
[url:https://space.bilibili.com/39597640/video]

download.condition = _:_
start.page = 1
```

- `url`：目标 UP 的视频页。
- `download.condition = _:_`：匹配所有视频。
- `start.page = 1`：从第一页开始。

换成其他 UP 主时，把配置里的 mid 改成目标数字 ID 即可。

### 常用进阶配置

```
download.condition = _!downloaded        # 跳过已下载
start.page = 5                          # 断点续跑
stop.condition = page:10                # 处理到第 10 页停止
```

> 同一行条件用逗号分隔表示“且”，多行条件表示“或”。

## 5. 下载位置与清晰度

- 默认保存到程序目录下的 `./download/`，可在 `config/app.config` 中修改 `bilibili.savePath`。
- 期望清晰度不可用时的行为由 `bilibili.download.quality.autoFallback` 控制：
  - `true`（默认）：自动降级为实际可用的最高清晰度，并在日志中说明；
  - `false`：抛出提示，要求更换优先清晰度后再试。

## 6. 常见问题

| 现象 | 处理 |
| --- | --- |
| 状态栏提示“请求过于频繁/触发风控” | 稍等片刻点「继续加载」；或登录账号；或调大 `bilibili.download.period.between.query` |
| 列表为空 | 确认该 UP 有公开投稿、链接可在浏览器打开；未登录时部分 UP 会被限制 |
| 个别视频没加入队列 | 状态栏会提示失败数量，通常是该视频需要登录/会员或有特殊权限限制 |
| 下载清晰度低于预期 | 见第 5 节；会员视频需要账号权限 |
| 双击脚本没反应 | 用 `Double-Click-to-Run-for-Win-debug.bat` 运行，会保留控制台输出和错误信息 |
