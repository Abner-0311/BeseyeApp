#!/bin/bash
# Program:
#	Build Android Alpha/Beta/Production version
# History:
# 2015/07/22 Abner first release
# Availalbe parameters:
#                      -b [alpha|beta|prod]   ==> build by type
#                      -c                     ==> clean
#                      -i                     ==> install apk

#Assign your ANT_HOME here
ANT_HOME="../../../apache-ant-1.9.3/bin"

#Check if ANT_HOME exists
test ! -d "$ANT_HOME" && echo "Can't find ANT_HOME: $ANT_HOME" && exit 0

if [ "$1" = "-b" ]; then
  if [ "$2" = "alpha" ]; then
    CFG_DIR="config_alpha"
    BUILD_PRM="-DIs_Alpha=true"
  elif [ "$2" = "beta" ]; then
    CFG_DIR="config_beta"
    BUILD_PRM="-DIs_Beta=true"
  elif [ "$2" = "prod" ]; then
    CFG_DIR="config_release"
    BUILD_PRM=""
  else
    echo "Invalid Build type !!!! -- $2" && exit 0
  fi
elif [ "$1" = "-c" ]; then
  "$ANT_HOME"/ant clean && echo "Clean Done..." && exit 0
elif [ "$1" = "-i" ]; then
  "$ANT_HOME"/ant installr && echo "Install $2 apk Done..." && exit 0
else
  echo "Invalid parameter !!!! -- $1" && exit 0
fi

echo "Build $2 Version ..."

#Check if last build is failed and restore AndroidManifest.xml
test -e "AndroidManifest-backup.xml" && echo "AndroidManifest-backup.xml exist, restore it" && mv AndroidManifest-backup.xml AndroidManifest.xml && echo "Restore AndroidManifest.xml Done..."

#Copy config files based on build type
echo "Copy from config folder: $CFG_DIR"

cp "$CFG_DIR"/ant.properties . && cp "$CFG_DIR"/beseye.release.keystore . && "$ANT_HOME"/ant clean  && "$ANT_HOME"/ant release "$BUILD_PRM" && echo "Build $2 Version Done ..." && exit 0

echo "Build $2 Version Failed ..."
