CPP=g++
CPPFLAGS=-O2

UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Linux)
	LIB_NATIVE=libhelfy.so
	INCLUDES=-I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux
endif
ifeq ($(UNAME_S),Darwin)
	LIB_NATIVE=libhelfy.jnilib
	INCLUDES=-I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/darwin
endif

all: build build/$(LIB_NATIVE)

build:
	mkdir -p build

build/$(LIB_NATIVE): src/*.cpp src/*.h
	$(CPP) $(CPPFLAGS) $(INCLUDES) -fPIC -shared -o $@ src/*.cpp

clean:
	rm -rf build
