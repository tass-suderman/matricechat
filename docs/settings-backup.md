# Settings backup

Open **Settings → Settings backup → Export settings** and save the ZIP in Downloads or another location outside the app's private storage.
A successful export displays “Settings exported successfully”. To restore a backup, choose
**Settings → Settings backup → Import settings** and select the ZIP. Restart the app after importing.
If restoring to a new installation, sign into your Matrix account. Additional accounts inherit their preferences when you sign into them.

Backups contain local app preferences, raw Turtle tweaks (including disabled overrides and light/dark colours), account preferences,
notification enablement and provider choices, labs, analytics consent, shake-to-report preferences, playback speed, selected spaces,
wallpaper (including its 0–100% opacity slider) and custom notification/call sounds. Matrix server settings, such as room notification rules and emoji/sticker packs, follow your Matrix account.

Sign-in tokens, push endpoints/client secrets, analytics identifiers, messages, encryption keys, PIN credentials, permission prompts and active
location-sharing state are not settings backups. Set up the device PIN and Android permissions again. Android-managed notification overrides
are not portable; settings selected inside the app, including custom sounds, are included.

## Compatibility contract

The ZIP contains `settings.json` with `format: "matrix-client-settings"`, `version: 1`, and a `stores` object. Each setting is stored under its
stable identifier with an explicit `type` and `value`. Scalar values are strings so 64-bit integers and ARGB values survive JSON tooling exactly.
Assets use the fixed ZIP entries `assets/wallpaper`, `assets/message_sound` and `assets/call_sound`; `assetMimeTypes` records their MIME types.
An explicit `type: "unset"` entry restores an optional setting to its default; a missing entry leaves its current value intact.
Optional `distributors` and `locale` fields hold push distributor selections and the Android per-app language.

The format identifier, version-1 reader, preference identifiers and account filename hashes must survive branding/package changes.
Add settings without bumping the format version. Missing settings retain current values/defaults; unknown stores, fields and value types are
ignored. Do not clear stores before importing. Removed enum choices fall back to current values, and integer tweaks are limited to their current ranges.
When changing a setting's identifier or type, add a migration from its old representation and retain regression fixtures for every previous format version.

Imports read and validate the entire archive and value types before changing settings. They merge preferences through the live DataStore instances,
including account stores that have not been opened yet. File references are rewritten to the receiving app's private paths and FileProvider authority.
The import finishes through activity recreation; failures during application restore the previous preferences and remove newly created assets.
Archive input is limited to 16 entries and 80 MiB uncompressed; the manifest is limited to 2 MiB. No ZIP entry path is extracted directly.
