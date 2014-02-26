#!/bin/bash

DIR_OPENSSL=`cd ../../../openssl-1.0.1e/build_android;pwd`
DIR_APP=`cd ../../../../BeseyeApp/jni/main;pwd`

DEST=`pwd`/build_android && rm -rf $DEST

ANDROID_NDK=../../../../../android-ndk-r8e
TOOLCHAIN=/tmp/librtmp
SYSROOT=$TOOLCHAIN/sysroot/
$ANDROID_NDK/build/tools/make-standalone-toolchain.sh --system=darwin-x86_64 --platform=android-14 --install-dir=$TOOLCHAIN

export PATH=$TOOLCHAIN/bin:$PATH

PREFIX="$DEST" && mkdir -p $PREFIX

make clean

make CROSS_COMPILE=arm-linux-androideabi- \
CRYPTO=OPENSSL \
INC="-I${DIR_OPENSSL}/include" \
XLDFLAGS="-L${DIR_OPENSSL}/lib" \
prefix=$PREFIX \
#LIBS_posix=-ldl \
SHARED=no

make install prefix=$PREFIX SHARED=no

cp -r $PREFIX/include ${DIR_APP}
cp $PREFIX/lib/*.a ${DIR_APP}

