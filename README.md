## Setting up a new project

1. Enter the devshell with `nix develop`
1. Run `android-studio`
1. Click `New Project`
   - If you get an error about a missing/corrupted SDK, open an existing project
     first. See
     [this issue comment](https://github.com/NixOS/nixpkgs/issues/355045#issuecomment-2466910779)
     for details
1. Pick a template, then fill out the project details
   - Make sure the `Save location` is set to the `src` folder inside of this
     repo to avoid clutter (i.e `<repo_root>/src`). Ignore the warning about the
     directory not being empty.
1. Wait for the initial background tasks to complete
1. Try to build the app, if this fails with an error about the SDK directory not
   being writeable:
   1. Select `File > Project Structure`
   1. Take not of the `Gradle Version`
   1. Select the `Modules` tab
   1. `Compile Sdk Version` will probably be set to something invalid, open the
      dropdown and select the correct version
   1. `Build Tools Version` will probably be unset, open the dropdown and select
      something
   1. `NDK Version` will probably be unset, open the dropdown and select
      something
   1. Click `Apply` then `OK`
1. Wait for the Gradle sync to complete, then try building again
1. Run `./update_gradle_lock.sh` inside `src`
   - Re-run this any time you update your gradle dependencies
1. In `modules/apk.nix`, make sure the gradle version is set to the same thing
   shown inside Android Studio

## Signing release APKs

### Generating a Keystore

Run:

```bash
keytool \
    -genkeypair \
    -v \
    -keystore [KEYSTORENAME].jks \
    -alias [KEYNAME] \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000
```

Notes:

- You can pick anything for FILENAME and KEYSTORENAME
- Minimum keysize is 2048, use 4096 for extra security
- Validity is in days. 10000 days is approximately 27 years

### Locally

Run `./build_signed.sh` from inside the devshell. Pass in arguments for an
`apksigner sign` call, see the
[apksigner docs](https://developer.android.com/tools/apksigner) for details

### Via GitHub Actions

1. On the repo page, navigate to the `Settings` tab
1. Select `Secrets and variables > Actions`
1. For each of the following, click `New repository secret`, then fill in:
   - Name: `KEYSTORE_B64`, Secret: the output of
     `base64 -w 0 [KEYSTORENAME].jks`
   - Name: `KEYSTORE_PASSWORD`, Secret: your keystore password
   - Name: `KEY_ALIAS`, Secret: your `[KEYNAME]`
   - Name: `KEY_PASSWORD`, Secret: your key password (by default this is the
     same as the keystore password)
1. Push a tag to GitHub, wait for the release to be generated
   - By default, the release will be created as a prerelease. Manually mark as a
     full release once final testing is complete.
