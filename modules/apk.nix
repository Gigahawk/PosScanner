{ inputs, ... }:
{
  perSystem =
    {
      self',
      pkgs,
      system,
      ...
    }:
    let
      sdk = import ./_android-sdk.nix { inherit pkgs; };
      gradle = pkgs.gradle-packages.mkGradle {
        version = "9.3.1";
        hash = "sha256-smbV/2uQ6tptw7IMsJDjcxMC5VOifF0+TfHw12vq/wY";
        defaultJava = pkgs.jdk21;
        updateScriptMajorVersion = "9";
      };
      _version = inputs.gradle2nix.builders.${system}.buildGradlePackage rec {
        pname = "_version";
        version = "0unstable";
        src = ../src;
        lockFile = ../src/gradle.lock;
        inherit gradle;

        postPatch = ''
          export _BUILD_GRADLE_PATH="app/build.gradle.kts"

          cat <<'EOF' >> "$_BUILD_GRADLE_PATH"

          tasks.register("printVersionName") {
            val outputFile = layout.buildDirectory.file("version.txt")
            outputs.file(outputFile)

            doLast {
              val vName = android.defaultConfig.versionName
              val vCode = android.defaultConfig.versionCode
              val vText = "''${vName}-''${vCode}"

              println(vText)
              outputFile.get().asFile.writeText(vText)
            }
          }
          EOF
        '';

        gradleInstallFlags = [
          "printVersionName"
        ];

        postInstall = ''
          cp app/build/version.txt "$out"
        '';
      };
    in
    {
      packages = rec {
        # inherit _version;
        apk = inputs.gradle2nix.builders.${system}.buildGradlePackage rec {
          pname = "PosScanner";
          version = builtins.readFile _version;
          src = ../src;
          lockFile = ../src/gradle.lock;
          inherit gradle;

          postPatch = ''
            export _AAPT2_PATH=$(ls ${sdk.androidSdk}/libexec/android-sdk/build-tools/*/aapt2 | head -n1)
            export _GRADLE_PROPS_PATH="gradle.properties"

            echo "Using aapt2 from $_AAPT2_PATH"
            # Ensure we start a newline
            echo "" >> "$_GRADLE_PROPS_PATH"
            echo "android.aapt2FromMavenOverride=$_AAPT2_PATH" >> "$_GRADLE_PROPS_PATH"
          '';

          gradleBuildFlags = [
            ":app:assembleDebug"
            ":app:assembleRelease"
          ];
          preBuild = ''
            export ANDROID_SDK_ROOT="${sdk.androidSdk}/libexec/android-sdk";
          '';

          dontFixup = true;

          postInstall = ''
            mkdir -p $out
            cp app/build/outputs/apk/debug/app-debug.apk $out/${pname}_${version}_debug_unsigned.apk
            cp app/build/outputs/apk/release/app-release-unsigned.apk $out/${pname}_${version}_release_unsigned.apk
          '';
        };
        default = apk;
      };
    };
}
