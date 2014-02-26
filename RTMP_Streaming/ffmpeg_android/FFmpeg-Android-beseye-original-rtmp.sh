#!/bin/bash

#DEST=`pwd`/build/ffmpeg && rm -rf $DEST
DEST=`pwd`/build/ffmpeg-rtmp

SOURCE=`pwd`/ffmpeg

if [ -d ffmpeg ]; then
cd ffmpeg
else
git clone git://github.com/FFmpeg/FFmpeg.git ffmpeg
cd ffmpeg
fi

#git reset --hard
#git clean -f -d
#git checkout `cat ../ffmpeg-version`
#patch -p1 <../FFmpeg-VPlayer.patch
#[ $PIPESTATUS == 0 ] || exit 1

#git log --pretty=format:%H -1 > ../ffmpeg-version

ANDROID_NDK=/usr/build/android-ndk-r8e_
TOOLCHAIN=/tmp/beseye
SYSROOT=$TOOLCHAIN/sysroot/
$ANDROID_NDK/build/tools/make-standalone-toolchain.sh --system=darwin-x86_64 --platform=android-14 --install-dir=$TOOLCHAIN

export PATH=$TOOLCHAIN/bin:$PATH
export CC=arm-linux-androideabi-gcc
export CXX=arm-linux-androideabi-g++
export LD=arm-linux-androideabi-ld
export AR=arm-linux-androideabi-ar
export RANLIB=arm-linux-androideabi-ranlib
export NM=arm-linux-androideabi-nm

CFLAGS="-I/Users/Abner/Documents/Develop/speex-1.2rc1/build/include \
-I/Users/Abner/Documents/Develop/rtmpdump-2.3_2/librtmp/build/include \
-I/Users/Abner/Documents/Develop/openssl-1.0.1e/build/include \
-O3 -Wall -mthumb -pipe -fpic -fasm \
-finline-limit=300 -ffast-math \
-fstrict-aliasing -Werror=strict-aliasing \
-fmodulo-sched -fmodulo-sched-allow-regmoves \
-Wno-psabi -Wa,--noexecstack \
-D__ARM_ARCH_5__ -D__ARM_ARCH_5E__ -D__ARM_ARCH_5T__ -D__ARM_ARCH_5TE__ \
-DANDROID -DNDEBUG"

LDFLAGS="-L/Users/Abner/Documents/Develop/speex-1.2rc1/build/lib \
-L/Users/Abner/Documents/Develop/rtmpdump-2.3_2/librtmp/build/lib \
-L/Users/Abner/Documents/Develop/openssl-1.0.1e/build/lib"

FFMPEG_FLAGS="--target-os=linux \
--arch=arm \
--enable-cross-compile \
--cross-prefix=arm-linux-androideabi- \
--enable-shared \
--disable-symver \
--disable-doc \
--disable-ffmpeg \
--disable-ffplay \
--disable-ffprobe \
--disable-ffserver \
--disable-avfilter \
--disable-swscale \
--disable-swresample \
--enable-protocols \
--enable-parsers \
--enable-demuxers \
--enable-decoders \
--enable-bsfs \
--enable-network \
--disable-demuxer=sbg \
--disable-demuxer=dts \
--disable-parser=dca \
--disable-decoder=dca \
--enable-asm \
--enable-version3"

for version in armv7; do

cd $SOURCE

case $version in
neon)
EXTRA_CFLAGS="-march=armv7-a -mfpu=neon -mfloat-abi=softfp -mvectorize-with-neon-quad"
EXTRA_LDFLAGS="-Wl,--fix-cortex-a8"
;;
armv7)
EXTRA_CFLAGS="-march=armv7-a -mfpu=vfpv3-d16 -mfloat-abi=softfp"
EXTRA_LDFLAGS="-Wl,--fix-cortex-a8"
;;
vfp)
EXTRA_CFLAGS="-march=armv6 -mfpu=vfp -mfloat-abi=softfp"
EXTRA_LDFLAGS=""
;;
armv6)
EXTRA_CFLAGS="-march=armv6"
EXTRA_LDFLAGS=""
;;
*)
EXTRA_CFLAGS=""
EXTRA_LDFLAGS=""
;;
esac

PREFIX="$DEST/$version" && mkdir -p $PREFIX
FFMPEG_FLAGS="$FFMPEG_FLAGS --prefix=$PREFIX"

./configure $FFMPEG_FLAGS --extra-cflags="$CFLAGS $EXTRA_CFLAGS" --extra-ldflags="$LDFLAGS $EXTRA_LDFLAGS" | tee $PREFIX/configuration.txt
cp config.* $PREFIX
[ $PIPESTATUS == 0 ] || exit 1

make clean
make -j4 || exit 1
make install || exit 1

rm libavcodec/inverse.o
#$CC -lm -lz -shared --sysroot=$SYSROOT -Wl,--no-undefined -Wl,-z,noexecstack $LDFLAGS $EXTRA_LDFLAGS libavutil/*.o libavutil/arm/*.o libavcodec/*.o libavcodec/arm/*.o libavformat/*.o libswresample/*.o libswscale/*.o -lm -lspeex -o $PREFIX/libffmpeg.so

#$CC -lm -lz -shared --sysroot=$SYSROOT -Wl,--no-undefined -Wl,-z,noexecstack $LDFLAGS -L$PREFIX/lib $EXTRA_LDFLAGS -lswresample -lavdevice -lavfilter -lavformat -lavcodec -lswscale -lavutil -lm -lspeex -lrtmp -lssl -lcrypto -o $PREFIX/libffmpeg.so

#cp $PREFIX/libffmpeg.so $PREFIX/libffmpeg-debug.so
#arm-linux-androideabi-strip --strip-unneeded $PREFIX/libffmpeg.so

done
