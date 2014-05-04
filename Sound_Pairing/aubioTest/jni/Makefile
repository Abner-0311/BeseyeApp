TOOLCHAIN=/opt/buildroot-gcc342
TOOLCHAIN_BIN=$(TOOLCHAIN)/bin/
CROSS_COMP=mipsel-linux-
INSTALL_DIR=./
INC_PATH=include
SRC_PATH=src
ZXING_PATH =zxing

CC=$(TOOLCHAIN_BIN)$(CROSS_COMP)gcc
CXX=$(TOOLCHAIN_BIN)$(CROSS_COMP)g++
LD=$(TOOLCHAIN_BIN)$(CROSS_COMP)ld
AR=$(TOOLCHAIN_BIN)$(CROSS_COMP)ar
RANLIB=$(TOOLCHAIN_BIN)$(CROSS_COMP)ranlib
NM=$(TOOLCHAIN_BIN)$(CROSS_COMP)nm
STRIP=$(TOOLCHAIN_BIN)$(CROSS_COMP)strip

CXXSOURCES=$(SRC_PATH)/FFT.cpp \
$(ZXING_PATH)/Exception.cpp \
$(ZXING_PATH)/common/IllegalArgumentException.cpp \
$(ZXING_PATH)/common/reedsolomon/GenericGF.cpp \
$(ZXING_PATH)/common/reedsolomon/GenericGFPoly.cpp \
$(ZXING_PATH)/common/reedsolomon/ReedSolomonDecoder.cpp \
$(SRC_PATH)/../common/src/utils.cpp \
$(SRC_PATH)/sp_config.cpp \
$(SRC_PATH)/AudioBufferMgr.cpp \
$(SRC_PATH)/FreqAnalyzer.cpp \
$(SRC_PATH)/AudioTest.cpp \
$(SRC_PATH)/main.cpp \
$(SRC_PATH)/../cam-handler/src/delegate/account_mgr.cpp


CXXOBJECTS=$(CXXSOURCES:.cpp=.o)

SOURCES=$(SRC_PATH)/preprocess/kiss_fft.c \
$(SRC_PATH)/preprocess/kiss_fftr.c \
$(SRC_PATH)/preprocess/mdf.c \
$(SRC_PATH)/preprocess/smallft.c \
$(SRC_PATH)/preprocess/fftwrap.c \
$(SRC_PATH)/preprocess/filterbank.c \
$(SRC_PATH)/preprocess/preprocess.c \
$(SRC_PATH)/../common/src/json_utils.c \
$(SRC_PATH)/../http_cgi_lib/src/http_cgi.c \
$(SRC_PATH)/../libwebsockets/src/simple_websocket_mgr.c \
$(SRC_PATH)/../ws-client/src/websocket_utils.c

OBJECTS=$(SOURCES:.c=.o)

EXECUTABLE=soundpairing

CFLAGS=-c -Wall -O3 -ffast-math -fforce-addr -I$(INC_PATH) -I$(TOOLCHAIN)/include/speex -I./ -Isrc -Isrc/preprocess -Icommon/inc -Icam-handler/inc -Ihttp_cgi_lib/inc -Iws-client/inc -Ilibwebsockets/inc -I$(TOOLCHAIN)/include -DCAM_ENV
LDFLAGS=-L$(TOOLCHAIN)/lib -lcurl -ljson-c -lwebsockets -lpthread -L$(TOOLCHAIN)/lib/libstdc++.a
#-lspeexdsp
CXXFLAGS= -Wall -ansi -fPIC -O3 -ffast-math -fforce-addr -I$(INC_PATH) -I$(TOOLCHAIN)/include/speex -I./ -Isrc -Isrc/preprocess -Icommon/inc -Icam-handler/inc -Ihttp_cgi_lib/inc -Iws-client/inc -Ilibwebsockets/inc -I$(TOOLCHAIN)/include -DCAM_ENV

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
