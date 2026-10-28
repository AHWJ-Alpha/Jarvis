"""Root and one-shot scheduler diagnostics on the connected Jarvis phone.

Uses Jarvis's own bridge and embedded Gateway. Credentials stay in memory.
Only uniquely named test files/settings/jobs are changed, and settings are restored.
"""
import argparse
import importlib.util
import json
import shlex
import time
import urllib.error
import urllib.request
import uuid
from datetime import datetime, timedelta, timezone
from pathlib import Path

from websockets.sync.client import connect

spec = importlib.util.spec_from_file_location("jarvis_device", Path(__file__).with_name("test-operation-memory-device.py"))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
Device = module.Device


class Gateway:
    def __init__(self, device):
        device.command("forward", "tcp:18789", "tcp:18789")
        self.token = device.command("shell", "su", "-c", f"cat {device.base}/config/gateway.token").strip()
        self.socket = None

    def invoke(self, tool, args):
        request = urllib.request.Request("http://127.0.0.1:18789/tools/invoke",
            json.dumps({"tool": tool, "args": args}).encode(),
            {"Authorization": "Bearer " + self.token, "Content-Type": "application/json"})
        try:
            with urllib.request.urlopen(request, timeout=40) as response:
                return json.load(response)
        except urllib.error.HTTPError as error:
            return {"httpStatus": error.code, "response": json.load(error)}

    def rpc(self, method, params=None):
        if self.socket is None:
            self.socket = connect("ws://127.0.0.1:18789", open_timeout=15, max_size=8 * 1024 * 1024)
            challenge = json.loads(self.socket.recv(timeout=15))
            assert challenge.get("event") == "connect.challenge", "No Gateway challenge"
            self._request("connect", {
                "minProtocol": 4, "maxProtocol": 4,
                "client": {"id": "gateway-client", "version": "jarvis-device-diagnostic-0.0.6",
                           "platform": "android", "mode": "backend"},
                "role": "operator", "scopes": ["operator.admin", "operator.read", "operator.write"],
                "auth": {"token": self.token}})
        return self._request(method, params or {})

    def _request(self, method, params):
        request_id = uuid.uuid4().hex
        self.socket.send(json.dumps({"type": "req", "id": request_id, "method": method, "params": params}))
        deadline = time.monotonic() + 45
        while time.monotonic() < deadline:
            message = json.loads(self.socket.recv(timeout=max(1, deadline - time.monotonic())))
            if message.get("id") == request_id and message.get("type") == "res":
                if not message.get("ok"):
                    raise RuntimeError(json.dumps(message.get("error"), ensure_ascii=False))
                return message.get("payload")
        raise TimeoutError(method)


