NDK=/usr/build/android-ndk-r8e_
SYSROOT=$NDK/platforms/android-9/arch-arm/
PREBUILT=$NDK/toolchains/arm-linux-androideabi-4.4.3/prebuilt/darwin-x86_64

./configure \
--enable-cross-compile \
--target-os=darwin \
--arch=i386 \
--cpu=i386 \
--enable-protocol=file  \
--arch=arm --cpu=armv7-a \
--enable-shared \
--sysroot=$SYSROOT \
--cc=$PREBUILT/bin/arm-linux-androideabi-gcc-4.4.3	 \
--enable-memalign-hack \
--disable-ffmpeg --disable-ffplay  --disable-ffserver \
--disable-doc \
--disable-ffprobe \
--disable-asm \
--disable-debug 
