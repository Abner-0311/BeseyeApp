#!/bin/bash

###########################################################################
#  Choose your ffmpeg version and your currently-installed iOS SDK version:
#
VERSION="2.1"
SDKVERSION="7.1"


#echo "install gas-* perl script"

#echo "install gas-preproccesor.pr"
#git clone git://github.com/mansr/gas-preprocessor.git

#echo "copy gas-preprocessor.pl to /usr/bin"
#sudo cp -f gas-preprocessor/gas-preprocessor.pl /usr/local/bin/

#echo "set execute right"
#chmod +x /usr/local/bin/gas-preprocessor.pl

#echo "install finished."

IOS_LIBRTMP=`cd ../rtmpdump-2.3_2;pwd`
DIR_OPENSSL=`cd ../openssl-1.0.1e/build_ios;pwd`

FFMPEG_FLAGS="--enable-cross-compile \
--disable-symver \
--disable-doc \
--disable-neon \
--disable-armv5te \
--disable-armv6 \
--disable-armv6t2 \
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


#
#
###########################################################################
#
# Don't change anything under this line!
#
###########################################################################

# No need to change this since xcode build will only compile in the
# necessary bits from the libraries we create
ARCHS="armv7 armv7s i386"

DEVELOPER=`xcode-select -print-path`

cd "`dirname \"$0\"`"
REPOROOT=$(pwd)

# Where we'll end up storing things in the end
OUTPUTDIR="${REPOROOT}/dependencies"
mkdir -p ${OUTPUTDIR}/include
mkdir -p ${OUTPUTDIR}/lib
mkdir -p ${OUTPUTDIR}/bin


BUILDDIR="${REPOROOT}/build"
mkdir -p $BUILDDIR

# where we will keep our sources and build from.
#SRCDIR="${BUILDDIR}/src"
#mkdir -p $SRCDIR
# where we will store intermediary builds
INTERDIR="${BUILDDIR}/built"
mkdir -p $INTERDIR

########################################

cd ffmpeg

# Exit the script if an error happens
#set -e

#if [ ! -e "${SRCDIR}/ffmpeg-${VERSION}.tar.bz2" ]; then
#echo "Downloading ffmpeg-${VERSION}.tar.bz2"
#curl -LO http://ffmpeg.org/releases/ffmpeg-${VERSION}.tar.bz2
#else
#echo "Using ffmpeg-${VERSION}.tar.bz2"
#fi

#tar zxf ffmpeg-${VERSION}.tar.bz2 -C $SRCDIR
#cd "${SRCDIR}/ffmpeg-${VERSION}"

set +e # don't bail out of bash script if ccache doesn't exist
CCACHE=`which ccache`
if [ $? == "0" ]; then
echo "Building with ccache: $CCACHE"
CCACHE="${CCACHE} "
else
echo "Building without ccache"
CCACHE=""
fi
set -e # back to regular "bail out on error" mode

for ARCH in ${ARCHS}
do
if [ "${ARCH}" == "i386" ];
then
PLATFORM="iPhoneSimulator"
EXTRA_CONFIG="--arch=i386 --disable-asm --enable-cross-compile --target-os=darwin --cpu=i386"
EXTRA_CFLAGS="-arch i386 -I${IOS_LIBRTMP}/include -I${DIR_OPENSSL}/include -no-integrated-as"
EXTRA_LDFLAGS="-I${DEVELOPER}/Platforms/${PLATFORM}.platform/Developer/SDKs/${PLATFORM}${SDKVERSION}.sdk/usr/lib -mfpu=neon -L${IOS_LIBRTMP}/lib -L${DIR_OPENSSL}/lib"

XCRUN_SDK=`echo $PLATFORM | tr '[:upper:]' '[:lower:]'`
CC="xcrun -sdk $XCRUN_SDK clang"

else
PLATFORM="iPhoneOS"

if [ "${ARCH}" == "arm64" ];
then
EXTRA_CONFIG="--arch=arm64 --target-os=darwin --enable-cross-compile --disable-armv5te"
else
EXTRA_CONFIG="--arch=arm --target-os=darwin --enable-cross-compile --cpu=cortex-a9 --disable-armv5te"
fi

EXTRA_CFLAGS="-w -arch ${ARCH} -mfpu=neon -I${IOS_LIBRTMP}/include -I${DIR_OPENSSL}/include -no-integrated-as"
EXTRA_LDFLAGS="-mfpu=neon -L${IOS_LIBRTMP}/lib -L${DIR_OPENSSL}/lib"

XCRUN_SDK=`echo $PLATFORM | tr '[:upper:]' '[:lower:]'`
CC="xcrun -sdk $XCRUN_SDK clang"

fi

mkdir -p "${INTERDIR}/${ARCH}"

./configure --prefix="${INTERDIR}/${ARCH}" $FFMPEG_FLAGS --sysroot="${DEVELOPER}/Platforms/${PLATFORM}.platform/Developer/SDKs/${PLATFORM}${SDKVERSION}.sdk" --cc="$CC" --as='/usr/local/bin/gas-preprocessor.pl /Applications/XCode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/clang' --extra-cflags="${EXTRA_CFLAGS} -miphoneos-version-min=${SDKVERSION} -I${OUTPUTDIR}/include" --extra-ldflags="-arch ${ARCH} ${EXTRA_LDFLAGS} -isysroot /Applications/Xcode.app/Contents/Developer/Platforms/${PLATFORM}.platform/Developer/SDKs/${PLATFORM}${SDKVERSION}.sdk -miphoneos-version-min=${SDKVERSION} -L${OUTPUTDIR}/lib" ${EXTRA_CONFIG} --enable-pic --extra-cxxflags="$CPPFLAGS -I${OUTPUTDIR}/include -isysroot ${DEVELOPER}/Platforms/${PLATFORM}.platform/Developer/SDKs/${PLATFORM}${SDKVERSION}.sdk"

make clean
make && make install && make clean

done

mkdir -p "${INTERDIR}/universal/lib"

cd "${INTERDIR}/armv7/lib"
for file in *.a
do

cd ${INTERDIR}
xcrun -sdk iphoneos lipo -output universal/lib/$file  -create -arch armv7 armv7/lib/$file -arch armv7s armv7s/lib/$file -arch i386 i386/lib/$file
echo "Universal $file created."

done
cp -r ${INTERDIR}/armv7/include ${INTERDIR}/universal/

echo "Done."
