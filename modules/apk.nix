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
    in
    {
      packages = rec {
        apk = inputs.gradle2nix.builders.${system}.buildGradlePackage rec {
          pname = "PosScanner";
          version = "0unstable";
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

          gradleBuildFlags = [ ":app:assembleDebug" ];
          preBuild = ''
            export ANDROID_SDK_ROOT="${sdk.androidSdk}/libexec/android-sdk";
          '';

          dontFixup = true;

          postInstall = ''
            mkdir -p $out
            cp app/build/outputs/apk/debug/app-debug.apk $out/${pname}_${version}_app-debug.apk
          '';
        };
        default = apk;
      };
    };
}
