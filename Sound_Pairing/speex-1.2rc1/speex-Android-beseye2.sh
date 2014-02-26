#!/bin/bash

DEST=`pwd`/build && rm -rf $DEST
SOURCE=`pwd`

ANDROID_NDK=/usr/build/android-ndk-r7
TOOLCHAIN=/tmp/speex
SYSROOT=$TOOLCHAIN/sysroot/
$ANDROID_NDK/build/tools/make-standalone-toolchain.sh --platform=android-14 --install-dir=$TOOLCHAIN

export PATH=$TOOLCHAIN/bin:$PATH
export CC="ccache arm-linux-androideabi-gcc"
export CPP=arm-linux-androideabi-cpp
export CXX=arm-linux-androideabi-g++
export LD=arm-linux-androideabi-ld
export AR=arm-linux-androideabi-ar
export RANLIB=arm-linux-androideabi-ranlib
export NM=arm-linux-androideabi-nm


PREFIX=/usr
DEST=`pwd`/build && rm -rf $DEST

export CFLAGS="-I/Users/Abner/Documents/Develop/libogg-1.3.1/build/armv7/include" 

export LDFLAGS="-L/Users/Abner/Documents/Develop/libogg-1.3.1/build/armv7/lib"



./configure --prefix=$DEST --host=arm-linux --target=arm-linux --enable-shared=no 

  make clean
  make -j4 || exit 1
  make install || exit 1


