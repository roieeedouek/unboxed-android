# Unboxed for Android

[![License](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)   [![Build Status](https://img.shields.io/github/actions/workflow/status/roieeedouek/unboxed-android/build.yaml?branch=master)](https://github.com/roieeedouek/unboxed-android/actions)   [![Releases](https://img.shields.io/github/v/release/roieeedouek/unboxed-android?include_prereleases)](https://github.com/roieeedouek/unboxed-android/releases)

App to interact with [TorBox](https://torbox.app/) APIs.

**Unboxed is a fork of [Unchained](https://github.com/LivingWithHippos/unchained-android)**, migrated
from a Real-Debrid client into a TorBox client: every network call, data model, and auth flow was
rebuilt against TorBox's API instead of Real-Debrid's. All credit for the original app, its
architecture, and its UI goes to [LivingWithHippos](https://github.com/LivingWithHippos) - see the
[Credits](#credits-crown) section below.

### What is TorBox :question:

TorBox is a service to download files from hosting websites and the torrent network. Files are
downloaded directly on their servers, which you can then use for your own downloads at high
speeds, without needing premium accounts on every individual hosting service. It can also stream
media files directly.
**N.B. TorBox is a paid service**

### Features :memo:

- [x] magnets/torrents support
- [x] file hosting services support (via TorBox's web downloads)
- [x] streaming support (best-effort - needs a player that supports streaming like mpv or VLC)
- [x] search websites for files with plugins
- [x] user info
- [x] themes

> Real-Debrid's container-file upload (`.rsdf`/`.ccf`/`.dlc`) and "remote traffic" toggle have no
> TorBox equivalent and were removed rather than stubbed out.

### Installation :calling:

Get the [latest release](https://github.com/roieeedouek/unboxed-android/releases) (debug and
release APKs) from this fork's GitHub Releases page, or build it yourself from source (see below).

### Developing and Contributing :writing_hand:

This app is written in Kotlin and uses the following architectures/patterns/libraries:

MVVM architectural pattern, Dagger-Hilt for dependency injection, Data Binding for managing
ui-data relations, Navigation, Moshi, Retrofit, OkHTTP, Room, Coroutines, Flow, Livedata, Coil

Build with Gradle from the `app/` directory (`./gradlew assembleDebug` / `./gradlew
assembleRelease`); a release build needs its own signing key, see `app/build.gradle.kts`'s
`signingConfigs` block.

The app is available in English, Spanish, French, Italian, Korean and Turkish - you can contribute
translations by forking the project and editing the `strings.xml` file in the `values-xx` folders.

#### Search Plugins

Search plugins (scrapers for sites like 1337x, nyaa, rlsbb, etc.) are unrelated to the
Real-Debrid/TorBox backend - they just find magnets/links, which then get handed to whichever
debrid service the app is configured for. Unboxed still points at the upstream
[unchained-plugins repository](https://gitlab.com/LivingWithHippos/unchained-plugins) by default,
and existing plugins work unmodified.

It's possible to create new plugins with a bit of knowledge of html and regexes. There's also a
work in progress
[wiki page](https://github.com/LivingWithHippos/unchained-android/wiki/Search-Engine) on the
upstream project.

### Credits :crown:

This fork wouldn't exist without the original **Unchained** project. Full credit for the app's
architecture, UI, and the large majority of its code goes to
[LivingWithHippos](https://github.com/LivingWithHippos) and the upstream contributors below. If
you'd like to support the original project, see its
[README](https://github.com/LivingWithHippos/unchained-android#donate-coffee) for donation links.

#### Beta testers

- Oathzed

#### Donors

- DaisyF8
- Roadhouse

#### Translators

- edgarpatronperez (spanish)
- mikropsoft (turkish)
- poihoii (korean)

#### Media

Logo and symbols inspired by
[minimal logo design set](https://www.rawpixel.com/image/843352/minimal-logo-designs-set) offered
by [rawpixel.com](https://www.rawpixel.com) (Unboxed recolors the same shapes in a teal/navy
palette).
Icons by [Fluent UI](https://www.svgrepo.com/collection/fluent-ui-icons-outlined/) offered by
[SVG Repo](https://www.svgrepo.com/)
Backgrounds courtesy of [haikei](https://haikei.app/) and
[SVG Backgrounds](https://www.svgbackgrounds.com/)
