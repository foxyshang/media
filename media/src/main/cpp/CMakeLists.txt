cmake_minimum_required(VERSION 3.4.1)
#project(sffstreamer LANGUAGES C CXX)

#SET(CMAKE_C_FLAGS_DEBUG)          #指定编译器

add_library(rtmp
            SHARED
            IMPORTED)
set_target_properties(rtmp
                    PROPERTIES IMPORTED_LOCATION
                    ${CMAKE_SOURCE_DIR}/${ANDROID_ABI}/librtmp.so)

add_library(sffstreamer
    SHARED
   simplest_ffmpeg_streamer.c
   sps_decode.c
   stream_h264.c
   stream_aac.c
   comm.c)

#include libraries needed for echo lib




include_directories(src/main/cpp)
target_link_libraries( sffstreamer rtmp log)



#find_library( # Sets the name of the path variable.
#
#
#              log-lib
#
#              # Specifies the name of the NDK library that
#              # you want CMake to locate.
#              log )

#add_definitions(-DNO_CRYPTO)

#target_compile_options(sffstreamer
# PRIVATE
#    -Wall -Werror)
