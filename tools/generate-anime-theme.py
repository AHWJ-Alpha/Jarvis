"""Generate the Jarvis anime theme with the local A1111 SDXL checkpoint."""

import base64
import argparse
import json
import urllib.request
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / "artwork" / "themes"
RES = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
API = "http://127.0.0.1:7861"
MODEL = "sdxl\\kohaku-xl-epsilon-rev1.safetensors [516047db97]"
STYLE = (
    "masterpiece, best quality, polished anime film illustration, Japanese animation "
    "background painting, precise delicate linework, rich hand-painted lighting, "
    "soft cinematic atmosphere, cohesive cherry pink, coral and midnight blue palette"
)
NEGATIVE = (
    "text, letters, logo, watermark, signature, ui, interface, split screen, collage, "
    "low quality, blurry, bad anatomy, extra limbs, duplicate person, photorealistic, 3d render"
)
PROMPTS = {
    "banner": (
        "wide outdoor establishing shot from a hill above a Japanese city at dusk, "
        "gently curving street lined with blooming cherry trees leading toward distant "
        "rooftops, warm street lamps, distant mountains, pink petals in the breeze, "
        "expansive pale blush sky and delicate clouds across the left half, "
        "beautiful scenic anime background art, no people, no foreground building"
    ),
    "avatar": (
        "close-up portrait of one young adult anime woman with short dark blue hair "
        "and a subtle coral ribbon, warm confident expression, looking toward viewer, "
        "cherry blossoms and softly lit evening city behind her, centered head and shoulders, "
        "clean readable silhouette, beautiful expressive eyes"
    ),
}


def request(path, payload):
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(API + path, data, {"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=300) as response:
        return json.load(response)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--only", choices=("banner", "avatar"))
    args = parser.parse_args()
    ART.mkdir(parents=True, exist_ok=True)
    RES.mkdir(parents=True, exist_ok=True)
    request("/sdapi/v1/options", {"sd_model_checkpoint": MODEL})
    metadata = {"provider": "local A1111", "model": MODEL, "assets": []}
    for name, width, height, seed in (("banner", 1152, 640, 261028), ("avatar", 768, 768, 261027)):
        if args.only and name != args.only:
            continue
        payload = {
            "prompt": STYLE + ", " + PROMPTS[name],
            "negative_prompt": NEGATIVE + (
                ", interior, window, window frame, close building, fence, wall"
                if name == "banner" else ""
            ),
            "width": width, "height": height, "seed": seed,
            "steps": 28, "cfg_scale": 6.5,
            "sampler_name": "DPM++ 2M", "scheduler": "Karras",
            "batch_size": 1, "n_iter": 1,
            "send_images": True, "save_images": False,
        }
        result = request("/sdapi/v1/txt2img", payload)
        source = ART / f"anime-{name}-sdxl.png"
        source.write_bytes(base64.b64decode(result["images"][0].split(",")[-1]))
        with Image.open(source) as image:
            output = RES / ("theme_anime.webp" if name == "banner" else "theme_avatar_anime.webp")
            image.convert("RGB").save(output, quality=88, method=6)
        metadata["assets"].append({"name": name, "prompt": payload["prompt"],
                                   "negative_prompt": payload["negative_prompt"], "seed": seed,
                                   "size": [width, height], "steps": 28, "cfg_scale": 6.5,
                                   "sampler": "DPM++ 2M Karras", "source": str(source.relative_to(ROOT)),
                                   "resource": str(output.relative_to(ROOT))})
        print(f"Saved {name}: {output}", flush=True)
    path = ART / "anime-generation.json"
    if path.exists() and args.only:
        previous = json.loads(path.read_text(encoding="utf-8"))
        metadata["assets"] = [asset for asset in previous["assets"]
                              if asset["name"] != args.only] + metadata["assets"]
    path.write_text(
        json.dumps(metadata, ensure_ascii=False, indent=2), encoding="utf-8"
    )


if __name__ == "__main__":
    main()
