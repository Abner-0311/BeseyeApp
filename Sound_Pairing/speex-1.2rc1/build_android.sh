#!/bin/bash

DIR_OGG=`cd ../libogg-1.3.0/build_android;pwd`
DEST=`pwd`/build_android && rm -rf $DEST

ANDROID_NDK=../../../android-ndk-r8e
TOOLCHAIN=/tmp/speex
SYSROOT=$TOOLCHAIN/sysroot/
$ANDROID_NDK/build/tools/make-standalone-toolchain.sh --system=darwin-x86_64 --platform=android-14 --install-dir=$TOOLCHAIN

export PATH=$TOOLCHAIN/bin:$PATH
export CC=arm-linux-androideabi-gcc
export CPP=arm-linux-androideabi-cpp
export CXX=arm-linux-androideabi-g++
export LD=arm-linux-androideabi-ld
export AR=arm-linux-androideabi-ar
export RANLIB=arm-linux-androideabi-ranlib
export NM=arm-linux-androideabi-nm

export CFLAGS="-I${DIR_OGG}/include"

export LDFLAGS="-L${DIR_OGG}/lib"

PREFIX="$DEST" && mkdir -p $PREFIX

./configure --prefix=$PREFIX --host=arm-linux --target=arm-linux --enable-shared=no

  make clean
  make -j4 || exit 1
  make install || exit 1


