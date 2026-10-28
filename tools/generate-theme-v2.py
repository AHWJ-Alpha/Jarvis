"""Generate the six v2 theme illustrations (banner / avatar / texture) with local A1111.

Runs against the local AUTOMATIC1111 API only; no remote service, no credentials.
Sources and the machine-readable manifest live in artwork/themes-v2, packaged WebP
assets land in app/src/main/res/drawable-nodpi.

Pillow is required. The system interpreter has no Pillow, so run it with the WebUI
runtime that already ships one:

    "D:\sd-webui-aki(1)\sd-webui-aki-v4.11.1-cu128\python\python.exe" tools/generate-theme-v2.py
    ... tools/generate-theme-v2.py --theme deep_night      # regenerate one theme only
"""
import argparse
import base64
import json
import time
import urllib.request
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageStat

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / "artwork/themes-v2"
RES = ROOT / "app/src/main/res/drawable-nodpi"
API = "http://127.0.0.1:7861"
MODEL = "sdxl\\kohaku-xl-epsilon-rev1.safetensors [516047db97]"
SAMPLER, SCHEDULER, STEPS, CFG = "DPM++ 2M", "Karras", 24, 6.0
BASE_SEED = 270601
BOOT_WAIT = 180

STYLE = (
    "masterpiece, best quality, anime film illustration, Japanese animation background painting, "
    "anime key visual, cel-shaded brushwork, precise delicate linework, rich hand-painted lighting, "
    "soft cinematic atmosphere, 2d anime art"
)
NEGATIVE = (
    "text, letters, logo, watermark, signature, ui, interface, split screen, collage, border, "
    "frame, low quality, blurry, bad anatomy, extra limbs, duplicate person, photorealistic, "
    "3d render, photograph, abstract shapes, faceless, cluttered left side"
)
# The UI overlays a surface-fading scrim on the left ~63% of the banner, so the subject
# must sit on the right and the left must stay quiet.
BANNER_COMPOSITION = (
    "ultra wide anime banner composition, the whole subject sits inside the right third of the "
    "frame, the left half stays calm and almost empty, clean quiet negative space, no text"
)
STYLE_NOTE = (
    "style_generation v1 images were rendered before the STYLE prefix gained 'Japanese animation "
    "background painting, anime key visual, cel-shaded brushwork, 2d anime art'; v2 images match the "
    "current STYLE exactly. Textures carry no STYLE prefix, so they are reproduced identically either "
    "way. The v1 set was deliberately NOT re-rendered: reviewed art was kept over metadata symmetry, "
    "so v1 prompts are records of past runs, not reproducible with --force."
)
TEXTURE_SUFFIX = "flat seamless texture tile, even flat lighting, no objects, no text, uniform pattern"
# Light themes: the texture sits under body text, so it must stay near paper white. Prompt wording
# alone never guarantees that, hence paperify() below plus explicit negative tokens.
TEXTURE_PREFIX_LIGHT = "near-white, very low contrast, subtle paper fiber, fine grain, seamless tile"
# A patterned source stays patterned after paperify() flattens it, so the source itself must be
# near-uniform: black% > 2 means the model drew structure, and a fresh seed is cheaper than a fix.
TEXTURE_ATTEMPTS = 5
# Dark-theme ceilings: a glitchy tile breaks both of these.
TEXTURE_MAX_SD = 12.0
TEXTURE_MAX_SAT = 3.0
TEXTURE_NEGATIVE_NEON = (
    "glitch, glitch art, color blocks, saturated blocks, bright colors, neon sign, glow, bloom, "
    "kaleidoscope, ornament, mandala, high contrast, huge color faces, gradient background, "
    "chromatic aberration, datamosh, noisy rgb"
)
TEXTURE_NEGATIVE_LIGHT = (
    "kaleidoscope, pattern, ornament, high contrast, dark shapes, black areas, mandala, "
    "decorative motif, symmetrical pattern, ink splash, heavy brushstroke, vignette, "
    "grid, ruled lines, plaid, checkerboard, tile pattern, repeating shapes"
)

# gen size -> final size. Banners render straight at the 12:5 target: decoding a taller
# frame and cropping it throws away VAE work on this 8GB card for pixels that get discarded.
SIZES = {"banner": ((1536, 640), (1536, 640)), "avatar": ((768, 768), (512, 512)),
         "texture": ((768, 768), (512, 512))}
