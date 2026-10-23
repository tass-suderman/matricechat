# Features and changes compared to upstream

Note that following list of changes compared to Element X is likely incomplete, since it is only occassionally updated.


## General UI & App Behaviour

- Schildi theme with more neutral colors and some design tweaks †
- Schildi layout tweaks †
- Faster screen transitions †
- App icon & branding
- Customizable colors for message bubbles †
- Render bridge icons in inbox † and chat title
- Render [MSC4431](https://github.com/matrix-org/matrix-spec-proposals/pull/4431) personal room name overrides


## Chat overview ("room list")

- Bottom space bar with swipe navigation, including support for hierarchical spaces †
- Filter for favorites, unreads, DMs, and group chats via our spaces navigation
- Configure room list sort order to optionally: †
    - Show unread chats on top (while optionally ignoring muted unreads)
    - Pin favorites
    - Show low priority on bottom
- Show start-new-chat action in the top app bar rather than as floating action button †
- Show unread counts †
- Show alt text for inline images / custom emotes, instead of not showing them at all
- When filtering for DMs, also treat DMs with more than 2 members as DMs if marked as such


## Conversation screen

- Floating date header while scrolling †
- Bigger stickers
- Animate Lottie (`.json` and gzipped `.tgs`) stickers, including files with an incorrect image MIME type
- Animate APNG stickers through on-device FFmpeg previews on 64-bit devices
- Show Klipy and Tenor MP4s as looping GIF previews using on-device FFmpeg on 64-bit devices
    - Previews are limited to 30 seconds, 480px, and 15fps; tapping opens the original video
    - Set `bridge.klipy_gifs: false` in mautrix-discord and restart it to keep conversion off the bridge for new messages
- Differentiate notices from normal text messages by adding some transparency
- Custom [compose-native message rendering](https://github.com/beeper/matrix-messageformat-compose) instead of old view-based spans
    - Render collapsible `<details>` tags in messages
    - Render reveal-able spoilers in messages
    - Render inline images such as custom emotes in text messages
    - Render tables
- URL previews (optionally also in encrypted chats) †
- Hide deleted messages †

- Allow sending freeform reactions
- Don't waste horizontal space in message bubbles with forced line-breaks that do not make full use of the available width
- Setting for reply preview max lines †

- Disable Element's pinned message overlay on top of the conversation screen †
- Access pinned messages via toolbar action when the pinned message overlay is disabled †
- Hide some icons from the top app bar †


## Notifications

- Setting to only alert once for successive messages in the same chat
- Notify about failures to resolve notifications


## Experimental/unstable features

- Color usernames in rooms by member powerlevel †


## Other changes

- Force-disable all analytics
- Use schildi.chat servers for rageshake bug reports and FCM push gateway server
- Streamlined settings framework to make settings-controlled features faster to implement
- Use [UnifiedPush's FOSS FCM distributor](https://github.com/UnifiedPush/android-foss_embedded_fcm_distributor) instead of bundling Google's closed source FCM libraries
- Misc bug fixes related to upstream bugs (if they're annoying me enough to not wait for Element to fix them. Usually we'll try to contribute these back upstream)


## Historic

- Render media captions ([MSC2530](https://github.com/matrix-org/matrix-spec-proposals/pull/2530)): adopted by upstream at v0.4.7
- Bigger emoji-only messages: upstreamed at v0.5.2
- Show avatar placeholders instead of blank space for avatars that failed to load
- Copy URLs to clipboard in text messages on longclick
- Copy user MXIDs to clipboard by long-pressing on the member-detail screen
- Suggest and record frequently used emoji reactions (synced with desktop clients via `io.element.recent_emoji` account data), adopted by upstream in v25.10.0
- Non-expanding compact app bar in the chat overview, adopted by upstream in v25.11.2


†: Can be partly enabled or disabled by user setting

- Optional flat message layout in Timeline settings: left-aligned messages beneath each sender’s name, without chat bubbles. Disabled by default.

- Text to emoji submenu in Timeline settings: define whole-token replacements using the emoji picker, add or delete rules, and prefix a token with a backslash to send it literally. Rules apply when sending, excluding code and links.

- Independent top and bottom bar opacity controls accompany wallpaper opacity in appearance settings. Chat and room-list bars reveal a continuous wallpaper underneath while controls remain opaque.
- An optional Timeline setting shows normal timestamps for sender/device mismatch warnings from bridges; other encryption warnings retain their existing display.

- Filter chip backgrounds and their scrolling container follow top-bar opacity. The Chat colours section adds one shared accent for new-message dividers, fading highlights and the typing cursor in both light and dark themes.

- Text replies offer a per-reply `@ On` / `@ Off` control (on by default), using `fi.mau.discord.reply_ping` for the Discord bridge while preserving deliberate mentions and thread references.

- Read receipt avatars omit presence indicators. The reply mention toggle overlays the preview below its close button without reserving message space.
