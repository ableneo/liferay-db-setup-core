#
# The Nix configuration allows to start up a shell with complete project environment (required tools).
# The environment allows for building and contribution to the repository.
#
# The environment is local- does not mess with the global configuration of your PC.
#
# 1. To start you need to install Nix first: https://nixos.org/guides/install-nix.html
# 2. Next step is to start up the project environment with a command:
#        nix-shell
#
# Project environment consists of:
#  - JDK8 package that is used by Liferay
#  - maven
#  - git
#  - bash shell with command completion
#
{ pkgs ? import <nixpkgs> { overlays = [( self: super:
{
    maven = super.maven.override {
        jdk = self.adoptopenjdk-hotspot-bin-8; # use jdk8 with maven
    };
}
)]; } }:

(pkgs.buildFHSUserEnv {
    name = "db-setup-core";
    targetPkgs = pkgs: (with pkgs; [ adoptopenjdk-hotspot-bin-8 maven gitAndTools.gitFull bash-completion bash]) ++ (with pkgs.xorg; []);
    multiPkgs = pkgs: (with pkgs; [ ]);
    runScript = ''
        env SHELL_NAME="db-setup-core" bash --rcfile <(cat ~/.bashrc; echo 'source "${pkgs.gitAndTools.gitFull}/share/git/contrib/completion/git-completion.bash"; source "${pkgs.gitAndTools.gitFull}/share/git/contrib/completion/git-prompt.sh";')
    '';
}).env
