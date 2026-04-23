## Setting up a new project

1. Enter the devshell with `nix develop`
2. Run `android-studio`
3. Click `New Project`
   - If you get an error about a missing/corrupted SDK, open an existing project
     first. See
     [this issue comment](https://github.com/NixOS/nixpkgs/issues/355045#issuecomment-2466910779)
     for details
4. Pick a template, then fill out the project details
   - Make sure the `Save location` is set to the `src` folder inside of this
     repo to avoid clutter (i.e `<repo_root>/src`). Ignore the warning about the
     directory not being empty.
5. Wait for the initial background tasks to complete
6. Try to build the app, if this fails with an error about the SDK directory not
   being writeable:
   1. Select `File > Project Structure`
   2. Take not of the `Gradle Version`
   3. Select the `Modules` tab
   4. `Compile Sdk Version` will probably be set to something invalid, open the
      dropdown and select the correct version
   5. `Build Tools Version` will probably be unset, open the dropdown and select
      something
   6. `NDK Version` will probably be unset, open the dropdown and select
      something
   7. Click `Apply` then `OK`
7. Wait for the Gradle sync to complete, then try building again
8. Run `./update_gradle_lock.sh` inside `src`
9. In `modules/apk.nix`, make sure the gradle version is set to the same thing
   shown inside Android Studio

## Signing release APKs

### Locally

Run `./build_signed.sh` from inside the devshell. Pass in arguments for an
`apksigner sign` call, see the
[apksigner docs](https://developer.android.com/tools/apksigner) for details
