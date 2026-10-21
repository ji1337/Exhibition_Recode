# Exhibition Client Source

> **This is a fork of [ExhibitionSource/exhibition-client](https://github.com/ExhibitionSource/exhibition-client).**
> preserved unchanged. What this fork changes is listed below.

## Changes in this fork

The goal is to make the client build and produce a runnable JAR on a modern toolchain,
without the 2.7 GB legacy MCP `jars/` tree that upstream depended on.

### Build configuration

- **`Client.iml` rewritten (313 → 14 lines)** — dependencies now resolve from the in-repo
  `libs/` directory via a single `libs` project library, instead of `$MCP_LOC$/jars/...`.
- **`.idea/libraries/libs.xml`** — one consolidated library definition, replacing upstream's
  dozen per-dependency XML files.
- **`.idea/artifacts/Exhibition.xml`** — the fat-JAR artifact now extracts every JAR in `libs/`
  into the output, so `Exhibition.jar` is self-contained (upstream's packed module output only).
- **`src/minecraft/META-INF/MANIFEST.MF`** added (`Main-Class: Start`), making the artifact
  directly runnable.
- **`jars/` is no longer tracked** — it is game runtime data, needed only to *run* the client
  locally, never to build it.

### Dependency upgrades (`libs/`)

| Library | Upstream | This fork |
| --- | --- | --- |
| gson | 2.2.4 | 2.14.0 |
| JNA / JNA-platform | 5.1.0 | 5.10.0 |
| OSHI core | 3.11.0 | 5.8.5 |
| Apache HttpClient | 4.5.2 + httpcore 4.4.4 + httpasyncclient 4.1.4 | httpclient5 5.6.2 + httpcore5 5.3.1 |
| spotify-web-api-java | 6.0.0 | 6.1.0 |
| platform | 3.4.0 | removed (superseded by jna-platform) |

### Source changes

- **`Start.java`** — launcher arguments are no longer blindly prepended. The defaults
  (`--version`, `--accessToken`, `--assetsDir`, `--assetIndex`, `--userProperties`) are now added
  only when the launcher did not already supply them, fixing duplicate-argument failures when
  starting through HMCL and similar launchers.
- **`CommandManager.java`** — commented out the import of the missing
  `org.sun.verification.Unknown`.

## Building

There is no Maven or Gradle build; the IntelliJ project files in `.idea/` and `Client.iml` *are*
the build definition, which is why they are tracked.

1. Open the project in IntelliJ IDEA with the project SDK set to **Java 8**.
2. **Build → Rebuild Project**.
3. **Build → Build Artifacts… → Exhibition → Rebuild** produces
   `out/artifacts/Exhibition/Exhibition.jar`.

See [`AGENTS.md`](AGENTS.md) for fuller development notes.

---

### Why is this repository public?

This repository was made public due to the client no longer being developed or maintained. It is provided for educational and archival purposes only.

I am not sure if every update is included in between the commits. Most notable changes are included, but there may be some missing.

### Final Note

It has been fun working on this project since 2016 after numerous Lucid/Crest/Virtue 5.5 skidded clients. Triton was the first client, Crest leak based, that I started to make my own features for. From recreating Trendy jump, jump pot, and having Friendly/Lynch help explain and fix 4 damage durability for my tick aura remake. After KitPvP servers like VeltPvP started to die off due to EULA, the next NCP server to target was the Pixel.

One of my biggest regrets was neglecting the client for so long from 2018-2019. I had a lot of plans for the client, but I was too busy with school and work to continue development. I hope that this repository can serve as a reminder of what could have been and inspire others to continue working on their own projects.

The Pit was another regret I had. Many great features were planned and I should have made more pit features earlier in the development of the client.

Some of my personal favorite features were the visuals, adaptive target strafe, client smoothness after fixing ground offset crits, and during the "Disabler" period being able to significantly reduce aura angle flags with Reduce mode and Bypass by using illegal angles to keep the same Yaw and attack behind the player.

With so many clients coming up around the same 2019-2021 period, it was a fun time to be a part of the competition. The community that we cultivated around the client was amazing, and I am grateful for all the support we received. Many other client communities were toxic, but we kept good people around and had a lot of fun.

### Thanks and Credits

- Dream - For being an amazing friend, helping with the client and community, and for always being there to support me and my projects.
- Valkyrie - Great guy, gave me his TTF font renderer and helped me with many issues.
- Aristhena - For putting in time and effort to make a great KitPvP client.
- N3xus - Interesting guy, helped explain the original item swapping dura method and how it worked.
- Friendly/Lynch - For helping me with many issues, especially with fixing the 4 damage durability issues in tick aura.
- Ceanko - A good lesson that others should not be trusted, even if they seem friendly and helpful.
- LPK - For his original client base that Exhibition was based on.
- Neohack - Great friend, one of the first people who bought the client early on (with an apple gift card lol). Went on to contribute to the client with many features and ideas.
- Frog & Implacity - Amazing people who helped with the client and community, always there to support and provide feedback. They were closer to Dream but were always great members to be around.
- Peh - This tyrant was a great friend and supporter of the client. Helped out with management and was there to provide feedback and ideas for the pit.
- Tomato - Zesty red dot drink guy, always hung around and gave good feedback and criticisms. Helped with the pit and was a great friend.
- 3ds - A great friend who always stuck around and enjoyed the client and community. Thanks for being there and providing feedback.
- Catgorl - Helpful and supportive, gave feedback on UI designs and random ideas.
- Alerithe - Great member of the community, provided a simple HTTP server for the spotify callback.
- Max - A great friend who was always there to support and provide feedback. Helped with the client and community.
- Anthrecite - A great member and friend of the community. May he rest in peace.
- All the beta testers - For providing feedback and sacrificing their time and alts to help improve the client.
- All the people who contributed to the client, whether it was through code, ideas, or just being a part of the community.
