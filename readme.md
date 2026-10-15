# MatriceChat

MatriceChat is a Matrix client based on [SchildiChat Next](https://github.com/SchildiChat/schildichat-android-next) and [Element X](https://github.com/element-hq/element-x-android).
Install the universal APK from the latest `Build Android APK` GitHub Actions run. Its package ID is `chat.matrice.android`, so it can be installed beside SchildiChat Next.


## MatriceChat features

- Enable personal packs and global room packs in FluffyChat; their emoji and stickers are then available in every MatriceChat conversation.
- Type `:smi` to search emoji by shortcode or name, or use the smiley beside the message field. The picker has Emoji and Stickers tabs and focuses search automatically.
- Selecting a sticker sends it immediately, including the current reply target. Custom emoji can also be used as reactions.
- Mark an image or video as a spoiler with the visibility button in the attachment preview. Incoming tagged media stays hidden until you reveal it.
- Feature switches are under **Turtle tweaks → Conversation screen**. Global wallpaper and light/dark colors, including alpha, are under **Turtle tweaks → General**.

The feature switches are enabled by default. Pack creation and management remain in FluffyChat.

## Building

In general, building works the same as for Element X or any common Android project.
Just import into Android Studio and make sure you have all the required SDKs ready.


## WYSIWYG development

To develop changes in our [matrix-rich-text-editor fork](https://github.com/SchildiChat/matrix-rich-text-editor):

### Build WYSIWYG locally

- Clone the repo
- Bump the version number to some future version that [doesn't exist yet](https://github.com/SchildiChat/matrix-rich-text-editor/tags)
  using `./update_version 1.2.3` where `1.2.3` is your chosen version number. By not re-using any existing version number you can make sure you're using your
  local build if the build of SchildiChat succeeds.
- Publish the wysiwyg by running `make android` in its directory. (Make sure you have `JAVA_HOME`, `ANDROID_NDK_HOME` and all the build dependencies setup)

### Include local-built WYSIWYG in SchildiChat

- Modify `settings.gradle.kts` to insert `mavenLocal()` into the `dependencyResolutionManagement {}` block.
  ([Cherry-pick](https://github.com/SchildiChat/schildichat-android-next/commit/de2a30082012a079d8978caf7d5af7e5764310a4))
- Change the version number of `wysiwyg` in `gradle/libs.versions.toml` to match the one you published locally.


## Contributing

Generally, contributions are welcome!  
Note that in order to ease upstream merges, we want to leave the smallest footprint possible on Element's sources
when implementing original features or patching Element's behaviour.

In particular (may change a bit while the project is still in alpha):
- Put code into additional files (`chat.schildi.*` package names) if reasonable
    - Prefer `schildi/lib` module if it doesn't depend on element modules (except maybe strings)
    - Prefer `schildi/components` module if it depend on some of Element's Design/UI components but nothing else
    - Otherwise, prefer element module where it makes most sense (or create a new module for bigger features, maybe)
- Put Schildi-specific drawables and other xml resources that override upstream resources into the `sc` build flavor
    - This way, we can use the same name and avoid merge conflicts
    - Compare e.g. `libraries/designsystem`: we define the flavor `sc` and thus put drawables in the `sc` instead of `main` directory.
      For new modules that do not feature a `sc` flavor yet, copy over the required `build.gradle.kts` content from a module that does.
- Put Schildi-specific strings into `schildi/lib`
    - Never touch upstream strings! If we want to change Element's strings, we'll either want a script that patches them,
      so we can restore upstream strings before upstream merges and re-do our changes automatically after the merge,
      or alternative put it into an `sc` build flavor, if we do not need to touch multiple translations.
- Don't worry too much about code style if violating it can ease upstream merges
    - When putting upstream code into a new block (e.g., putting it in an `if`-statement), don't indent the upstream code, but rather add comments like
        `// Wrong indention for merge-ability - start` and `// Wrong indention for merge-ability - end`

### Settings backup

Use **Settings → Settings backup** to export or import local settings, wallpaper and custom sounds. See [the backup guide](docs/settings-backup.md).

Wallpaper opacity is adjustable from 0–100% under **Turtle tweaks → General → Appearance** and is included in settings backups.
