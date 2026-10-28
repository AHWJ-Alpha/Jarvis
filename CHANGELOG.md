# Jarvis changelog

## 0.0.15

- Add the DSH-style chat layout with message-level edit, undo, and branch actions.
- Add workspace-grouped conversation navigation with collapsible groups and a landscape side rail toggle.
- Add an in-composer reasoning depth menu (`off` through `max`).
- Fix floating assistant chat focus and accessibility progress presentation.
- Add an in-chat quick toolbox for voice transcription and optional development/media commands.
- Add CMake, Clang, Ninja, Make, FFmpeg/FFprobe, Python, Git, curl, Wget, jq, OpenSSL, SQLite, tar, unzip, zip, Node.js and Rust command detection with Termux installation requests.
- Add Chinese voice-to-text input through the Android speech recognition service; recognized text stays in the composer for review before sending.
- Include installed tool availability and Termux launch guidance in the assistant's per-turn context.
- Expose catalogued command execution to OpenClaw through `jarvis_runtime_tools`, with quoted arguments, current-user Termux paths, exit codes and timeouts.

## 0.0.14

- Add an OpenClaw plugin manager that imports self-contained JavaScript plugin ZIPs, lists installed plugins, and supports enable, disable, and uninstall with Gateway reloads.
- Add AI-generated plugin drafts that can be reviewed and edited before installation; generated plugins remain disabled until explicitly enabled.
- Persist conversations across navigation and app restarts; add real conversation selection, new conversations, branches, edit-and-resend, and undo of the latest chat turn.
- Add an optional LAN/Tailscale remote console with pairing, revocable browser access, and a separate device-control permission.
- Serialize local chat, automation, and remote task execution so only one task drives the accessibility service at a time.
- Add notification-triggered automation rules, operation memory, accessibility startup controls, and refreshed anime themes.
- Update the embedded OpenClaw Android compatibility layer and device bridge.

Plugin ZIPs must be reviewed before enabling. OpenClaw plugins execute in the Gateway process and are not isolated by a process sandbox.

## 0.0.1–0.0.2

See the existing GitHub Releases for the initial Android builds.
