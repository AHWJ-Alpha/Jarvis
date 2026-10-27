"""Generate Jarvis theme illustrations using the user's local A1111 SDXL model.

No remote service or credential is used. Source art and reproducible prompts are
kept in artwork/themes; palette-mapped WebP assets are packaged in the APK.
"""
import base64
import io
import json
import time
import urllib.request
from pathlib import Path
from PIL import Image, ImageOps, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "artwork/themes"
RES = ROOT / "app/src/main/res/drawable-nodpi"
THEMES = [
    dict(name="deep", label="深海 · 潮汐来信", seed=260101,
         dark="#123C55", light="#E7F3F8", mid="#5496B3",
         prompt="masterpiece, best quality, landscape illustration, beautiful anime background painting, tranquil ocean cove, distant coastal islands, rolling translucent ocean waves, wispy clouds, airy mist, elegant fluid brushwork, intricate sea foam, panoramic composition, blue teal monochromatic palette, no people, no buildings, calm refined atmosphere"),
    dict(name="graphite", label="石墨 · 秩序之城", seed=260102,
         dark="#303844", light="#F1F2F4", mid="#939BA8",
         prompt="masterpiece, best quality, editorial architectural illustration, sculptural modernist pavilion, layered archways and staircases, beautifully composed geometric architecture, cut paper relief art, matte paper texture, soft ambient occlusion, panoramic composition, graphite gray monochromatic palette, empty quiet space, no people, minimalist premium design"),
    dict(name="forest", label="松林 · 林间手记", seed=260103,
         dark="#214D3C", light="#EDF5E9", mid="#83AA88",
         prompt="masterpiece, best quality, delicate watercolor landscape illustration, tranquil evergreen forest, tall pine trees surrounding a winding stream, layered misty mountains, botanical foliage, soft handmade watercolor paper texture, expressive natural brushwork, panoramic composition, sage green monochromatic palette, no people, no buildings, serene elegant atmosphere"),
    dict(name="ink", label="墨黑 · 山月无声", seed=260104,
         dark="#13151B", light="#CAD0E0", mid="#535E78",
         prompt="masterpiece, best quality, traditional sumi-e ink wash landscape painting, majestic distant mountains under a small silver moon, lake reflections, delicate silver ink on charcoal black paper, sparse brushstrokes, layered atmospheric fog, restrained negative space, panoramic composition, black white silver monochromatic palette, no people, no buildings, quiet contemplative elegant atmosphere"),
]


def request(path, payload=None, timeout=10):
    data = json.dumps(payload).encode() if payload is not None else None
    req = urllib.request.Request("http://127.0.0.1:7861" + path, data,
                                 {"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=timeout) as response:
        return json.load(response)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    RES.mkdir(parents=True, exist_ok=True)
    # Bounded startup wait. Each iteration yields quickly if the server is ready.
    deadline = time.monotonic() + 180
    while True:
        try:
            models = request("/sdapi/v1/sd-models")
            break
        except (OSError, TimeoutError):
            if time.monotonic() > deadline:
                raise
            time.sleep(2)
    print("Local SDXL ready", flush=True)
    metadata = {"provider": "local A1111 txt2img", "models": [m.get("title") for m in models], "themes": []}
    for spec in THEMES:
        payload = dict(prompt=spec["prompt"],
                       negative_prompt="text, letters, logo, watermark, signature, collage, split screen, border, ui, interface, people, portrait, oversaturated, blurry, low quality",
                       seed=spec["seed"], steps=24, cfg_scale=6.0,
                       sampler_name="DPM++ 2M", scheduler="Karras",
                       width=1152, height=640, batch_size=1, n_iter=1,
                       send_images=True, save_images=False)
        source = OUT / (spec["name"] + "-sdxl.png")
        if not source.exists():
            print("Generating " + spec["name"], flush=True)
            result = request("/sdapi/v1/txt2img", payload, timeout=240)
            source.write_bytes(base64.b64decode(result["images"][0].split(",")[-1]))
        original = Image.open(source).convert("RGB")
        gray = ImageOps.autocontrast(ImageOps.grayscale(original), cutoff=0.4)
        art = ImageOps.colorize(gray, black=spec["dark"], white=spec["light"], mid=spec["mid"])
        art.save(RES / ("theme_" + spec["name"] + ".webp"), quality=88, method=6)
        avatar = Image.open(ROOT / "app/src/main/res/drawable/jarvis_user_icon.png").convert("RGB")
        avatar = ImageOps.fit(avatar, (160, 160), method=Image.Resampling.LANCZOS)
        avatar = ImageOps.colorize(ImageOps.grayscale(avatar), black=spec["dark"], white=spec["light"], mid=spec["mid"])
        avatar.save(RES / ("theme_avatar_" + spec["name"] + ".webp"), quality=90, method=6)
        metadata["themes"].append({**spec, "generation": payload,
                                   "asset": "drawable-nodpi/theme_" + spec["name"] + ".webp"})
        print("Saved " + spec["name"], flush=True)
    (OUT / "generation.json").write_text(json.dumps(metadata, ensure_ascii=False, indent=2), encoding="utf-8")
    preview = Image.new("RGB", (1152, 720), "#ffffff")
    for index, spec in enumerate(THEMES):
        art = Image.open(RES / ("theme_" + spec["name"] + ".webp"))
        art = art.resize((576, 320), Image.Resampling.LANCZOS)
        x, y = (index % 2) * 576, (index // 2) * 360
        preview.paste(art, (x, y))
        ImageDraw.Draw(preview).text((x + 16, y + 332), spec["name"].upper(), fill=spec["dark"])
    preview.save(OUT / "theme-art-preview.jpg", quality=93)


if __name__ == "__main__":
    main()
