#!/bin/bash
# Program:
#	Build Android Alpha/Beta/Production version
# History:
# 2015/07/22 Abner first release
# Availalbe parameters:
#                      -b [alpha|beta|prod]   ==> build by type
#                      -c                     ==> clean
#                      -i                     ==> install apk

#Assign your ANT_HOME & SDK_ROOT here
#ANT_HOME="/Users/kelly79126/Desktop/apache-ant-1.9.6/bin/"
#SDK_ROOT="/Users/kelly79126/Desktop/sdk/"

ANT_HOME="../../../apache-ant-1.9.3/bin"
SDK_ROOT="../../../adt-bundle-mac-x86_64-20140702/sdk"

#Check if ANT_HOME exists
test ! -d "$ANT_HOME" && echo "Can't find ANT_HOME: $ANT_HOME" && exit 0

#Check if SDK_ROOT exists
test ! -d "$SDK_ROOT" && echo "Can't find SDK_ROOT: $SDK_ROOT" && exit 0

export ANDROID_HOME=$SDK_ROOT

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

  echo "Build $2 Version ..."

  #Check if last build is failed and restore AndroidManifest.xml
  test -e "AndroidManifest-backup.xml" && echo "AndroidManifest-backup.xml exist, restore it" && mv AndroidManifest-backup.xml AndroidManifest.xml && echo "Restore AndroidManifest.xml Done..."

  #Copy config files based on build type
  echo "Copy from config folder: $CFG_DIR"

  cp "$CFG_DIR"/ant.properties . && cp "$CFG_DIR"/beseye.release.keystore . && "$ANT_HOME"/ant clean  && "$ANT_HOME"/ant release "$BUILD_PRM" -Dsdk.dir=$ANDROID_HOME && echo "Build $2 Version Done ..." && exit 0

  echo "Build $2 Version Failed ..."

elif [ "$1" = "-c" ]; then
  "$ANT_HOME"/ant clean && echo "Clean Done..." && exit 0
elif [ "$1" = "-i" ]; then
  "$ANT_HOME"/ant installr && echo "Install $2 apk Done..." && exit 0
elif [ "$1" = "-h" ]; then
  echo "Availalbe parameters:"
  echo "                      -b [alpha|beta|prod]   ==> build by type"
  echo "                      -c                     ==> clean"
  echo "                      -i                     ==> install apk"
else
  echo "Invalid parameter !!!! -- $1" && exit 0
fi