QUALITY = {"banner": 82, "avatar": 85, "texture": 78}
BUDGET = {"banner": 220_000, "avatar": 80_000, "texture": 120_000}

THEMES = [
    dict(id="deep_night", label="深海夜航", dark=True,
         palette=("#0A1A2B", "#1B3A5C", "#9EC6E0"), seed=BASE_SEED,
         banner="anime night seascape, a tall lighthouse on a cliff at the right edge of the frame "
                "with its beam sweeping to the left, a small sailboat with a warm lantern below it, "
                "the left two thirds is open empty dark sea and starry sky, drifting sea mist",
         avatar="close-up portrait of one young adult anime girl with short black hair, profile view "
                "turning slightly toward viewer, sea breeze lifting her hair, deep blue night ocean "
                "bokeh behind her, calm melancholic expression",
         texture="subtle dark navy grain noise texture, fine film grain, faint ocean shimmer"),
    dict(id="sakura_night", label="樱夜校园", dark=True,
         palette=("#160E1C", "#3D2338", "#E8B7C8"), seed=BASE_SEED + 10,
         banner="Japanese school building at night along a cherry blossom lined path, warm glowing "
                "street lamps, dusk afterglow along the horizon, drifting pink petals",
         avatar="close-up portrait of one anime schoolgirl in a sailor uniform, cherry blossoms and "
                "softly glowing night lanterns behind her, gentle smile, softly lit evening",
         texture="dark plum paper texture with faint falling petal specks, very subtle grain"),
    dict(id="neon_cyber", label="霓虹赛博", dark=True,
         palette=("#0B0A18", "#2A1147", "#22D3EE"), seed=BASE_SEED + 20,
         banner="rainy neon city street at night, holographic signage, wet asphalt reflecting magenta "
                "and cyan light, tall towers receding into purple fog",
         avatar="close-up portrait of one cyberpunk anime girl wearing headphones, neon rim light "
                "outline along her silhouette, magenta and cyan glow, confident expression",
         texture="near-black deep navy background, extremely thin 1px cyan and magenta neon grid "
                 "lines, fine scanlines, very low contrast, dark tech surface, flat even lighting",
         texture_negative=TEXTURE_NEGATIVE_NEON, no_native_tiling=True, texture_tint=0.0,
         texture_base="#04070D", texture_grain="procedural_neon"),
    dict(id="morning_bento", label="晨光便当", dark=False,
         palette=("#4A3520", "#C99A5B", "#FFF3DC"), seed=BASE_SEED + 30,
         banner="anime morning kitchen, a colourful handmade bento box with rice, tamagoyaki and "
                "greens on the right foreground of a wooden counter, warm sunlight through paper "
                "shoji screens, soft rising steam, honey and cream tones, cozy tidy",
         avatar="close-up portrait of one cheerful anime girl wearing a linen apron, warm morning "
                "sunlight, bright kitchen behind her, bright energetic smile",
         texture="plain near-white cream paper, fine fiber grain, very subtle warm flecks",
         texture_prefix=TEXTURE_PREFIX_LIGHT, texture_negative=TEXTURE_NEGATIVE_LIGHT),
    dict(id="ink_moon", label="水墨山月", dark=False,
         palette=("#2B2B2B", "#7C8592", "#F5F3EC"), seed=BASE_SEED + 40, bias_threshold=0.75,
         banner="anime film illustration in sumi-e ink wash, one ink brush mountain ridge rising on "
                "the right side of the frame, a pale moon in the upper right corner, the left two "
                "thirds is plain empty rice paper with a single faint wash, dry brush texture",
         avatar="close-up portrait of one anime girl painted in delicate ink wash style, pale "
                "watercolour washes, rice paper texture, sparse elegant linework",
         texture="plain off-white rice paper, fine fiber grain, the faintest grey wash",
         texture_prefix=TEXTURE_PREFIX_LIGHT, texture_negative=TEXTURE_NEGATIVE_LIGHT),
    dict(id="paper_minimal", label="纸感极简", dark=False,
         palette=("#3A3A38", "#B8B2A7", "#FBFAF7"), seed=BASE_SEED + 50,
         banner="flat paper-cut anime illustration, layered torn paper hills and a small torii gate "
                "on the right, one muted teal accent colour, generous off-white space on the left, "
                "crisp soft paper shadows",
         avatar="close-up portrait of one anime girl with a clear simple friendly face and big "
                "expressive eyes, flat paper-cut illustration style, clean thin outlines, one muted "
                "teal accent, plain off-white background, head and shoulders",
         texture="plain off-white paper surface, fine random sand-like fibre grain, "
                 "no repeating shapes, even tone",
         texture_prefix=TEXTURE_PREFIX_LIGHT, texture_negative=TEXTURE_NEGATIVE_LIGHT,
         texture_grain="procedural"),
]


