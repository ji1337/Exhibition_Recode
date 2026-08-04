# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

See also [`AGENTS.md`](AGENTS.md) (contribution conventions) and [`README.md`](README.md) (what this fork changed vs. upstream).

## Build & Run

There is **no Maven or Gradle**. The IntelliJ project files are the build definition and are tracked on purpose:

- `Client.iml` — single module `Client`; source root `src/minecraft`, resource root `src/minecraft/assets`, output `bin/`.
- `.idea/libraries/libs.xml` — the one `libs` project library; every JAR in `libs/` is listed here.
- `.idea/artifacts/Exhibition.xml` — fat-JAR definition; extracts each `libs/*.jar` into the output.
- `.idea/misc.xml` / `.idea/compiler.xml` — **Java 8** SDK, bytecode target 8.
- `src/minecraft/META-INF/MANIFEST.MF` — `Main-Class: Start`.

Adding a dependency means editing *three* files: drop the JAR in `libs/`, add a `<root url="jar://$PROJECT_DIR$/libs/…!/"/>` to `libs.xml`, and add an `<element id="extracted-dir" path="$PROJECT_DIR$/libs/…"/>` to `Exhibition.xml`. Skipping the artifact entry produces a JAR that builds fine but `NoClassDefFoundError`s at runtime.

Workflow:

1. Open in IntelliJ IDEA, project SDK = Java 8.
2. **Build → Rebuild Project** → compiles into `bin/`.
3. **Build → Build Artifacts… → Exhibition → Rebuild** → `out/artifacts/Exhibition/Exhibition.jar`.
4. Run the `Start` class from IDEA, or smoke-test the artifact:
   `java -cp './out/artifacts/Exhibition/Exhibition.jar' Start --username dev --gameDir './run'`

A small/thin `Exhibition.jar` means the artifact build was incomplete — verify it contains `Start.class` plus the library classes.

`jars/` (legacy MCP game data), `bin/`, and `out/` are git-ignored. `jars/` is only needed to *run* locally, never to build.

## Driving the build from Claude Code (IDEA MCP)

Because the build lives entirely in IntelliJ, **there is no way to compile or run this project from the shell.** The bridge is IntelliJ's built-in MCP server, registered at user scope:

```
claude mcp get idea        # sse → http://127.0.0.1:64342/sse
```

It reports as `IntelliJ IDEA MCP Server 2025.3.5` and exposes 23 tools. The ones that matter here:

| Tool | Use |
| --- | --- |
| `build_project` | The **Rebuild Project** step — the only way to compile. `{"rebuild": true, "timeout": 40000}` does a genuine full rebuild of all ~2200 sources in ~15 s. |
| `get_file_problems` | Compile/inspection errors for one file, without a full rebuild. |
| `get_run_configurations` / `execute_run_configuration` | The project has one config, **`Start`**, whose working directory is the repo's `jars/`. |
| `get_project_modules` / `get_project_dependencies` | Confirms the single `Client` JAVA_MODULE and its `libs` library. |
| `get_symbol_info`, `rename_refactoring`, `search_in_files_by_regex` | PSI-aware navigation/refactoring across the ~2200 source files. |

Caveats, all observed in practice:

- **IDEA must be running with this project open.** The server serves whatever project is open; `get_project_modules` returning `Client` is the cheap way to confirm you are talking to the right one.
- **The first tool call after connecting takes ~10 s** (IDEA warm-up/indexing). Short timeouts will look like a hang — the call is not lost, the response just arrives late.
- `64342` is IDEA's built-in server port; a second IDE instance gets a different port, and the registered URL then points at the wrong one.
- MCP servers are loaded at Claude Code **session start** — after `claude mcp add`, restart the session before the tools are callable.
- Building the fat JAR (**Build Artifacts → Exhibition**) is *not* covered by `build_project`; that still needs the IDE UI or a dedicated artifact run configuration.

