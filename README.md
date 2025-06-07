# Android Freecell

A classic Freecell solitaire card game implementation for Android devices.

## Overview

This project is an implementation of the Freecell solitaire card game for Android. It features:

- Complete Freecell game logic
- Drag-and-drop card movement
- Native Android UI with smooth animations
- Hybrid architecture using Kotlin and C++ with JNI

## Requirements

- Android SDK 34+
- Android Studio Iguana or newer
- CMake 3.22.1+
- C++17 compatible compiler

## Building

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Build and run on your device or emulator

## Architecture

The app uses a hybrid architecture:
- Game UI and logic implemented in Kotlin
- Rendering engine implemented in C++ using OpenGL ES

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

See the [TODO](TODO.md) file for planned improvements and known issues.
