cmake_minimum_required(VERSION 3.10.2)
project("danilka")

add_library(danilka SHARED
    native-lib.cpp)

find_library(log-lib log)
target_link_libraries(danilka ${log-lib})
