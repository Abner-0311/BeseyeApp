#!/bin/bash

DEST=/opt/buildroot-gcc342 

TOOLCHAIN=/opt/buildroot-gcc342

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

./configure --prefix=$PREFIX --host=mips-linux

  make clean
  make -j4 || exit 1
  make install || exit 1

#done
