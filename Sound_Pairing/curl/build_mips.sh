#!/bin/bash

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

./configure --host=mips-linux --prefix=$PREFIX

make clean
make -j4
make install
