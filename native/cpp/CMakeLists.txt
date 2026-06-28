cmake_minimum_required(VERSION 3.25)
project(mmdskin_native_cpp LANGUAGES CXX)

add_library(mmdskin_native_cpp STATIC src/mmdskin_backend.cpp)
target_compile_features(mmdskin_native_cpp PUBLIC cxx_std_20)
target_include_directories(mmdskin_native_cpp PUBLIC include)
