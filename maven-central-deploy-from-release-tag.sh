#!/usr/bin/env bash

# Deploy maven artefact in current directory into Maven central repository
# using maven-release-plugin goals

read -p "Really deploy to maven cetral repository  (yes/no)? "

if ( [ "$REPLY" == "yes" ] ) then
  eval `ssh-agent`
  ssh-add ~/.ssh/id_rsa
  ssh-add -l
  mvn clean deploy -e -P sonatype-oss-release
  ssh-add -D
else
  echo 'Exit without deploy'
fi
