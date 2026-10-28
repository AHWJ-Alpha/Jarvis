const bridgeUrl = () => process.env.JARVIS_BRIDGE_URL || "http://127.0.0.1:18791";
const bridgeToken = () => process.env.JARVIS_BRIDGE_TOKEN || "";

async function callBridge(capability, argumentsObject, risk, requiresApproval) {
  const response = await fetch(`${bridgeUrl()}/v1/capabilities`, {
    method: "POST",
    headers: {
      "authorization": `Bearer ${bridgeToken()}`,
      "content-type": "application/json"
    },
    body: JSON.stringify({ capability, arguments: argumentsObject, risk, requiresApproval, approved: false })
  });
  const body = await response.json();
  return {
    content: [{ type: "text", text: JSON.stringify(body) }],
    details: body
  };
}

function tool(name, label, capability, parameters, risk, requiresApproval) {
  const guidance = {
    "device.info": "Returns the Android user ID and external storage path for this app. Use these for multi-user file checks and root commands.",
    "accessibility.inspect": "Read the current visible app UI, viewId values and screen bounds. Inspect again after navigation; tap the center of a fresh visible target's bounds. Do not reuse coordinates from an earlier screen or orientation. Prefer this over uiautomator dump, which can disrupt the accessibility service.",
    "accessibility.click": "Tap a visible screen coordinate. Supply numeric x and y from the latest inspect result; this tool does not accept text selectors. Check the UI after tapping.",
    "accessibility.input": "Set text in the focused editable field, or the first editable field. Supply a nonempty text string.",
    "app.launch": "Launch an installed app in Jarvis's Android user. Supply its package ID in the package field, for example {package:'jp.pxv.android'}. Inspect the UI to verify the launch.",
    "runtime.tools": "List installed CMake, Clang, Ninja, Make, FFmpeg/FFprobe, Python, Git, curl, wget, jq, OpenSSL, SQLite, tar, unzip, zip, Node.js and Rust tools with action=list. To execute a catalogued tool, use action=run, tool=<listed id>, args=[literal argument strings], optional workingDirectory and timeoutSeconds. Run requires Full root mode. Results include exitCode and output; check both before reporting success. Termux environment is prepared for the current Android user. Never pass shell snippets as args; args are quoted as literal strings.",
    "accessibility.perform": "Act on exactly one live visible node. Required package and selector with exact viewId/text/description/class/editable fields; action click or input, text for input. An ambiguous selector or changed package fails. Prefer this over raw coordinates and use it in saved operation paths.",
    "accessibility.swipe": "Swipe from x,y to endX,endY on the current screen. Inspect before and after; saved swipes require a screen fingerprint guard.",
    "operation.memory": "Persistent procedures for ALL apps/tasks. First find(query,package optional). Run matching ready id with current named parameters. A failed run stops immediately, marks route stale and returns replan=true plus screen: explore from actual state, do not repeat completed sends/downloads, save the repaired route with the same id. After first successful exploration use recording to get actual steps, prune detours, parameterize dynamic targets, then save(recipe). Recipe: {id?:string,title,intent,package,parameters:[name],sampleParameters:{name:currentValue},steps:[{capability,arguments,before?,after?,timeoutMs?}],verify}. Dynamic strings use {{name}}. UI condition: {kind:'ui',package,selector?:{viewId?,text?,description?,class?,editable?,contains?},fingerprint?}. Every UI action needs before; click/input/swipe coordinates need a fresh fingerprint. Prefer accessibility.perform with package+unique selector. File condition: {kind:'file',path,minBytes:1}; file must be new/updated. Probe: {kind:'probe',capability:'device.info'|'accessibility.inspect'|'ocr.read'|'notification.read',field:'data.model',equals:value or contains:string}. Save verifies the real condition now. root.exec requires Full root and cannot interpolate dynamic parameters. list lists routes; invalidate(id,reason) disables a bad route; forget(id) deletes it. Memory stays on device and survives embedded-runtime reinstall.",
    "root.exec": "Execute a shell command with root in Full mode. Ten-second execution limit. Prefer jarvis_runtime_tools for catalogued CMake, Clang, Ninja, Make, FFmpeg/FFprobe, Python, Git, cURL, Wget, jq, OpenSSL, SQLite, tar, unzip, zip, Node.js and Rust commands; it uses literal argument arrays and reports exit status. For Termux binaries, use the current Android user ID from device.info: PREFIX=/data/user/<androidUserId>/com.termux/files/usr, HOME=/data/user/<androidUserId>/com.termux/files/home, TMPDIR=$PREFIX/tmp, LD_LIBRARY_PATH=$PREFIX/lib, PATH=$PREFIX/bin:$PATH. Do not silently use user 0's Termux environment. Android native builds still need a matching NDK/toolchain and ABI configuration. Shared storage from su may point to user 0; for file verification use /data/media/<androidUserId>/Pictures or Download. Prefer accessibility.inspect for UI reads instead of uiautomator dump. A screenshot file alone is not a downloaded image."
  };
  return {
    name,
    label,
    description: `Jarvis Android capability: ${capability}. ${guidance[capability] || ""} Permission and audit policy is enforced by Jarvis.`,
    parameters,
    execute: async (_toolCallId, args) => callBridge(capability, args || {}, risk, requiresApproval)
  };
}

