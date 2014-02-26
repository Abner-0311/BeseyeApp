#!/bin/bash

#DIR_OGG=/opt/tftpboot/libogg
#DEST=/opt/tftpboot/speex && rm -rf $DEST

TOOLCHAIN=/opt/buildroot-gcc342
DIR_OGG=$TOOLCHAIN/libogg
DEST=$TOOLCHAIN 

export PATH=$TOOLCHAIN/bin:$PATH
export CC=mipsel-linux-gcc
export CXX=mipsel-linux-g++
export LD=mipsel-linux-ld
export AR=mipsel-linux-ar
export RANLIB=mipsel-linux-ranlib
export NM=mipsel-linux-nm
export STRIP=mipsel-linux-strip

export CFLAGS="-I${DIR_OGG}/include"
export LDFLAGS="-L${DIR_OGG}/lib"

PREFIX="$DEST" 
#&& mkdir -p $PREFIX

./configure --prefix=$PREFIX --host=mips-linux --target=mips-linux --enable-shared --enable-static=no

make clean
make -j4 || exit 1
make install-strip || exit 1

#done


