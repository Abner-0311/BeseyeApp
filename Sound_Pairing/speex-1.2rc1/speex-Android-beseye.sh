#!/bin/bash

NDK=/usr/build/android-ndk-r7
PREBUILT=$NDK/toolchains/arm-linux-androideabi-4.4.3/prebuilt/
# 系统库目录
PLATFORM=$NDK/platforms/android-8/arch-arm/
# 编译的目录目录
PREFIX=/Users/Abner/Documents/Develop/speex-1.2rc1


export CC=$PREBUILT/darwin-x86/bin/arm-linux-androideabi-gcc       
export CPP=$PREBUILT/darwin-x86/bin/arm-linux-androideabi-cpp
export CXX=$PREBUILT/darwin-x86/bin/arm-linux-androideabi-g++
export LDFLAGS=--sysroot=$NDK/platforms/android-8/arch-arm/ -L$NDK/platforms/android-8/arch-arm/usr/lib
export CFLAGS=--sysroot=$NDK/platforms/android-8/arch-arm/
export AR=$PREBUILT/darwin-x86/bin/arm-linux-androideabi-ar
export RANLIB=$PREBUILT/darwin-x86/bin/arm-linux-androideabi-ranlib
export PATH=$PREBUILT/darwin-x86/bin/:$PATH


sudo ./configure --prefix=$PREFIX --host=arm-linux --target=arm-linux --enable-shared=no && cd ./libspeex && make && sudo make install   