def root_smoke(device, gateway):
    events = []

    def check(name, command):
        response = gateway.invoke("jarvis_root_exec", {"command": command})
        details = response.get("result", {}).get("details", {})
        events.append({"name": name, "response": response})
        assert response.get("ok") and details.get("ok"), (name, response)
        print(name + ": PASS", flush=True)
        return details["message"].strip()

    identity = check("app-owned Gateway root identity", "id")
    assert "uid=0(root)" in identity, identity
    nonce = uuid.uuid4().hex
    directory = f"/data/local/tmp/jarvis-root-test-{nonce}"
    key = f"jarvis_root_diagnostic_{nonce}"
    try:
        value = check("protected root-owned file write and read",
            f"mkdir -m 700 {directory} && printf '%s' {nonce} > {directory}/marker && "
            f"chmod 600 {directory}/marker && stat -c '%u %a %s' {directory}/marker && cat {directory}/marker")
        assert value.endswith(nonce) and value.startswith("0 600 32"), value
        value = check("secure settings write-read-restore",
            f"trap 'settings --user {device.user} delete secure {key} >/dev/null' EXIT; "
            f"settings --user {device.user} put secure {key} {nonce} && "
            f"settings --user {device.user} get secure {key}")
        assert value == nonce, value
        restored = device.command("shell", "settings", "--user", str(device.user), "get", "secure", key).strip()
        assert restored == "null", "Temporary secure key was not removed"
        check("root screen capture", f"screencap -p {directory}/screen.png && stat -c '%s' {directory}/screen.png")
        header = check("screen capture PNG verification", f"od -An -tx1 -N8 {directory}/screen.png")
        assert header.split() == ["89", "50", "4e", "47", "0d", "0a", "1a", "0a"], header
        check("root launch Android settings in current user", f"am start --user {device.user} -a android.settings.SETTINGS")
        device.wait_package("com.android.settings")
        check("root return to Jarvis", f"am start --user {device.user} -n com.jarvis.app/.MainActivity")
        device.wait_package("com.jarvis.app")
    finally:
        cleanup = device.call("root.exec", {"command": f"rm -f {directory}/marker {directory}/screen.png; rmdir {directory}; settings --user {device.user} delete secure {key} >/dev/null"})
        events.append({"name": "remove only diagnostic files and key", "response": cleanup})
    return {"androidUser": device.user, "identity": identity, "events": events}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("action", choices=["root", "status", "schedule", "result", "cleanup"])
    parser.add_argument("--state", type=Path, default=Path("artwork/root-proactivity-test.json"))
    args = parser.parse_args()
    device = Device("1f5244c1")
    gateway = Gateway(device)
    state = json.loads(args.state.read_text(encoding="utf-8")) if args.state.is_file() else {}
    if args.action == "root":
        state["root"] = root_smoke(device, gateway)
    elif args.action == "status":
        config = json.loads(device.command("shell", "su", "-c", f"cat {device.base}/runtime/openclaw-home/openclaw.json"))
        state["configuration"] = {"tools": config.get("tools"), "cron": config.get("cron"),
                                  "heartbeat": config.get("agents", {}).get("defaults", {}).get("heartbeat")}
        state["automationTool"] = gateway.invoke("automations", {"action": "status"})
        state["scheduler"] = gateway.rpc("cron.status")
        state["jobsBefore"] = gateway.rpc("cron.list", {"includeDisabled": True})
        print(json.dumps({k: state[k] for k in ["configuration", "automationTool", "scheduler", "jobsBefore"]}, ensure_ascii=False, indent=2))
    elif args.action == "schedule":
        assert not state.get("jobId"), "A diagnostic job already exists; inspect/clean it first"
        marker = f"/data/local/tmp/jarvis-proactive-{uuid.uuid4().hex}.txt"
        command = f"id > {marker}; date +%s >> {marker}; chmod 600 {marker}"
        at = (datetime.now(timezone.utc) + timedelta(seconds=30)).isoformat(timespec="seconds").replace("+00:00", "Z")
        payload = {"name": "Jarvis one-shot root proactivity diagnostic", "enabled": True,
                   "deleteAfterRun": False, "schedule": {"kind": "at", "at": at},
                   "sessionTarget": "isolated", "wakeMode": "now",
                   "payload": {"kind": "agentTurn", "message": "这是用户授权的一次后台 root 能力测试。请直接调用 jarvis_root_exec 执行以下固定命令，然后仅回复测试已完成。不要操作任何其他应用或发送消息：\n" + command,
                               "timeoutSeconds": 120, "toolsAllow": ["jarvis_root_exec"]}, "delivery": {"mode": "none"}}
        result = gateway.rpc("cron.add", payload)
        state.update({"jobId": result["id"], "marker": marker, "scheduledAt": at, "createdJob": result})
        print(json.dumps({"jobId": result["id"], "scheduledAt": at, "marker": marker}, ensure_ascii=False))
    elif args.action == "result":
        state["jobStatus"] = gateway.rpc("cron.get", {"jobId": state["jobId"]})
        state["runs"] = gateway.rpc("cron.runs", {"jobId": state["jobId"], "limit": 5})
        state["markerResult"] = device.call("root.exec", {"command": f"if test -f {shlex.quote(state['marker'])}; then cat {shlex.quote(state['marker'])}; else echo NOT_CREATED; fi"})
        print(json.dumps({"jobState": state["jobStatus"].get("job", state["jobStatus"]).get("state"), "runs": state["runs"], "markerResult": state["markerResult"]}, ensure_ascii=False, indent=2))
    elif args.action == "cleanup":
        if state.get("jobId"):
            try:
                state["cleanupJob"] = gateway.rpc("cron.remove", {"jobId": state["jobId"]})
            except Exception as error:
                # OpenClaw may commit job removal before transcript archive
                # cleanup fails. Continue removing our unique marker either way.
                state["cleanupJobError"] = str(error)
            state["cleanupMarker"] = device.call("root.exec", {"command": "rm -f " + shlex.quote(state["marker"])})
            remaining = gateway.rpc("cron.list", {"includeDisabled": True})
            state["testJobRemoved"] = all(job.get("id") != state["jobId"] for job in remaining.get("jobs", []))
            state["cleanedUp"] = bool(state["cleanupMarker"].get("ok")) and state["testJobRemoved"]
        print(json.dumps({"cleanedUp": state.get("cleanedUp", False),
                          "testJobRemoved": state.get("testJobRemoved", False),
                          "cleanupJobError": state.get("cleanupJobError")}, ensure_ascii=False))
    args.state.parent.mkdir(parents=True, exist_ok=True)
    args.state.write_text(json.dumps(state, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    main()
