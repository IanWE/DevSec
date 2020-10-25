# See LICENSE file for license and copyright information

#set(ARCH arm64-v8a)

set(LIBFLUSH_VERSION_MAJOR 0)
set(LIBFLUSH_VERSION_MINOR 0)
set(LIBFLUSH_VERSION_REV 1)

set(VERSION ${LIBFLUSH_VERSION_MAJOR}.${LIBFLUSH_VERSION_MINOR}.${LIBFLUSH_VERSION_REV})

# If the API changes, the API version and the ABI version have to be bumped.
set(LIBFLUSH_VERSION_API 1)

# If the ABI breaks for any reason, this has to be bumped.
set(LIBFLUSH_VERSION_ABI 1)
# Rules for the SOMAJOR and SOMINOR.
# Before a release check perform the following checks against the last release:
# * If a function has been removed or the paramaters of a function have changed
#   bump SOMAJOR and set SOMINOR to 0.
# * If any of the exported datastructures have changed in a incompatible way
# 	bump SOMAJOR and set SOMINOR to 0.
# * If a function has been added bump SOMINOR.

set(SOMAJOR 1)
set(SOMINOR 0)
set(SOVERSION ${SOMAJOR}.${SOMINOR})

# paths
#PREFIX /usr
#LIBDIR ?= ${PREFIX}/lib
#INCLUDEDIR ?= ${PREFIX}/include
#DEPENDDIR=.depend
#set(BUILDDIR build/${ARCH})
#BUILDDIR_RELEASE ?= ${BUILDDIR}/release
#BUILDDIR_DEBUG ?= ${BUILDDIR}/debug
#BUILDDIR_GCOV ?= ${BUILDDIR}/gcov
#BINDIR ?= bin

# libs
#FIU_INC ?= $(shell ${PKG_CONFIG} --cflags libfiu)
#FIU_LIB ?= $(shell ${PKG_CONFIG} --libs libfiu) -ldl

#INCS =
#LIBS = -lm

# flags

# linker flags
#LDFLAGS += -fPIC

# debug
#DFLAGS = -O0 -g

# compiler
#CC ?= gcc

# archiver
#AR ?= ar

# strip
#SFLAGS ?= -s

# gcov & lcov
#GCOV_CFLAGS=-fprofile-arcs -ftest-coverage -fno-inline -fno-inline-small-functions -fno-default-inline
#GCOV_LDFLAGS=-fprofile-arcs
#LCOV_OUTPUT=gcov
#LCOV_EXEC=lcov
#LCOV_FLAGS=--base-directory . --directory ${BUILDDIR_GCOV} --capture --rc \
#lcov_branch_coverage=1 --output-file ${BUILDDIR_GCOV}/$(PROJECT).info
#GENHTML_EXEC=genhtml
#GENHTML_FLAGS=--rc lcov_branch_coverage=1 --output-directory ${LCOV_OUTPUT} ${BUILDDIR_GCOV}/$(PROJECT).info

# libfiu
#WITH_LIBFIU ?= 0
#FIU_RUN ?= fiu-run -x
set(WITH_LIBFIU 0)
set(FIU_RUN " fiu-run -x ")

# set to something != 0 if you want verbose build output
set(VERBOSE 0)

# enable colors
set(COLOR 1)

# android
set(ANDROID_PLATFORM 29)

# thread safe
set(WITH_PTHREAD 1)

# pagemap access
set(HAVE_PAGEMAP_ACCESS 1)

# time sources
#TIME_SOURCES = (register perf monotonic_clock thread_counter)
set(TIME_SOURCE monotonic_clock)

# use eviction instead of flush
set(USE_EVICTION 0)#only available for armv7

# Define device
set(DEVICE_CONFIGURATION default)#available only if using eviction
#set(DEVICE_CONFIGURATION 0)