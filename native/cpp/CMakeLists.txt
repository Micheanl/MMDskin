cmake_minimum_required(VERSION 3.25)
project(mmdskin_native_cpp LANGUAGES CXX)

add_library(mmdskin_native_cpp STATIC src/mmdskin_backend.cpp)
target_compile_features(mmdskin_native_cpp PUBLIC cxx_std_20)
target_include_directories(mmdskin_native_cpp PUBLIC include)

if (DEFINED IMGUI_SOURCE_DIR)
    find_package(JNI REQUIRED)
    add_library(mmdskin_imgui SHARED
            src/mmdskin_imgui.cpp
            "${IMGUI_SOURCE_DIR}/imgui.cpp"
            "${IMGUI_SOURCE_DIR}/imgui_draw.cpp"
            "${IMGUI_SOURCE_DIR}/imgui_tables.cpp"
            "${IMGUI_SOURCE_DIR}/imgui_widgets.cpp"
    )
    target_compile_features(mmdskin_imgui PRIVATE cxx_std_20)
    target_include_directories(mmdskin_imgui PRIVATE include "${IMGUI_SOURCE_DIR}" ${JNI_INCLUDE_DIRS})
    target_link_libraries(mmdskin_imgui PRIVATE ${JNI_LIBRARIES})
    set_target_properties(mmdskin_imgui PROPERTIES
            ARCHIVE_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}"
            LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}"
            RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}"
            ARCHIVE_OUTPUT_DIRECTORY_RELEASE "${CMAKE_BINARY_DIR}"
            LIBRARY_OUTPUT_DIRECTORY_RELEASE "${CMAKE_BINARY_DIR}"
            RUNTIME_OUTPUT_DIRECTORY_RELEASE "${CMAKE_BINARY_DIR}"
    )
endif()
