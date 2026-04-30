{ inputs, ... }:
{
  imports = [ inputs.treefmt-nix.flakeModule ];
  perSystem =
    {
      pkgs,
      ...
    }:
    {
      treefmt = {
        projectRootFile = "flake.nix";
        programs.dos2unix.enable = true;

        programs.nixfmt.enable = true;

        programs.ktfmt.enable = true;
        programs.google-java-format.enable = true;
        programs.xmllint.enable = true;
        programs.prettier.enable = true;
        programs.taplo.enable = true;
        programs.clang-format.enable = true;
        programs.clang-tidy.enable = true;
        programs.shfmt.enable = true;
        programs.shellcheck.enable = true;
        programs.mdformat.enable = true;
        programs.detekt = {
          enable = true;
          buildUponDefaultConfig = true;
          configFile = ./treefmt/detekt.yml;
        };

        programs.actionlint.enable = true;
        programs.yamlfmt.enable = true;
      };
    };
}