def request(path, payload=None, timeout=1200):
    data = json.dumps(payload).encode("utf-8") if payload is not None else None
    req = urllib.request.Request(API + path, data, {"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=timeout) as response:
        return json.load(response)


def wait_for_api():
    deadline = time.monotonic() + BOOT_WAIT
    while True:
        try:
            return request("/sdapi/v1/sd-models", timeout=10)
        except OSError:
            if time.monotonic() > deadline:
                raise SystemExit("A1111 did not answer within %ss" % BOOT_WAIT)
            time.sleep(2)


def wait_idle(limit=180):
    """Clear any residual job first: A1111 queues instead of rejecting overlapping work."""
    deadline = time.monotonic() + limit
    while time.monotonic() < deadline:
        try:
            if not request("/sdapi/v1/progress", timeout=15).get("state", {}).get("job"):
                return
        except OSError:
            time.sleep(3)
            continue
        request("/sdapi/v1/interrupt", {}, timeout=15)
        time.sleep(3)


def hex_rgb(value):
    return tuple(int(value[i:i + 2], 16) for i in (1, 3, 5))


def palette_overlay(size, palette, strength):
    """Vertical three-stop wash so every asset shares its theme palette."""
    dark, mid, light = (hex_rgb(c) for c in palette)
    width, height = size
    wash = Image.new("RGB", (width, height))
    draw = ImageDraw.Draw(wash)
    for y in range(height):
        t = y / max(1, height - 1)
        if t < 0.5:
            a, b, k = dark, mid, t * 2
        else:
            a, b, k = mid, light, (t - 0.5) * 2
        draw.line([(0, y), (width, y)],
                  fill=tuple(int(a[i] + (b[i] - a[i]) * k) for i in range(3)))
    return wash, strength


def tint(image, palette, strength, size):
    image = image.convert("RGB").resize(size, Image.Resampling.LANCZOS)
    wash, k = palette_overlay(size, palette, strength)
    return Image.blend(image, wash, k)


def right_bias(image, threshold=0.45):
    """Flip when saliency sits left of centre: the UI scrim covers the left 63% of a banner."""
    edges = image.convert("L").filter(ImageFilter.FIND_EDGES)
    width, height = edges.size
    step = max(1, width // 96)
    total = weighted = 0.0
    for x in range(0, width, step):
        column = sum(edges.crop((x, 0, x + step, height)).convert("L").resize((1, 1)).getdata())
        total += column
        weighted += column * x
    if not total:
        return image, None
    centroid = weighted / total / width
    if centroid < threshold:
        return image.transpose(Image.Transpose.FLIP_LEFT_RIGHT), round(centroid, 3)
    return image, round(centroid, 3)


def seam_error(image):
    """Mean absolute difference between the opposing edges of a candidate tile."""
    width, height = image.size
    vertical = ImageChops.difference(image.crop((0, 0, 1, height)),
                                     image.crop((width - 1, 0, width, height))).convert("L")
    horizontal = ImageChops.difference(image.crop((0, 0, width, 1)),
                                       image.crop((0, height - 1, width, height))).convert("L")
    return (ImageStat.Stat(vertical).mean[0] + ImageStat.Stat(horizontal).mean[0]) / 2.0


def seam_axes(image):
    width, height = image.size
    lr = ImageStat.Stat(ImageChops.difference(image.crop((0, 0, 1, height)),
                                              image.crop((width - 1, 0, width, height))).convert("L")).mean[0]
    tb = ImageStat.Stat(ImageChops.difference(image.crop((0, 0, width, 1)),
                                              image.crop((0, height - 1, width, height))).convert("L")).mean[0]
    return lr, tb


def make_tileable(image, tolerance=3.0, trust=False):
    """Mirror-wrap into a 2x2 block and keep the centre: seamless by construction.

    Both wrap axes are judged separately: a tile can look clean left-to-right and still show a
    hard line top-to-bottom, which is what an averaged score hides.
    """
    lr, tb = seam_axes(image)
    # trust=True is only for tiles that are seamless by construction (the procedural neon grid with a
    # periodic noise patch). Everything else is judged on the strict absolute floor: letting a sampled
    # tile through on a relative comparison shipped a 9.6-level vertical seam on morning_bento.
    if trust or (lr <= tolerance and tb <= tolerance):
        return image, round(max(lr, tb), 1), "native-tiling"
    width, height = image.size
    canvas = Image.new("RGB", (width * 2, height * 2))
    canvas.paste(image, (0, 0))
    canvas.paste(image.transpose(Image.Transpose.FLIP_LEFT_RIGHT), (width, 0))
    canvas.paste(image.transpose(Image.Transpose.FLIP_TOP_BOTTOM), (0, height))
    canvas.paste(image.transpose(Image.Transpose.ROTATE_180), (width, height))
    tile = canvas.crop((width // 2, height // 2, width // 2 + width, height // 2 + height))
    lr, tb = seam_axes(tile)
    return tile, round(max(lr, tb), 1), "mirror-wrap"


def save_budgeted(image, path, quality, budget):
    """Write WebP, stepping quality down until the byte budget is met."""
    used = quality
    while True:
        image.save(path, quality=used, method=6)
        size = path.stat().st_size
        if size <= budget or used <= 40:
            return used, size
        used -= 6


def black_pct(image, threshold=64):
    """Share of near-black pixels: the metric that caught the kaleidoscope textures."""
    gray = image.convert("L")
    hist = gray.histogram()
    return 100.0 * sum(hist[:threshold]) / float(gray.width * gray.height)


def saturated_pct(image, s_min=153, v_min=102):
    """Share of pixels that are both saturated (S>0.6) and bright (V>0.4) — the glitch metric."""
    data = list(image.convert("HSV").getdata())
    hot = sum(1 for _, s, v in data if s > s_min and v > v_min)
    return 100.0 * hot / float(len(data))


def percentile(gray, point):
    total, acc = gray.width * gray.height, 0
    for value, count in enumerate(gray.histogram()):
        acc += count
        if acc >= total * point / 100.0:
            return value
    return 255


def texture_qa(image):
    gray = image.convert("L")
    stat = ImageStat.Stat(gray)
    return {"gray_mean": round(stat.mean[0], 1), "gray_stddev": round(stat.stddev[0], 2),
            "contrast_pct": round(100.0 * stat.stddev[0] / 255.0, 2),
            "black_pct": round(black_pct(image), 2),
            "p1": percentile(gray, 1), "p5": percentile(gray, 5),
            "saturated_pct": round(saturated_pct(image), 2)}


# Cell count per tile: 512/24 is not integral, so 22 cells give an even ~23.3px pitch that still
# wraps exactly. Line deltas are calibrated *after* WebP encode, not on the drawing: q78 smooths a
# 1px line to roughly half its drawn contrast, so the drawn ~10/255 luminance step lands at a
# measured 2.75% of range (2.5-3.0% band) with stddev ~3.3 and zero saturated pixels.
NEON_CELLS = 22
NEON_LINE_DELTAS = ((3, 11, 14), (24, 6, 14))  # cyan-ish verticals, magenta-ish horizontals


def procedural_neon_grid(size, base_hex, cells=NEON_CELLS, deltas=NEON_LINE_DELTAS, noise=2):
    """Near-black tile with 1px cyan/magenta grid lines.

    Used for neon_cyber: SDXL renders this brief as saturated glitch blocks, and the UI needs a
    dark base with sub-8% structure instead. Lines sit at V<=0.09 so they can never read as a
    bright coloured face, and rounding the positions to the cell grid keeps the wrap seamless.
    """
    base = hex_rgb(base_hex)
    lines = [tuple(min(255, c + d) for c, d in zip(base, delta)) for delta in deltas]
    img = Image.new("RGB", size, base)
    draw = ImageDraw.Draw(img)
    # Half-cell offset: a line sitting exactly on the tile edge makes the wrap look discontinuous to
    # any edge-similarity test (and forces a mirror wrap that stamps a 2x2 symmetry on the tile).
    # With the edges falling in cell middles the grid stays even and the tile wraps as drawn.
    for i in range(cells):
        x = int(round((i + 0.5) * size[0] / float(cells)))
        draw.line([(x, 0), (x, size[1] - 1)], fill=lines[0])
        y = int(round((i + 0.5) * size[1] / float(cells)))
        draw.line([(0, y), (size[0] - 1, y)], fill=lines[1])
    if noise:
        # Periodic noise from a 128px patch instead of a full-tile sample: the result is seamless by
        # construction, so make_tileable() leaves the grid untouched (a mirror wrap would impose a
        # faint 2x2 quadrant symmetry that reads as a large-scale block).
        patch_size = 128
        patch = Image.effect_noise((patch_size, patch_size), 6).point(
            lambda v: int(v * noise / 255.0))
        grain = Image.new("L", size)
        for gx in range(0, size[0], patch_size):
            for gy in range(0, size[1], patch_size):
                grain.paste(patch, (gx, gy))
        img = Image.merge("RGB", [ImageChops.add(c, grain) for c in img.split()])
    return img


def paperify(image, palette, target_sd=11.0):
    """Flatten a light texture onto paper white, keeping only its fibre detail.

    Body text sits on these tiles, so the model's output cannot be trusted to stay near white:
    detail is re-centred on the theme light colour and driven to ~4% of range (target_sd 11 of
    255) whatever contrast the model returned.
    """
    base = hex_rgb(palette[2])
    gray = image.convert("L")
    flat = Image.new("L", gray.size, int(round(ImageStat.Stat(gray).mean[0])))
    deviation = ImageChops.difference(gray, flat)
    scale, out = 1.0, image
    for _ in range(5):
        detail = deviation.point(lambda v, k=scale: min(255, int(v * k)))
        out = Image.merge("RGB", [ImageChops.subtract(Image.new("L", gray.size, c), detail)
                                  for c in base])
        sd = ImageStat.Stat(out.convert("L")).stddev[0]
        if sd < 1e-6 or abs(sd - target_sd) <= 0.6:
            break
        scale *= target_sd / sd
    return out


def procedural_grain(size, palette, target_sd=11.0, shift=0):
    """Isotropic fibre noise for themes where SDXL only returns ruled/blocky structure.

    Used when spec["texture_grain"] == "procedural": the tile is generated, not sampled, so it
    cannot come back with a repeating ornament; contrast is held to the same ~4% band.
    """
    base = hex_rgb(palette[2])
    noise = Image.effect_noise(size, 40)
    if shift:
        noise = ImageChops.offset(noise, shift % size[0], (shift * 3) % size[1])
    deviation = ImageChops.difference(noise, Image.new("L", size, 128))
    scale, out = 1.0, None
    for _ in range(6):
        detail = deviation.point(lambda v, k=scale: min(90, int(v * k)))
        out = Image.merge("RGB", [ImageChops.add(Image.new("L", size, max(0, c - 45)), detail)
                                  for c in base])
        sd = ImageStat.Stat(out.convert("L")).stddev[0]
        if abs(sd - target_sd) <= 0.6:
            break
        scale *= target_sd / max(sd, 1e-6)
    return out


def prompt_for(kind, spec):
    """Single source of truth for prompts: generation and metadata checks both use it."""
    if kind == "banner":
        return STYLE + ", " + BANNER_COMPOSITION + ", " + spec["banner"]
    if kind == "avatar":
        return STYLE + ", " + spec["avatar"] + ", centred head and shoulders, clean readable silhouette"
    prefix = spec.get("texture_prefix")
    return (prefix + ", " if prefix else "") + spec["texture"] + ", " + TEXTURE_SUFFIX


def negative_for(kind, spec):
    extra = spec.get("texture_negative") if kind == "texture" else None
    return NEGATIVE + (", " + extra if extra else "")


def generate(kind, spec, index, seed_offset=0):
    gen_size, _ = SIZES[kind]
    seed = spec["seed"] + index + seed_offset
    prompt = prompt_for(kind, spec)
    payload = {"prompt": prompt, "negative_prompt": negative_for(kind, spec), "seed": seed,
               "steps": STEPS, "cfg_scale": CFG, "sampler_name": SAMPLER, "scheduler": SCHEDULER,
               "width": gen_size[0], "height": gen_size[1], "batch_size": 1, "n_iter": 1,
               "send_images": True, "save_images": False,
               "override_settings": {"sd_model_checkpoint": MODEL}}
    # Native circular padding makes SDXL produce blocky grids (and sometimes blanks); the light
    # textures are tiled by make_tileable() instead. Dark textures keep their historical request.
    if kind == "texture" and spec["dark"] and not spec.get("no_native_tiling"):
        payload["tiling"] = True
    result = request("/sdapi/v1/txt2img", payload)
    source = ART / spec["id"] / (kind + ".png")
    source.parent.mkdir(parents=True, exist_ok=True)
    source.write_bytes(base64.b64decode(result["images"][0].split(",")[-1]))
    info = {"prompt": prompt, "negative_prompt": negative_for(kind, spec), "seed": seed,
            "gen_size": list(gen_size), "request": {k: payload[k] for k in
            ("steps", "cfg_scale", "sampler_name", "scheduler")}}
    return source, info


def build(spec, index, kinds, force, offline=False, seed_offset=0):
    images = {}
    for kind in ("banner", "avatar", "texture"):
        if kinds and kind not in kinds:
            continue
        source = ART / spec["id"] / (kind + ".png")
        if offline and not source.exists():
            raise SystemExit("--offline needs an existing source PNG: %s" % source)
        if force and offline:
            raise SystemExit("--force and --offline contradict each other")
        if force or not source.exists():
            print("  generating %s/%s" % (spec["id"], kind), flush=True)
            offset, attempt = seed_offset, 1
            while True:
                source, info = generate(kind, spec, index, offset)
                if kind != "texture" or spec.get("texture_grain"):
                    break
                with Image.open(source) as raw:
                    raw_black = black_pct(raw)
                    raw_sd = ImageStat.Stat(raw.convert("L")).stddev[0]
                info["source_black_pct"] = round(raw_black, 2)
                info["source_stddev"] = round(raw_sd, 2)
                raw_sat = saturated_pct(raw.convert("RGB"))
                info["source_saturated_pct"] = round(raw_sat, 2)
                if spec["dark"]:
                    passed = raw_sd <= TEXTURE_MAX_SD and raw_sat <= TEXTURE_MAX_SAT
                    detail = "sd %.1f (>%.0f) / sat %.1f%% (>%.1f%%)" % (
                        raw_sd, TEXTURE_MAX_SD, raw_sat, TEXTURE_MAX_SAT)
                else:
                    passed = raw_black <= 2.0 and raw_sd >= 3.0
                    detail = "black %.1f%% / sd %.1f" % (raw_black, raw_sd)
                if passed or attempt >= TEXTURE_ATTEMPTS:
                    break
                print("    source %s rejected: new seed" % detail, flush=True)
                attempt, offset = attempt + 1, offset + 100
            info["attempts"] = attempt
        else:
            info = None
        gen_size, final = SIZES[kind]
        with Image.open(source) as opened:
            art = opened.convert("RGB")
            if kind == "banner":
                art = art.resize((final[0], gen_size[1]), Image.Resampling.LANCZOS)
                left = (art.width - final[0]) // 2
                art = art.crop((left, (art.height - final[1]) // 2, left + final[0],
                                (art.height - final[1]) // 2 + final[1]))
            strength = spec.get("texture_tint", 0.18) if kind == "texture" else 0.08
            art = tint(art, spec["palette"], strength, final) if strength else art.convert("RGB")
            if kind == "texture" and spec.get("texture_grain") == "procedural_neon":
                art = procedural_neon_grid(final, spec["texture_base"])
            elif kind == "texture" and spec.get("texture_grain") == "procedural":
                art = procedural_grain(final, spec["palette"], shift=spec["seed"])
            elif kind == "texture" and not spec["dark"]:
                art = paperify(art, spec["palette"])
            tiling, flipped = None, None
            if kind == "banner":
                art, flipped = right_bias(art, spec.get("bias_threshold", 0.45))
            if kind == "texture":
                art, seam, tiling = make_tileable(
                    art, trust=spec.get("texture_grain") == "procedural_neon")
        path = RES / ("theme_%s_%s.webp" % (spec["id"], kind))
        quality, size = save_budgeted(art, path, QUALITY[kind], BUDGET[kind])
        if info is None:
            previous = (load_manifest().get("themes") or [])
            seed = spec["seed"] + index
            info = {"prompt": "reused existing %s" % source.name, "seed": seed,
                    "gen_size": list(gen_size)}
            for old in previous:
                if old.get("id") == spec["id"] and kind in old.get("images", {}):
                    info = {k: old["images"][kind].get(k, info.get(k)) for k in
                            ("prompt", "negative_prompt", "seed", "gen_size", "request", "tiling",
                             "attempts", "source_black_pct", "source_stddev")}
        entry = dict(info)
        entry.update({"size": list(final), "source": str(source.relative_to(ROOT)),
                      "output": str(path.relative_to(ROOT)), "bytes": size,
                      "webp_quality": quality})
        if tiling:
            entry["tiling"] = tiling
            entry["seam_max"] = seam
        if flipped is not None:
            entry["saliency_centroid"] = flipped
            entry["flipped_for_right_bias"] = flipped < 0.45
        if kind == "texture":
            # Measure the encoded file, not the in-memory image: q78 is what the device renders, and
            # it is what moved these numbers (a 1px line loses about half its contrast to it).
            with Image.open(path) as saved:
                entry["qa"] = dict(entry.get("qa") or {}, **texture_qa(saved.convert("RGB")))
            entry["grain"] = spec.get("texture_grain", "sdxl")
        # v2 == the prompt recorded here is exactly what this script would send today.
        entry["style_generation"] = "v2" if entry.get("prompt") == prompt_for(kind, spec) else "v1"
        images[kind] = entry
        print("  %s %dx%d %dKB q%d" % (path.name, final[0], final[1], round(size / 1024), quality), flush=True)
    return images


def load_manifest():
    path = ART / "manifest.json"
    if not path.exists():
        return {}
    return json.loads(path.read_text(encoding="utf-8"))


def main():
    parser = argparse.ArgumentParser(description="Generate the v2 theme artwork set.")
    parser.add_argument("--theme", action="append", metavar="ID",
                       help="only this theme id (repeatable); other themes in the manifest are preserved")
    parser.add_argument("--only", choices=("banner", "avatar", "texture"), action="append",
                       help="only this asset kind (repeatable)")
    parser.add_argument("--force", action="store_true", help="re-request images even if source PNGs exist")
    parser.add_argument("--seed-offset", type=int, default=0, metavar="N",
                       help="shift every seed by N to explore a different seed family")
    parser.add_argument("--offline", action="store_true",
                       help="never call A1111: rebuild WebP and manifest from existing source PNGs")
    parser.add_argument("--list", action="store_true", help="list theme ids and exit")
    args = parser.parse_args()

    if args.list:
        for spec in THEMES:
            print("%-14s %s" % (spec["id"], spec["label"]))
        return
    unknown = [t for t in (args.theme or []) if t not in {s["id"] for s in THEMES}]
    if unknown:
        raise SystemExit("unknown theme id(s): " + ", ".join(unknown))

    ART.mkdir(parents=True, exist_ok=True)
    RES.mkdir(parents=True, exist_ok=True)
    models = None
    if not args.offline:
        models = wait_for_api()
        wait_idle()
        print("A1111 ready and idle, model %s" % MODEL, flush=True)

    manifest = load_manifest() or {"provider": "local A1111", "api": API}
    manifest.update({"generator": "tools/generate-theme-v2.py", "model": MODEL,
                     "sampler": SAMPLER + " " + SCHEDULER, "steps": STEPS,
                     "cfg_scale": CFG,
                     "style_note": STYLE_NOTE})
    if models is not None:
        manifest["available_models"] = [m.get("title") for m in models]
    by_id = {t.get("id"): t for t in manifest.get("themes", [])}
    for index, spec in enumerate(THEMES):
        if args.theme and spec["id"] not in args.theme:
            continue
        print("[%s] %s" % (spec["id"], spec["label"]), flush=True)
        images = build(spec, index, args.only, args.force, args.offline, args.seed_offset)
        merged = dict(by_id.get(spec["id"], {}))
        merged.update({"id": spec["id"], "label": spec["label"], "dark": spec["dark"],
                       "palette": list(spec["palette"]), "base_seed": spec["seed"]})
        merged["images"] = dict(merged.get("images", {}), **images)
        by_id[spec["id"]] = merged
    manifest["themes"] = [by_id[s["id"]] for s in THEMES if s["id"] in by_id]
    manifest["generated_at"] = time.strftime("%Y-%m-%dT%H:%M:%S")
    (ART / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2),
                                       encoding="utf-8")
    print("manifest written: %s" % (ART / "manifest.json"), flush=True)


if __name__ == "__main__":
    main()
