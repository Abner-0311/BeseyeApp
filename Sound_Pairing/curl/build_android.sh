#!/bin/bash

DEST=`pwd`/build_android && rm -rf $DEST

ANDROID_NDK=../../../android-ndk-r8e
TOOLCHAIN=/tmp/curl
SYSROOT=$TOOLCHAIN/sysroot/
$ANDROID_NDK/build/tools/make-standalone-toolchain.sh --system=darwin-x86_64 --platform=android-14 --install-dir=$TOOLCHAIN

export PATH=$TOOLCHAIN/bin:$PATH
export CC=arm-linux-androideabi-gcc
export CXX=arm-linux-androideabi-g++
export LD=arm-linux-androideabi-ld
export AR=arm-linux-androideabi-ar
export RANLIB=arm-linux-androideabi-ranlib
export NM=arm-linux-androideabi-nm

PREFIX="$DEST" && mkdir -p $PREFIX

./Configure --host=arm-linux-androideabi --prefix=$PREFIX

make clean
make -j4
make install