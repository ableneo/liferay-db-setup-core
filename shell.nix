{ pkgs ? import <nixpkgs> {} }:

(pkgs.buildFHSUserEnv {
  name = "db-setup-core";
  targetPkgs = pkgs: (with pkgs; [ jdk maven gitAndTools.gitFull bash-completion bash]) ++ (with pkgs.xorg; []);
  multiPkgs = pkgs: (with pkgs; [ ]);
  runScript = ''
    env SHELL_NAME="db-setup-core" bash --rcfile <(cat /home/ktor/.bashrc; echo 'source "${pkgs.gitAndTools.gitFull}/share/git/contrib/completion/git-completion.bash"; source "${pkgs.gitAndTools.gitFull}/share/git/contrib/completion/git-prompt.sh";')
  '';
}).env
