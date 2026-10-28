"""Harmless bridge smoke test: Android settings navigation, no account or settings changes.

Requires a connected rooted test phone and Jarvis in its current Android user.
Tokens are read into memory, never logged or saved in the result artifact.
"""
import argparse
import json
import subprocess
import time
import urllib.request
from pathlib import Path


class Device:
    def __init__(self, serial):
        self.adb = str(Path.home() / "AppData/Local/Android/Sdk/platform-tools/adb.exe")
        self.serial = serial
        self.user = int(self.command("shell", "am", "get-current-user").strip())
        self.base = f"/data/user/{self.user}/com.jarvis.app/files/jarvis"
        self.token = self.command("shell", "su", "-c", f"cat {self.base}/config/bridge.token").strip()
        assert len(self.token) >= 32, "Missing bridge token"
        self.command("forward", "tcp:18791", "tcp:18791")

    def command(self, *args):
        return subprocess.check_output([self.adb, "-s", self.serial, *args], encoding="utf-8", errors="replace", timeout=45)

    def call(self, capability, arguments=None):
        body = json.dumps({"capability": capability, "arguments": arguments or {}}, ensure_ascii=False).encode()
        request = urllib.request.Request("http://127.0.0.1:18791/v1/capabilities", body,
            {"Authorization": f"Bearer {self.token}", "Content-Type": "application/json"})
        with urllib.request.urlopen(request, timeout=150) as response:
            return json.load(response)

    def memory(self, action, **args):
        return self.call("operation.memory", {"action": action, **args})

    def inspect(self):
        return self.call("accessibility.inspect")["data"]

    def wait_package(self, package):
        deadline = time.monotonic() + 10
        while time.monotonic() < deadline:
            ui = self.inspect()
            if ui.get("package") == package:
                return ui
            time.sleep(.25)
        raise AssertionError(f"Did not enter {package}")


def smoke(device):
    pkg = "com.android.settings"
    route_id = "device-test-open-settings"
    events = []

    def check(label, response):
        assert response.get("ok"), (label, response)
        events.append({"label": label, "response": response})
        return response

    started = time.monotonic()
    check("first exploration launch", device.call("app.launch", {"package": pkg}))
    device.wait_package(pkg)
    exploration_ms = round((time.monotonic() - started) * 1000)
    recipe = {"id": route_id, "title": "打开 Android 系统设置", "intent": "仅打开 Android 系统设置首页，不修改任何设置",
        "package": pkg, "parameters": [], "steps": [{"capability": "app.launch", "arguments": {"package": pkg}}],
        "verify": {"kind": "ui", "package": pkg}}
    check("save independently verified first success", device.memory("save", recipe=recipe))
    check("search saved route", device.memory("find", query="Android 系统设置", package=pkg))
    check("return home", device.call("accessibility.global", {"action": "home"}))
    replay = check("second use batched replay", device.memory("run", id=route_id, parameters={}))

    # Deliberately obsolete selector. Must stop before any click, not complete remaining actions.
    obsolete = json.loads(json.dumps(recipe))
    obsolete["steps"] = [{"capability": "accessibility.perform", "timeoutMs": 0,
        "arguments": {"package": pkg, "action": "click", "selector": {"viewId": "jarvis:test:obsolete"}},
        "before": {"kind": "ui", "package": pkg, "selector": {"viewId": "jarvis:test:obsolete"}}},
        {"capability": "app.launch", "arguments": {"package": pkg}}]
    check("stage outdated selector", device.memory("save", recipe=obsolete))
    failure = device.memory("run", id=route_id, parameters={})
    assert not failure["ok"] and failure["data"]["replan"] and failure["data"]["completedSteps"] == 0, failure
    events.append({"label": "obsolete route stopped before first click", "response": failure})
    stale = device.memory("find", query="系统设置", package=pkg)
    row = next(r for r in stale["data"]["recipes"] if r["id"] == route_id)
    assert row["status"] == "stale", row
    check("re-explore actual screen", device.call("app.launch", {"package": pkg}))
    device.wait_package(pkg)
    check("replace failed route with verified fresh path", device.memory("save", recipe=recipe))
    check("repaired path works", device.memory("run", id=route_id, parameters={}))
    check("return to Jarvis", device.call("app.launch", {"package": "com.jarvis.app"}))
    device.wait_package("com.jarvis.app")
    return {"androidUser": device.user, "explorationMs": exploration_ms,
        "singleCallReplayMs": replay["data"]["durationMs"], "events": events}


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=["inspect", "smoke", "list", "send"])
    parser.add_argument("--serial", default="1f5244c1")
    parser.add_argument("--output", type=Path)
    parser.add_argument("--prompt-file", type=Path)
    args = parser.parse_args()
    device = Device(args.serial)
    if args.mode == "inspect":
        result = device.inspect()
    elif args.mode == "list":
        result = device.memory("list")
    elif args.mode == "send":
        assert args.prompt_file, "--prompt-file is required for send"
        device.wait_package("com.jarvis.app")
        entered = device.call("accessibility.perform", {"package": "com.jarvis.app", "action": "input",
            "selector": {"editable": True}, "text": args.prompt_file.read_text(encoding="utf-8-sig")})
        assert entered["ok"], entered
        result = device.call("accessibility.perform", {"package": "com.jarvis.app", "action": "click", "selector": {"description": "发送"}})
        assert result["ok"], result
    else:
        result = smoke(device)
    text = json.dumps(result, ensure_ascii=False, indent=2)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(text, encoding="utf-8")
        print(f"Saved {args.mode} evidence to {args.output}")
    else:
        print(text)
