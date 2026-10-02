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
  return {
    name,
    label,
    description: `Jarvis Android capability: ${capability}. Permission and audit policy is enforced by Jarvis.`,
    parameters,
    execute: async (_toolCallId, args) => callBridge(capability, args || {}, risk, requiresApproval)
  };
}

const empty = { type: "object", additionalProperties: false, properties: {} };

export default {
  id: "jarvis-device-bridge",
  name: "Jarvis Device Bridge",
  register(api) {
    api.registerTool(() => tool("jarvis_device_info", "Jarvis Device Info", "device.info", empty, "LOW", false), { name: "jarvis_device_info" });
    api.registerTool(() => tool("jarvis_camera_capture", "Jarvis Camera Capture", "camera.capture", empty, "MEDIUM", true), { name: "jarvis_camera_capture" });
    api.registerTool(() => tool("jarvis_accessibility_inspect", "Jarvis Accessibility Inspect", "accessibility.inspect", empty, "LOW", false), { name: "jarvis_accessibility_inspect" });
    api.registerTool(() => tool("jarvis_accessibility_click", "Jarvis Accessibility Click", "accessibility.click", {
      type: "object", additionalProperties: false, required: ["x", "y"], properties: {
        x: { type: "number" }, y: { type: "number" }
      }
    }, "HIGH", true), { name: "jarvis_accessibility_click" });
    api.registerTool(() => tool("jarvis_ocr_read", "Jarvis OCR Read", "ocr.read", empty, "LOW", false), { name: "jarvis_ocr_read" });
    api.registerTool(() => tool("jarvis_notification_read", "Jarvis Notification Read", "notification.read", empty, "MEDIUM", false), { name: "jarvis_notification_read" });
    api.registerTool(() => tool("jarvis_root_exec", "Jarvis Root Exec", "root.exec", {
      type: "object", additionalProperties: false, required: ["command"], properties: {
        command: { type: "string", minLength: 1, maxLength: 2000 }
      }
    }, "HIGH", true), { name: "jarvis_root_exec" });
  }
};
