# Jarvis runtime tools

The chat composer provides a quick toolbox for speech input and optional command-line tools. The same tools appear in Settings under **Runtime tools**.

## Voice transcription

Voice input opens the Android speech-recognition service with Simplified Chinese and an offline-recognition preference. Recognized words are appended to the current draft; Jarvis never sends the transcript automatically. Offline availability depends on the recognizer and its downloaded language pack. The selected system service may use its own network service and privacy terms.

## Command-line tools

Jarvis detects CMake, Clang, Ninja, GNU Make, FFmpeg, FFprobe, Python, Git, cURL, Wget, jq, OpenSSL, SQLite, tar, unzip, zip, Node.js, Rust (`rustc`) and Cargo. Missing commands can be requested from Termux if Termux has been initialized in the same Android user and its **Allow external apps to run commands** option is enabled. Installation runs `pkg install` through that user's Termux; if Termux is not initialized or rejects the command, Jarvis opens it and copies the install command for manual execution. Return to Jarvis and refresh the list after the package manager finishes.

These commands are not bundled in the APK. Jarvis reports their detected location and includes that status in the assistant's task context. OpenClaw can call `jarvis_runtime_tools` to list or run a catalogued command with literal arguments. To invoke them from a task, enable **Full root mode** and grant the root manager's request. The bridge prepares the current Android user's Termux environment for Termux commands and returns the exit code and bounded output. Check the result before reporting success.

CMake alone is not an Android application build environment. Building Android native code also requires an Android NDK toolchain configured for the intended ABI. FFmpeg and FFprobe are installed together through the `ffmpeg` Termux package.
