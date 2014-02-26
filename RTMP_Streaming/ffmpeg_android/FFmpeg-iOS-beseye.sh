#!/bin/sh
#install yasm first https://trac.ffmpeg.org/wiki/MacOSXCompilationGuide
#download gas-preprocessor.pl from https://github.com/yuvi/gas-preprocessor, then copy it to /usr/bin

IOS_LIBRTMP=`cd ../rtmpdump-2.3_2;pwd`
DIR_OPENSSL=`cd ../openssl-1.0.1e/build_ios;pwd`
rm ffmpeg/config.h

FFMPEG_FLAGS2="--enable-cross-compile --disable-debug --disable-ffmpeg \
--disable-ffplay --disable-ffprobe --disable-ffserver \
--disable-doc --disable-encoders --disable-muxers \
--disable-bsfs --disable-devices --disable-filters --enable-pic"

FFMPEG_FLAGS="--enable-cross-compile \
--disable-symver \
--disable-doc \
--disable-neon \
--disable-armv5te \
--disable-ffmpeg \
--disable-ffplay \
--disable-ffprobe \
--disable-ffserver \
--disable-avdevice \
--enable-swscale \
--enable-avformat \
--enable-network \
--disable-encoders \
--enable-encoder=aac \
--enable-encoder=pcm_alaw \
--enable-encoder=pcm_mulaw \
--enable-encoder=adpcm_swf \
--disable-decoders \
--enable-decoder=aac \
--enable-decoder=pcm_alaw \
--enable-decoder=pcm_mulaw \
--enable-decoder=pcm_s16le \
--enable-decoder=adpcm_swf \
--enable-decoder=h264 \
--disable-parsers \
--enable-parser=aac \
--enable-parser=h264 \
--disable-demuxers \
--enable-demuxer=flv \
--enable-demuxer=pcm_s16le \
--enable-demuxer=pcm_mulaw \
--enable-demuxer=rtsp \
--disable-muxers \
--enable-muxer=flv \
--enable-protocols \
--disable-protocol=md5 \
--disable-protocol=mmsh \
--disable-protocol=mmst \
--disable-protocol=mmsu \
--disable-protocol=hls \
--disable-protocol=gopher \
--disable-protocol=concat \
--disable-filters \
--enable-filter=abuffersink \
--enable-filter=anull \
--enable-filter=anullsink \
--enable-filter=anullsrc \
--enable-filter=aformat \
--enable-filter=aresample \
--disable-bsfs \
--enable-rdft \
--enable-asm \
--enable-version3 \
--enable-librtmp"


LIBS="libavcodec libavformat libavutil libswscale libavfilter"

ARCHS="armv7 armv7s arm64 i386 x86_64"
#ARCHS="arm64"

# directories
SOURCE="ffmpeg"
FAT="fat"

SCRATCH="scratch"
# must be an absolute path
THIN=`pwd`/"thin"

COMPILE="y"
LIPO="y"

if [ "$*" ]
then
if [ "$*" = "lipo" ]
then
# skip compile
COMPILE=
else
ARCHS="$*"
if [ $# -eq 1 ]
then
# skip lipo
LIPO=
fi
fi
fi

if [ "$COMPILE" ]
then
CWD=`pwd`
for ARCH in $ARCHS
do
echo "building $ARCH..."
mkdir -p "$SCRATCH/$ARCH"
cd "$SCRATCH/$ARCH"

if [ "$ARCH" = "i386" -o "$ARCH" = "x86_64" ]
then
PLATFORM="iPhoneSimulator"
CPU=
if [ "$ARCH" = "x86_64" ]
then
SIMULATOR="-mios-simulator-version-min=7.0"
else
SIMULATOR="-mios-simulator-version-min=5.0"
fi
else
PLATFORM="iPhoneOS"
if [ $ARCH = "armv7s" ]
then
CPU="--cpu=swift"
else
CPU=
fi
SIMULATOR=
fi

XCRUN_SDK=`echo $PLATFORM | tr '[:upper:]' '[:lower:]'`
CC="xcrun -sdk $XCRUN_SDK clang"
CFLAGS="-arch $ARCH $SIMULATOR -I${IOS_LIBRTMP}/include \
-I${DIR_OPENSSL}/include -no-integrated-as"
CXXFLAGS="$CFLAGS"
LDFLAGS="$CFLAGS -L${IOS_LIBRTMP}/lib \
-L-I${DIR_OPENSSL}/lib"

$CWD/$SOURCE/configure \
--target-os=darwin \
--arch=$ARCH \
--cc="$CC" \
$FFMPEG_FLAGS \
--extra-cflags="$CFLAGS" \
--extra-cxxflags="$CXXFLAGS" \
--extra-ldflags="$LDFLAGS" \
$CPU \
--prefix="$THIN/$ARCH"

make -j3 install
cd $CWD
done
fi

if [ "$LIPO" ]
then
echo "building fat binaries..."
mkdir -p $FAT/lib
set - $ARCHS
CWD=`pwd`
cd $THIN/$1/lib
for LIB in *.a
do
cd $CWD
lipo -create `find $THIN -name $LIB` -output $FAT/lib/$LIB
done

cd $CWD
cp -rf $THIN/$1/include $FAT
fi