const empty = { type: "object", additionalProperties: false, properties: {} };

export default {
  id: "jarvis-device-bridge",
  name: "Jarvis Device Bridge",
  register(api) {
    api.registerTool(() => tool("jarvis_operation_memory", "Jarvis Operation Memory", "operation.memory", {
      type: "object", additionalProperties: false, required: ["action"], properties: {
        action: { type: "string", enum: ["find", "list", "get", "recording", "save", "run", "invalidate", "forget"] },
        id: { type: "string" }, query: { type: "string" }, package: { type: "string" }, reason: { type: "string" },
        parameters: { type: "object", additionalProperties: { type: "string" } },
        recipe: { type: "object", additionalProperties: true }
      }
    }, "HIGH", false), { name: "jarvis_operation_memory" });
    api.registerTool(() => tool("jarvis_accessibility_perform", "Jarvis Selector Action", "accessibility.perform", {
      type: "object", additionalProperties: false, required: ["package", "selector", "action"], properties: {
        package: { type: "string" }, action: { type: "string", enum: ["click", "input"] }, text: { type: "string", maxLength: 4000 },
        selector: { type: "object", additionalProperties: false, properties: {
          viewId: { type: "string" }, text: { type: "string" }, description: { type: "string" }, class: { type: "string" },
          editable: { type: "boolean" }, contains: { type: "boolean" }
        } }
      }
    }, "HIGH", false), { name: "jarvis_accessibility_perform" });
    api.registerTool(() => tool("jarvis_accessibility_swipe", "Jarvis Swipe", "accessibility.swipe", {
      type: "object", additionalProperties: false, required: ["x", "y", "endX", "endY"], properties: {
        x: { type: "number" }, y: { type: "number" }, endX: { type: "number" }, endY: { type: "number" }, durationMs: { type: "integer", minimum: 100, maximum: 1500 }
      }
    }, "HIGH", false), { name: "jarvis_accessibility_swipe" });
    api.registerTool(() => tool("jarvis_device_info", "Jarvis Device Info", "device.info", empty, "LOW", false), { name: "jarvis_device_info" });
    api.registerTool(() => tool("jarvis_runtime_tools", "Jarvis Runtime Tools", "runtime.tools", {
      type: "object", additionalProperties: false, required: ["action"], properties: {
        action: { type: "string", enum: ["list", "run"] },
        tool: { type: "string", enum: ["cmake", "ninja", "clang", "make", "ffmpeg", "ffprobe", "python", "git", "curl", "wget", "jq", "openssl", "sqlite3", "tar", "unzip", "zip", "node", "rustc", "cargo"] },
        args: { type: "array", maxItems: 64, items: { type: "string", maxLength: 4096 } },
        workingDirectory: { type: "string", maxLength: 1024 },
        timeoutSeconds: { type: "integer", minimum: 1, maximum: 120 }
      }
    }, "HIGH", false), { name: "jarvis_runtime_tools" });
    api.registerTool(() => tool("jarvis_camera_capture", "Jarvis Camera Capture", "camera.capture", empty, "MEDIUM", true), { name: "jarvis_camera_capture" });
    api.registerTool(() => tool("jarvis_accessibility_inspect", "Jarvis Accessibility Inspect", "accessibility.inspect", empty, "LOW", false), { name: "jarvis_accessibility_inspect" });
    api.registerTool(() => tool("jarvis_accessibility_click", "Jarvis Accessibility Click", "accessibility.click", {
      type: "object", additionalProperties: false, required: ["x", "y"], properties: {
        x: { type: "number" }, y: { type: "number" }
      }
    }, "HIGH", true), { name: "jarvis_accessibility_click" });
    api.registerTool(() => tool("jarvis_accessibility_input", "Jarvis Accessibility Input", "accessibility.input", {
      type: "object", additionalProperties: false, required: ["text"], properties: {
        text: { type: "string", minLength: 1, maxLength: 4000 }
      }
    }, "HIGH", false), { name: "jarvis_accessibility_input" });
    api.registerTool(() => tool("jarvis_accessibility_global", "Jarvis Accessibility Global Action", "accessibility.global", {
      type: "object", additionalProperties: false, required: ["action"], properties: {
        action: { type: "string", enum: ["home", "back", "recents", "notifications"] }
      }
    }, "MEDIUM", false), { name: "jarvis_accessibility_global" });
    api.registerTool(() => tool("jarvis_app_launch", "Jarvis Launch App", "app.launch", {
      type: "object", additionalProperties: false, required: ["package"], properties: {
        package: { type: "string", minLength: 3, maxLength: 150 }
      }
    }, "MEDIUM", false), { name: "jarvis_app_launch" });
    api.registerTool(() => tool("jarvis_ocr_read", "Jarvis OCR Read", "ocr.read", empty, "LOW", false), { name: "jarvis_ocr_read" });
    api.registerTool(() => tool("jarvis_notification_read", "Jarvis Notification Read", "notification.read", empty, "MEDIUM", false), { name: "jarvis_notification_read" });
    api.registerTool(() => tool("jarvis_root_exec", "Jarvis Root Exec", "root.exec", {
      type: "object", additionalProperties: false, required: ["command"], properties: {
        command: { type: "string", minLength: 1, maxLength: 2000 }
      }
    }, "HIGH", true), { name: "jarvis_root_exec" });
  }
};
