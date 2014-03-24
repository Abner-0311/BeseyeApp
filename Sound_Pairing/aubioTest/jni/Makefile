TOOLCHAIN=/opt/buildroot-gcc342
TOOLCHAIN_BIN=$(TOOLCHAIN)/bin/
CROSS_COMP=mipsel-linux-
INSTALL_DIR=/home/abner.huang/ss_test
INC_PATH=include
SRC_PATH=src
ZXING_PATH = zxing
CC=$(TOOLCHAIN_BIN)$(CROSS_COMP)gcc
CXX=$(TOOLCHAIN_BIN)$(CROSS_COMP)g++
LD=$(TOOLCHAIN_BIN)$(CROSS_COMP)ld
AR=$(TOOLCHAIN_BIN)$(CROSS_COMP)ar
RANLIB=$(TOOLCHAIN_BIN)$(CROSS_COMP)ranlib
NM=$(TOOLCHAIN_BIN)$(CROSS_COMP)nm
STRIP=$(TOOLCHAIN_BIN)$(CROSS_COMP)strip
#SOURCES=$(SRC_PATH)/test-client.c

SOURCES=$(SRC_PATH)/preprocess/kiss_fft.c \
$(SRC_PATH)/preprocess/kiss_fftr.c \
$(SRC_PATH)/preprocess/mdf.c \
$(SRC_PATH)/preprocess/smallft.c \
$(SRC_PATH)/preprocess/fftwrap.c \
$(SRC_PATH)/preprocess/filterbank.c \
$(SRC_PATH)/preprocess/preprocess.c

CXXSOURCES=$(SRC_PATH)/FFT.cpp \
$(ZXING_PATH)/Exception.cpp \
$(ZXING_PATH)/common/IllegalArgumentException.cpp \
$(ZXING_PATH)/common/reedsolomon/GenericGF.cpp \
$(ZXING_PATH)/common/reedsolomon/GenericGFPoly.cpp \
$(ZXING_PATH)/common/reedsolomon/ReedSolomonDecoder.cpp \
$(SRC_PATH)/utils.cpp \
$(SRC_PATH)/sp_config.cpp \
$(SRC_PATH)/AudioBufferMgr.cpp \
$(SRC_PATH)/FreqAnalyzer.cpp \
$(SRC_PATH)/AudioTest.cpp \
$(SRC_PATH)/http_cgi.cpp \
$(SRC_PATH)/main.cpp 

OBJECTS=$(SOURCES:.c=.o)
EXECUTABLE=soundpairing
CFLAGS=-c -Wall -O3 -I$(INC_PATH) -I$(TOOLCHAIN)/include/speex -Isrc -I/home/abner.huang/ss_test/soundpairing -I$(TOOLCHAIN)/include
LDFLAGS=-L$(TOOLCHAIN)/lib -lcurl -ljson-c -lpthread -L$(TOOLCHAIN)/lib/libstdc++.a
#LDFLAGS=-L$(TOOLCHAIN)/lib -L$(TOOLCHAIN)/lib/libssl.a -L$(TOOLCHAIN)/lib/libjson-c.a -lwebsockets

CXXOBJECTS=$(CXXSOURCES:.cpp=.o)
CXXFLAGS= -Wall -ansi -fPIC -O3 -I$(INC_PATH) -I$(TOOLCHAIN)/include/speex -I/home/abner.huang/ss_test/soundpairing -Isrc -Isrc/preprocess -I$(TOOLCHAIN)/include

#all:$(CXXSOURCES) $(EXECUTABLE)

#$(EXECUTABLE):$(CXXOBJECTS)
#	$(CXX) $(LDFLAGS) $(CXXOBJECTS) -o $@

all:$(CXXSOURCES) $(SOURCES) $(EXECUTABLE)

$(EXECUTABLE):$(OBJECTS) $(CXXOBJECTS)
	$(CXX) $(LDFLAGS) $(OBJECTS) $(CXXOBJECTS) -o $@

.c.o:
	$(CC) $(CFLAGS) $< -o $@

#.cpp.o:
#	$(CXX) $(CXXFLAGS) $< -o $@

install:
	cp $(EXECUTABLE) $(INSTALL_DIR)
clean:
	rm -f $(EXECUTABLE) $(OBJECTS)
	rm -f $(EXECUTABLE) $(CXXOBJECTS) 