**Baseline build result:** a clean full rebuild is `isSuccess: true` with **0 errors and 22 warnings** — 21 are `JsonParser()` / `parse(...)` deprecations (fallout of this fork's gson 2.2.4 → 2.14.0 upgrade, in `StatisticsFile`, `ShaderGroup`, `ShaderManager`, `AbstractResourcePack`, `ResourceIndex`), plus one `ProtocolVersion.getId()` deprecation in `ViaFabricAddress`. Treat 22 as the floor: anything above it came from your edit. Note `build_project` with `rebuild: false` returns in a few seconds having compiled nothing when `bin/` is already current — check that `bin/**/*.class` mtimes actually moved before believing a fast "success".

## Testing

No test framework, no coverage gate. Classes named `*Test` (e.g. `management/command/impl/NotificationTest.java`) are in-game diagnostics, not JUnit. Verification = rebuild project → rebuild artifact → smoke-launch. For launcher/argument changes, test both a bare launch and one with explicit `--version/--accessToken/--assetsDir/--assetIndex/--userProperties` to catch duplicate-argument regressions.

## Architecture

### Boot chain

`Start.main` → reflectively calls `Main.bruh(args)` → `Main` parses launcher options and constructs `MCHook` (a subclass of `net.minecraft.client.Minecraft`) → `MCHook.drawSplashScreen` constructs `Client`, so client init runs *inside* the vanilla splash-screen call.

`Start` only fills in `DEFAULT_ARGUMENTS` the launcher did **not** already supply (`addMissingDefaults`) — this is what makes HMCL-style launchers work.

Two-phase client init:
- `Client.init(ProgressScreen)` — fonts, Discord RPC thread, `CommandManager`, `ModuleManager`, ViaFabric. Runs at splash time.
- `Client.setup()` — module status/settings load, `ClickGui`, `FileManager`, config/macro/waypoint managers, `EventSystem.register(this)`. **Called from `AuthenticatedUser.setupClient()`**, i.e. only after login succeeds. Anything needing a logged-in user belongs in `setup()`, not `init()`.

### Event system (`exhibition/event`)

Hand-rolled, reflection-scanned, singleton-instance dispatcher — not a typical bus:

- Listeners implement `EventListener` (`getName()`, `onEvent(Event)`) and annotate a method `@RegisterEvent(events = {EventX.class, …})`. `EventSystem.register(listener)` scans declared methods for that annotation to learn which events the listener wants — but dispatch always goes through the interface's `onEvent`, so listeners do `if (event instanceof EventX)` branching inside one method.
- Each event type is a **single reused instance** held in the `EventSystem.instances` static map. New events must be added to that static block or `getInstance` returns null.
- Firing pattern from patched vanilla code: `EventSystem.getInstance(EventTick.class).fire(...)`, then check `event.isCancelled()`. Events carry mutable fields set by `fire(...)` — never hold a reference to an event across ticks.
- `EventSubscription.fire` **drops every event when `mc.thePlayer == null`**, except `EventScreenDisplay` and `EventPacket`. Listener order comes from `EventListener.Priority` (`FIRST`…`LAST`, default `NORMAL`).

Hooks live directly in patched vanilla sources — there is no mixin/ASM layer. The ~14 files that fire events are `net/minecraft/{client/Minecraft, client/entity/EntityPlayerSP, client/renderer/EntityRenderer, network/NetworkManager, entity/Entity, block/Block, …}`. Grep for `EventSystem` under `src/minecraft/net/` to find every hook site.

### Modules (`exhibition/module`)

`Module extends Saveable implements EventListener, Bindable, Toggleable, MinecraftUtil`. The `MinecraftUtil` interface exists solely to give every implementor a `mc` field.

- Registration is a hardcoded list of `add(new Foo(new ModuleData(Category, "Name", "description")))` calls in `ModuleManager.setup()` — that entire block is wrapped in a swallowing `catch (Exception e) {}`, so a constructor that throws silently drops that module *and every module after it*. Add new modules to the end and verify they appear in-game.
- `Module.toggle()` registers/unregisters the module with `EventSystem` and calls `onEnable`/`onDisable`/`onToggle`. A disabled module receives no events at all.
- Settings are a `SettingsMap` of `Setting<E>` keyed by **uppercase** name; the convention is a `private String NAME = "MODE";` field per setting plus `settings.put(NAME, new Setting<>(NAME, value, "description"))` in the constructor. Value types drive both the ClickGUI widget and persistence: `Boolean`, `Number`, `String`, `Options` (enum-like), `MultiBool`, `BlockList`.
- Categories are `ModuleData.Type` (Combat / Player / Movement / Visuals / Other / Minigames). Note `Type.values()` omits `Minigames`.

### Persistence

Two unrelated mechanisms — don't confuse them:

- **Flat text**, in `<gameDir>/ArthimoWare/` via `FileUtils.getConfigFile(name)` → `Mods.txt` (name:enabled:hidden:held), `Binds.txt`, `Sets.txt` (module:setting:value, with `GlobalValue` as a pseudo-module for `GlobalValues`). Written by `ModuleManager.saveStatus()/saveSettings()`, parsed by colon-splitting in `loadStatus()/loadSettings()` — so **`:` and spaces in setting values are fragile** (spaces round-trip as `_`, except for settings named `URL`).
  - `Mods.txt` has **two four-field formats**: legacy `name:enabled:bind:hidden` and current `name:enabled:hidden:held`. Field count cannot tell them apart, so `loadStatus()` discriminates on `split[2]` — an integer means legacy (`isBindField`). A legacy file leaves `isNew` false, which fires the one-shot migration at the end of `loadStatus()`: notify, then `saveStatus()` rewrites `Mods.txt` in the current format and splits the keybinds out into `Binds.txt`.
- **Gson JSON**, via `Saveable` (`save()`/`load()`), `@Expose`-only, into `SubFolder` dirs (`Plugins`/`Configs`/`Other`).

### Other subsystems

- `exhibition/management/` — one manager per concern (`command`, `config`, `font`, `friend`, `keybinding`, `macros`, `notifications`, `spotify`, `waypoints`), most extending `AbstractManager<E>` (an array-backed registry with `get(Class)` lookup). `GlobalValues` holds client-wide `Setting`s.
- `exhibition/management/command/` — chat commands registered by alias array in `CommandManager.setup()`; the `ChatCommands` module is force-enabled at startup.
- `exhibition/util/security/` — auth, HWID, encryption, integrity/instrumentation checks. `Castable` exists to obscure casts from bytecode analysis; several call sites use `Class.forName(...).getMethod(...)` reflection and odd system-property gates (e.g. `NEoBuMASs`) for the same reason. Preserve that style when touching these files; don't "clean it up".
- `exhibition/gui/click/` — the ClickGUI (plus a `virtue/` alternate skin), opened from key handling inside `Minecraft.java`.
- ViaVersion/ViaBackwards/ViaRewind are **vendored as source** under `com/github/creeper123123321` (ViaFabric), `de/gerrygames`, `io/github`, `nl/matsv`, and initialized via `new ViaFabric().onInitialize()` in `Client.init`. `Client.is1_9orGreater()` / `is1_16_4()` read the negotiated protocol out of the Via decoder in the Netty pipeline — use these for version-dependent behavior.
- OptiFine and shader-mod sources are also vendored (`optifine/`, `shadersmod/`).

## Conventions

- Java 8 syntax only; four-space indent; match the surrounding file's style (imported legacy sources are not uniform).
- `PascalCase` classes, `camelCase` members, `UPPER_SNAKE_CASE` constants; lowercase packages matching directories.
- Do not reformat unrelated code, rename obfuscated Minecraft symbols (`var1`, `func_110121_a`, `p_main_0_`), or bulk-clean generated sources. Diffs in `net/minecraft/**` should be minimal and hook-focused.
- Commits: concise imperative, e.g. `fix: preserve launcher arguments`. Never commit `bin/`, `out/`, `jars/`, alt-manager accounts, access tokens, or machine-specific game directories.
