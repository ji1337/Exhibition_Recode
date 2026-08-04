# Repository Guidelines

## Project Structure & Module Organization

This is a legacy Java 8 Minecraft client project organized as one IntelliJ module, `Client`. Production Java sources live under `src/minecraft`; packages mirror directories such as `exhibition/module`, `exhibition/management`, and `net/minecraft`. Runtime resources are under `src/minecraft/assets`. Third-party dependencies are stored in `libs` and are wired into the module through the single `libs` project library in `.idea/libraries/libs.xml`. The legacy MCP/Minecraft layout under `jars` is git-ignored: it is only needed to *run* the client locally, never to build it. Treat `bin` and `out` as generated output. The fat-JAR definition is `.idea/artifacts/Exhibition.xml`, and its manifest is `src/minecraft/META-INF/MANIFEST.MF`.

## Build, Test, and Development Commands

There is no Maven or Gradle wrapper. Use IntelliJ IDEA with the project SDK set to Java 8.

- **Build → Rebuild Project**: compiles the `Client` module into `bin`.
- **Build → Build Artifacts… → Exhibition → Rebuild**: creates `out/artifacts/Exhibition/Exhibition.jar` and embeds the JARs listed in the artifact definition.
- Run the `Start` class from IDEA for local development.
- From PowerShell, smoke-test a built artifact with Java 8:
  `& java -cp '.\out\artifacts\Exhibition\Exhibition.jar' Start --username dev --gameDir '.\run'`

After dependency changes, verify that the artifact still contains `Start.class` and the required library classes; a small thin JAR indicates an incomplete artifact build.

## Coding Style & Naming Conventions

Use Java 8-compatible syntax and four-space indentation. Follow the brace and formatting style of the file being edited; the imported legacy sources are not fully uniform. Use `PascalCase` for classes, `camelCase` for methods and fields, and `UPPER_SNAKE_CASE` for constants. Keep packages lowercase and aligned with their directory paths. Avoid unrelated reformatting, renaming obfuscated Minecraft symbols, or broad generated-source cleanup.

## Testing Guidelines

No automated test framework or coverage gate is configured. Classes ending in `Test` are in-game diagnostics, not JUnit tests. At minimum, rebuild the project, rebuild the `Exhibition` artifact, and smoke-launch `Start`. For launcher or argument changes, test both an empty/default launch and a launch with explicit options to catch duplicate arguments.

## Commit & Pull Request Guidelines

Use concise imperative commits, for example `fix: preserve launcher arguments`. Pull requests should describe behavior changed, affected packages, build and launch verification, and any required configuration migration. Include screenshots for visible UI changes and relevant sanitized logs for startup failures. Do not commit `bin`, `out`, `jars`, launcher accounts, access tokens, or machine-specific game directories. Note that `.idea/` and `Client.iml` *are* tracked on purpose — with no Maven or Gradle build, they are this project's build definition.
