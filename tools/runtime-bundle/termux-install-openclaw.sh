#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

OPENCLAW_VERSION="2026.9.7"
BRIDGE_URL="${JARVIS_BRIDGE_URL:-http://127.0.0.1:18791}"
BRIDGE_TOKEN="${JARVIS_BRIDGE_TOKEN:-}"

pkg update -y
pkg install -y nodejs-lts curl git
npm install -g "openclaw@${OPENCLAW_VERSION}"
mkdir -p "$HOME/.openclaw"
printf '%s\n' "JARVIS_BRIDGE_URL=${BRIDGE_URL}" "JARVIS_BRIDGE_TOKEN=${BRIDGE_TOKEN}" > "$HOME/.openclaw/jarvis.env"
chmod 600 "$HOME/.openclaw/jarvis.env"
export JARVIS_BRIDGE_URL="$BRIDGE_URL" JARVIS_BRIDGE_TOKEN="$BRIDGE_TOKEN"
exec openclaw gateway --bind loopback
