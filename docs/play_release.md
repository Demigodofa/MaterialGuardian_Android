# Play Release Notes

Material Guardian supports release signing through either:

- environment variables: `MG_STORE_FILE`, `MG_STORE_PASSWORD`, `MG_KEY_ALIAS`, `MG_KEY_PASSWORD`
- a local repo file: `release-signing.properties`

Do not commit real signing material. The repo ignores:

- `release-signing.properties`
- `keystore/`

Recommended flow:

1. Copy `release-signing.sample.properties` to `release-signing.properties`.
2. Create an upload keystore, for example:

```powershell
keytool -genkeypair -v -keystore keystore\material-guardian-upload.jks -alias upload -keyalg RSA -keysize 4096 -validity 10000
```

3. Fill in the real values in `release-signing.properties`.
4. Build the Play bundle:

```powershell
.\gradlew.bat bundleRelease
```

5. Upload `app/build/outputs/bundle/release/app-release.aab` to Play Console and enroll in Play App Signing.

Versioning and naming standard:

- `versionName` should use `major.minor.patch` with no `v` prefix, for example `1.0.2`.
- `versionCode` should increase on every Play-uploaded Android bundle.
- Play Console release names should use `major.minor.patch (build) - summary`, for example `1.0.2 (3) - Export Fixes`.
- For internal-only releases, you may include the track for clarity: `1.0.2 (3) Internal - Export Fixes`.
- Keep release notes short and tester-focused; keep release names short and operational.
