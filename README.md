## Setting up a new project

1. Click `New Project`
   - If you get an error about a missing/corrupted SDK, open an existing project
     first. See
     [this issue comment](https://github.com/NixOS/nixpkgs/issues/355045#issuecomment-2466910779)
     for details
2. Pick a template, then fill out the project details
   - Make sure the `Save location` is set to the `src` folder inside of this
     repo to avoid clutter (i.e `<repo_root>/src`). Ignore the warning about the
     directory not being empty.
3. Wait for the initial background tasks to complete
4. Try to build the app, if this fails with an error about the SDK directory not
   being writeable:
   1. Select `File > Project Structure`
   2. Select the `Modules` tab
   3. `Compile Sdk Version` will probably be set to something invalid, open the
      dropdown and select the correct version
   4. `Build Tools Version` will probably be unset, open the dropdown and select
      something
   5. `NDK Version` will probably be unset, open the dropdown and select
      something
   6. Click `Apply` then `OK`
5. Wait for the Gradle sync to complete, then try building again
