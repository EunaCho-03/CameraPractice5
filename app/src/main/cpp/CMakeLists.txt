
cmake_minimum_required(VERSION 3.22.1)

set(pathPROJECT C:/Users/QR22003/AndroidStudioProjects/CameraPractice5)
set(pathOPENCV ${pathPROJECT}/OpenCV)
set(pathLIBOPENCV_JAVA ${pathOPENCV}/native/libs/${ANDROID_ABI}/libopencv_java4.so)

set(CMAKE_VERBOSE_MAKEFILE on)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

include_directories(${pathOPENCV}/native/jni/include)

project("camerapractice5")

add_library( # Sets the name of the library.
        camerapractice5
        SHARED
        ${pathPROJECT}/app/src/main/cpp/camerapractice5.cpp )

add_library( lib_opencv SHARED IMPORTED )

set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION ${pathLIBOPENCV_JAVA})

find_library( # Sets the name of the path variable.
        log-lib
        log )

target_link_libraries( # Specifies the target library.
        camerapractice5
        lib_opencv
        ${log-lib} )
