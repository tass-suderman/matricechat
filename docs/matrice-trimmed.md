# Trimmed MatriceChat build

Pass `-PmatriceTrimmed=true` to Gradle to build without calling, location sharing/maps or polls, without the bundled emoji font, and with only English and French translations. The flag defaults to **false**; normal builds and CI keep the full feature set.

The feature implementation modules and native call/map dependencies are excluded from the app. Small disabled bindings keep shared navigation and services working. Calls do not ring, location sharing is unavailable, and poll creation/history are hidden. Received polls and call/live-location events display as unsupported events; static locations retain their text without a map. Android supplies emoji glyphs, so emoji coverage depends on the device.

The trimmed build uses the same application ID and signing configuration, so it can update the full build without clearing accounts or preferences. Preferences for excluded features are retained for a later full build. When installing an update, use a `-PmatriceVersionCode` greater than the installed build's value.

The shared Matrix SDK still understands these event types. This option does not rebuild or strip features from the Rust SDK itself.
