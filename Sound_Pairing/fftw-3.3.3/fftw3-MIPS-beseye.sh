#!/bin/bash

DEST=/opt/tftpboot/fftw3

SOURCE=`pwd`

TOOLCHAIN=/opt/buildroot-gcc342

export PATH=$TOOLCHAIN/bin:$PATH
export CC=mipsel-linux-gcc
export CXX=mipsel-linux-g++
export LD=mipsel-linux-ld
export AR=mipsel-linux-ar
export RANLIB=mipsel-linux-ranlib
export NM=mipsel-linux-nm
export STRIP=mipsel-linux-strip

CFLAGS=""

LDFLAGS=""

FFTW_FLAGS="--host=mips-linux \
--enable-shared"

PREFIX="$DEST/$version" && mkdir -p $PREFIX

./configure $FFTW_FLAGS --prefix=$PREFIX $CFLAGS $LDFLAGS | tee $PREFIX/configuration.txt
cp config.* $PREFIX
[ $PIPESTATUS == 0 ] || exit 1

make clean
make 
make install-strip

#done
