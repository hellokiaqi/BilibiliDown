# BilibiliDown

[![Java](https://img.shields.io/badge/Java-8%2B-green.svg)](#requirements)
[![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey.svg)](#requirements)
[![License](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](LICENSE)

A desktop application (Java / Swing) for browsing and downloading Bilibili videos:
single videos, bangumi episodes, **an UP's entire upload history**, collections and favorites —
with quality selection, batch download and a clean, modern UI.

> This project is a fork of [nICEnnnnnnnLee/BilibiliDown](https://github.com/nICEnnnnnnnLee/BilibiliDown).
> It keeps the original downloading engine and adds a redesigned interface, a resilient upload-list
> fetcher, an "UP library" batch downloader and quality auto-fallback.
> Licensed under Apache-2.0, as the original project.

## Screenshots

| Home | UP library (all uploads) |
| --- | --- |
| ![Home](release/preview/index.png) | ![UP library](release/preview/upLibrary.png) |
| **Video details** | **Download queue** |
| ![Video details](release/preview/avDetails.png) | ![Download queue](release/preview/download.png) |

## Features

- **Many input types** – BV / av / ep / ss / md / ml IDs, full URLs, UP space links (or a bare mid).
- **UP library** – load every public upload of an UP into a sortable, filterable table (by year),
  tick the ones you want, and queue them for download in one click. Works with thousands of videos.
- **Resilient fetching** – Bilibili rate-limits the space API (`-799`, HTTP 412). The fetcher paces
  requests, retries with backoff, keeps partial results and lets you resume from the exact page that
  failed instead of starting over.
- **Quality control** – pick a preferred quality; if it is unavailable the app either falls back to the
  best available one (`bilibili.download.quality.autoFallback`, on by default) or tells you, depending
  on configuration.
- **Login support** – QR-code, SMS and password login; cookies are stored locally in `config/`.
- **Batch download from a config file** – for unattended/periodic downloads (`batchDownload.config`).
- **Cross-platform** – Windows, macOS and Linux; runs on JRE 8+ (release builds target Java 8 bytecode).

## Requirements

- Java 8 or newer (JRE is enough to run; JDK 8+ to build).
- `ffmpeg` for merging DASH audio/video or converting `m4s` → `mp4`
  (a Windows build is bundled in `release/ffmpeg.exe`).
- Network access to Bilibili.

## Quick start

1. Download or clone this repository.
2. Run the launcher in `release/`:
   - **Windows**: double-click `Double-Click-to-Run-for-Win.bat`
     (use `...-debug.bat` to keep the console output when troubleshooting).
   - **macOS**: `Double-Click-to-Run-for-Mac.command`.
   - **Linux**: `java -jar release/INeedBiliAV.jar`, or install the desktop entry from
     `release/BilibiliDown.desktop`.
3. Paste a link / ID into the search box and press **Enter** (or click **查找 / Search**).

The launcher prefers a bundled JRE (`minimal-bilibilidown-jre/`, `runtime/`), then `JAVA_HOME`,
then `java` from `PATH`.

## Usage

### Single video / bangumi

Paste `https://www.bilibili.com/video/BV1xx411c7mD`, `av12345`, `ep123`, `ss123`, … and press Enter.
The video tab lists every available quality of every part; click one to queue the download.

### Download all videos of an UP (UP library)

1. Paste `https://space.bilibili.com/39597640` or just `39597640`.
2. Click **「UP 全部视频」** – a new tab opens and loads all public uploads.
3. Sort by date, filter by year, tick videos (selection survives filtering), choose a quality and
   click **「下载所选」**. The app resolves each video, queues it and switches to the download page.

You can also use `https://space.bilibili.com/<mid>/video?full=1`, which forces the `arc/search`
parser (50 items per page, paginated with the "next page" button). It is slower because it requests
details for every item, and is more likely to be rate-limited.

### Batch download from a config file

Menu → `配置` → `一键下载配置` → choose `batchDownload.config`, then `操作` → `一键下载`.
Example:

```ini
[url:https://space.bilibili.com/39597640/video]

download.condition = _:_
start.page = 1
```

Handy conditions: `_!downloaded` (skip already downloaded), `start.page = 5` (resume),
`stop.condition = page:10` (stop after page 10). Conditions on the same line are AND-ed; separate
lines are OR-ed.

See [USAGE_UP_ALL.md](USAGE_UP_ALL.md) for the full walkthrough (Chinese).

### Rate limiting

Bilibili aggressively limits the space/upload-list endpoints. When that happens the app:

- paces requests (at least 300 ms between pages, and honours `bilibili.download.period.between.query`);
- retries with backoff and falls back to the legacy endpoint;
- keeps everything it already fetched, explains the reason (e.g. `HTTP 412`) and enables
  **「继续加载」 (resume)** so you can continue later without restarting.

Logging in first, or raising `bilibili.download.period.between.query` (e.g. to `1000`), makes the
whole flow much smoother.

## Configuration

Settings live in `config/app.config` (created from the built-in template on first run). The most
useful keys:

| Key | Default | Meaning |
| --- | --- | --- |
| `bilibili.savePath` | `download/` | Where files are saved. |
| `bilibili.pageSize` | `20` | Items per page when listing an UP / collection. |
| `bilibili.menu.download.qn` | `1080P` | Preferred quality for batch downloads. |
| `bilibili.download.quality.autoFallback` | `true` | Fall back to the best available quality instead of raising an error. |
| `bilibili.download.period.between.query` | `0` | Milliseconds to wait between query tasks (raise it if you hit rate limits). |
| `bilibili.download.poolSize` | `3` | Concurrent downloads. |
| `bilibili.ffmpegPath` | `ffmpeg` | Path to `ffmpeg`. |
| `bilibili.user.login` | `qr` | Login mode: `qr`, `pwd` or `sms`. |

Place a custom `config/background.png` to get a subtle background image; delete it to go back to the
default gradient.

## Build from source

The project uses plain `javac` + `jar` (no Maven/Gradle):

```bash
# Linux / macOS
./package.sh          # produces INeedBiliAV.jar
./.github/scripts/package_launcher.sh   # optional: low-memory launcher (launch.jar)
```

```bat
:: Windows
package.bat
```

The compiled jar targets Java 8 bytecode (CI builds with JDK 8). Copy it into `release/` to use the
bundled launchers.

## Project structure

```
src/                 Java sources (engine, parsers, UI)
src-launcher/        Low-memory launcher (launch.jar)
libs/                Vendored dependencies (zxing, javax.mail, JAF)
release/             Ready-to-run folder: launchers, jar, ffmpeg, preview screenshots
src/resources/       Embedded default config and assets
.github/workflows/   CI: build, package and publish releases
```

## Third-party components

- [JSON.org](https://github.com/stleary/JSON-java) (MIT) – JSON parsing.
- [zxing](https://github.com/zxing/zxing) (Apache-2.0) – QR code generation.
- [ffmpeg](http://www.ffmpeg.org) – transcoding / merging (invoked as an external binary).
- [geetest-validator](https://github.com/kuresaru/geetest-validator) – captcha helper.
- AV/BV conversion inspired by [Colerar/abv](https://github.com/Colerar/abv) (MIT).
- Cookie refresh WASM analysis referenced from
  [SocialSisterYi/bilibili-API-collect#524](https://github.com/SocialSisterYi/bilibili-API-collect/issues/524).

## License and disclaimer

Licensed under the [Apache License 2.0](LICENSE).
This project is not affiliated with Bilibili. Please use it for personal, lawful purposes only —
respect Bilibili's terms of service and the rights of content creators. Do not redistribute
downloaded content.
