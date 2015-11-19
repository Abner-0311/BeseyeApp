#!/bin/sh

# directories
SOURCE="ffmpeg"
FAT="FFmpeg-iOS"

SCRATCH="scratch"
# must be an absolute path
THIN=`pwd`/"thin"

# absolute path to x264 library
#X264=`pwd`/fat_x264

#CONFIGURE_FLAGS="--enable-cross-compile --disable-debug --disable-programs \
#                 --disable-doc --enable-pic"

#if [ "$X264" ]
#then
#	CONFIGURE_FLAGS="$CONFIGURE_FLAGS --enable-gpl --enable-libx264"
#fi

IOS_LIBRTMP=`cd ../rtmpdump-2.3_2;pwd`
DIR_OPENSSL=`cd ../openssl-1.0.1h/build_ios;pwd`

CONFIGURE_FLAGS="--enable-cross-compile \
--disable-symver \
--disable-doc \
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
--enable-vda \
--enable-hwaccel=h264 \
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
--disable-protocol=hls \
--disable-protocol=gopher \
--disable-protocol=concat \
--disable-filters \
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

# avresample
#CONFIGURE_FLAGS="$CONFIGURE_FLAGS --enable-avresample"

ARCHS="armv7 arm64 armv7s i386 x86_64"
# x86_64

COMPILE="y"
LIPO="y"

DEPLOYMENT_TARGET="7.0"

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
	if [ ! `which yasm` ]
	then
		echo 'Yasm not found'
		if [ ! `which brew` ]
		then
			echo 'Homebrew not found. Trying to install...'
			ruby -e "$(curl -fsSL https://raw.github.com/Homebrew/homebrew/go/install)" \
				|| exit 1
		fi
		echo 'Trying to install Yasm...'
		brew install yasm || exit 1
	fi
	if [ ! `which gas-preprocessor.pl` ]
	then
		echo 'gas-preprocessor.pl not found. Trying to install...'
		(curl -3L https://github.com/libav/gas-preprocessor/raw/master/gas-preprocessor.pl \
			-o /usr/local/bin/gas-preprocessor.pl \
			&& chmod +x /usr/local/bin/gas-preprocessor.pl) \
			|| exit 1
	fi

#if [ ! -r $SOURCE ]
#	then
#		echo 'FFmpeg source not found. Trying to download...'
#		curl http://www.ffmpeg.org/releases/$SOURCE.tar.bz2 | tar xj \
#			|| exit 1
#	fi

	CWD=`pwd`
	for ARCH in $ARCHS
	do
		echo "building $ARCH..."
		mkdir -p "$SCRATCH/$ARCH"
		cd "$SCRATCH/$ARCH"

        CFLAGS="-arch $ARCH"

#remove  -no-integrated-as for ios 8.4

		if [ "$ARCH" = "i386" -o "$ARCH" = "x86_64" ]
		then
		    PLATFORM="iPhoneSimulator"
            CFLAGS="$CFLAGS -miphoneos-version-min=$DEPLOYMENT_TARGET -I${IOS_LIBRTMP}/include -I${DIR_OPENSSL}/include"
		else
		    PLATFORM="iPhoneOS"
            CFLAGS="$CFLAGS -miphoneos-version-min=$DEPLOYMENT_TARGET -I${IOS_LIBRTMP}/include -I${DIR_OPENSSL}/include"
		    if [ "$ARCH" = "arm64" ]
		    then
		        EXPORT="GASPP_FIX_XCODE5=1"
		    fi
		fi

	CPU=
	#if [ $ARCH = "armv7s" ]
	#then
	#CPU="--cpu=cortex-a9"
	#fi

	if [ $ARCH = "armv7" ]
then
CPU="--cpu="
fi

		XCRUN_SDK=`echo $PLATFORM | tr '[:upper:]' '[:lower:]'`
		CC="xcrun -sdk $XCRUN_SDK clang"
		CXXFLAGS="$CFLAGS"
        LDFLAGS="$CFLAGS -L${IOS_LIBRTMP}/lib -L-I${DIR_OPENSSL}/lib -mfpu=neon"
#		if [ "$X264" ]
#		then
#			CFLAGS="$CFLAGS -I$X264/include"
#			LDFLAGS="$LDFLAGS -L$X264/lib"
#		fi

		$CWD/$SOURCE/configure \
		    --target-os=darwin \
		    --arch=$ARCH \
		    --cc="$CC" \
		    $CONFIGURE_FLAGS \
            $CPU \
		    --extra-cflags="$CFLAGS" \
		    --extra-cxxflags="$CXXFLAGS" \
		    --extra-ldflags="$LDFLAGS" \
		    --prefix="$THIN/$ARCH" \
		|| exit 1

		make -j3 install $EXPORT || exit 1
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
		echo lipo -create `find $THIN -name $LIB` -output $FAT/lib/$LIB 1>&2
		lipo -create `find $THIN -name $LIB` -output $FAT/lib/$LIB || exit 1
	done

	cd $CWD
	cp -rf $THIN/$1/include $FAT
fi

echo Done
