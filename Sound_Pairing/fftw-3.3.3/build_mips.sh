#!/bin/bash

#DIR_OGG=/opt/tftpboot/libogg
#DEST=/opt/tftpboot/speex && rm -rf $DEST

TOOLCHAIN=/opt/buildroot-gcc342
DEST=$TOOLCHAIN 

export PATH=$TOOLCHAIN/bin:$PATH
export CC=mipsel-linux-gcc
export CXX=mipsel-linux-g++
export LD=mipsel-linux-ld
export AR=mipsel-linux-ar
export RANLIB=mipsel-linux-ranlib
export NM=mipsel-linux-nm
export STRIP=mipsel-linux-strip

PREFIX="$DEST" 
#&& mkdir -p $PREFIX

./configure --enable-single --prefix=$PREFIX --host=mips-linux --target=mips-linux

make clean
make -j4 || exit 1
make install-strip || exit 1

#done


