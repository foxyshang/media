cmake_minimum_required(VERSION 3.4.1)
add_library(native-lib SHARED  src/main/cpp/native-lib.cpp )

add_library(sffstreamer SHARED
   src/main/cpp/simplest_ffmpeg_streamer.c
   src/main/cpp/sps_decode.c
   src/main/cpp/stream_h264.c
   src/main/cpp/stream_aac.c
   src/main/cpp/comm.c
   )

find_library(log-lib log)
target_link_libraries(native-lib ${log-lib})

add_library(rtmp SHARED IMPORTED)
set_target_properties(rtmp PROPERTIES IMPORTED_LOCATION ${PROJECT_SOURCE_DIR}/src/main/cpp/${ANDROID_ABI}/librtmp.so)
include_directories(src/main/cpp/)
target_link_libraries(sffstreamer rtmp ${log-lib} )
