"""Theme UI review through the real Jarvis accessibility bridge; no credentials saved."""
import argparse
import json
import importlib.util
import subprocess
import time
from pathlib import Path
spec = importlib.util.spec_from_file_location("jarvis_device", Path(__file__).with_name("test-operation-memory-device.py"))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
Device = module.Device


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("action", choices=["inspect", "capture", "click", "swipe", "rotation"])
    parser.add_argument("--text")
    parser.add_argument("--description")
    parser.add_argument("--output", type=Path, default=Path("artwork/themes/device-current"))
    parser.add_argument("--direction", choices=["up", "down"], default="up")
    parser.add_argument("--rotation", choices=["0", "1", "free"], default="0")
    parser.add_argument("--quiet", action="store_true")
    args = parser.parse_args()
    device = Device("1f5244c1")
    if args.action == "click":
        selector = {"text": args.text} if args.text else {"description": args.description}
        result = device.call("accessibility.perform", {"package": "com.jarvis.app", "action": "click", "selector": selector})
        assert result["ok"], result
        time.sleep(.6)
    elif args.action == "swipe":
        ui = device.inspect()
        bounds = ui["bounds"]
        w, h = bounds["right"], bounds["bottom"]
        y, end = (.78 * h, .4 * h) if args.direction == "up" else (.4 * h, .78 * h)
        result = device.call("accessibility.swipe", {"x": w * .75, "endX": w * .75, "y": y, "endY": end, "durationMs": 350})
        assert result["ok"], result
        time.sleep(.3)
    elif args.action == "rotation":
        if args.rotation == "free":
            device.command("shell", "wm", "user-rotation", "free")
        else:
            device.command("shell", "wm", "user-rotation", "lock", args.rotation)
        time.sleep(.7)
    ui = device.inspect()
    assert ui.get("active"), "Jarvis accessibility service not connected"
    if args.quiet:
        print(args.action + ": " + str(ui.get("package")))
    else:
        print(json.dumps({"package": ui.get("package"), "bounds": ui.get("bounds"),
                      "nodes": [{"text": n["text"], "description": n["description"], "bounds": n["bounds"]}
                                for n in ui.get("nodes", []) if n["text"] or n["description"]]}, ensure_ascii=False))
    if args.action == "capture":
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.with_suffix(".json").write_text(json.dumps(ui, ensure_ascii=False, indent=2), encoding="utf-8")
        with args.output.with_suffix(".png").open("wb") as output:
            subprocess.run([device.adb, "-s", device.serial, "exec-out", "screencap", "-p"], stdout=output, check=True)


if __name__ == "__main__":
    main()
