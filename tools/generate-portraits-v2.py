"""Generate the per-theme character illustration set for the v2 themes.

Kinds (one image per A1111 request, size + KB printed as each file lands):
  portrait_room   768x1024  conversation empty state, top third kept quiet for text
  portrait_header 1536x640  settings header, character on the right third
  scene_a         768x1024  empty-state rotation 1 (character + place)
  scene_b         768x1024  empty-state rotation 2 (different framing: close-up / full)
  thumb           512x512   small avatar-ish thumbnail, head centred and large

Reuses the v2 theme pipeline (STYLE prefix, A1111 client, tint, right_bias, WebP budget) by
loading tools/generate-theme-v2.py as a module, so prompts and colour handling stay in one place.

    "D:\sd-webui-aki(1)\sd-webui-aki-v4.11.1-cu128\python\python.exe" tools/generate-portraits-v2.py
    ... tools/generate-portraits-v2.py --theme deep_night --kind thumb --force

Boundary: every prompt describes a clearly adult woman, fully clothed, and the negative list
carries an explicit no-nudity / no-minor block. content_rating 'suggestive-non-explicit'.
"""
import argparse
import base64
import importlib.util
import json
import time
from pathlib import Path

from PIL import Image, ImageFilter, ImageStat

ROOT = Path(__file__).resolve().parents[1]


def _load_pipeline():
    path = ROOT / "tools/generate-theme-v2.py"
    spec = importlib.util.spec_from_file_location("theme_v2", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


v2 = _load_pipeline()

ART, RES = v2.ART, v2.RES
CONTENT_RATING = "suggestive-non-explicit"

# Hard block: nudity, exposed anatomy, sexual framing, and any minor read of the character.
BOUNDARY = (
    "nude, nudity, nsfw, explicit, topless, nipples, areola, exposed breasts, exposed chest, "
    "see-through fabric, transparent clothing, wet see-through shirt, lingerie only, "
    "exposed underwear, panties visible, cameltoe, crotch focus, spread legs, sexual pose, "
    "sexual act, suggestive object close-up, torn clothes, ripped uniform, clothes pulled open, "
    "bare shoulders, off-shoulder clothing, deep neckline, plunging neckline, bare chest, "
    "childlike face, oversized head, big round eyes, cute baby face, small chin, "
    "child, loli, underage, minor, teen body, kid, small child, toddler proportions, baby face, "
    "chibi"
)
# Positive phrasing that keeps the frame on the near side of the line: adult, clothed, tasteful.
BOUNDARY_POSITIVE = (
    "one adult woman in her late twenties, mature adult character design, narrow jaw and adult "
    "face proportions, small head relative to shoulders, fully clothed in opaque fabric that "
    "covers the shoulders and chest, tasteful provocative mood without exposure"
)

ROOM_COMPOSITION = (
    "vertical anime character illustration for a phone UI empty state, medium shot from the waist "
    "up, the character stands low in the frame with generous empty headroom above her head, her "
    "head starts below the top third line, the top third of the canvas is calm nearly empty "
    "background reserved for interface text, camera at eye level, shallow depth of field, no text"
)
SCENE_COMPOSITION = (
    "vertical anime scene illustration for a phone UI empty state, the character seen together "
    "with her place, calm nearly empty background band across the top of the canvas, the subject "
    "sits in the lower two thirds, soft depth of field, no text"
)
CLOSEUP_COMPOSITION = (
    "vertical anime close-up illustration, the character's face and shoulders fill the lower two "
    "thirds of the canvas, calm empty bokeh above her head, soft depth of field, no text"
)
HEADER_COMPOSITION = (
    "ultra wide anime character banner, the whole character sits inside the right third of the "
    "frame, the left two thirds is calm almost empty background reserved for a title, "
    "clean quiet negative space, no text"
)
THUMB_COMPOSITION = (
    "square anime icon illustration, upper body from the chest up, the whole head and shoulders are "
    "fully inside the canvas with clear margin around the hair, the head is centred, plain simple "
    "background, clear readable silhouette at small sizes, no text"
)

KINDS = {
    "portrait_room": dict(gen=(768, 1024), final=(768, 1024), quality=84, budget=260_000,
                          composition=ROOM_COMPOSITION, vertical=True),
    "portrait_header": dict(gen=(1536, 640), final=(1536, 640), quality=82, budget=220_000,
                            composition=HEADER_COMPOSITION, vertical=False),
    "scene_a": dict(gen=(768, 1024), final=(768, 1024), quality=84, budget=260_000,
                    composition=SCENE_COMPOSITION, vertical=True),
    "scene_b": dict(gen=(768, 1024), final=(768, 1024), quality=84, budget=260_000,
                    composition=CLOSEUP_COMPOSITION, vertical=True),
    "thumb": dict(gen=(768, 768), final=(512, 512), quality=85, budget=80_000,
                  composition=THUMB_COMPOSITION, vertical=False),
}
KIND_ORDER = ("portrait_room", "portrait_header", "scene_a", "scene_b", "thumb")

THUMB_NEGATIVE = ("cropped head, forehead cut off, extreme close-up, zoomed face, "
                 "half face out of frame")
HEADER_FIX = {"morning_bento": "mirror"}

ROOM_NEGATIVE = (
    "close-up, tight crop, cropped head, head touching the top edge, face fills the frame, "
    "no headroom, zoomed in"
)

# ui_budget: what the Kotlin layer must honour, so the artwork can never take over the screen.
UI_BUDGET = {
    "portrait_room": {
        "usage": "conversation empty state, one of three rotating illustrations",
        "rotation_group": ["portrait_room", "scene_a", "scene_b"],
        "rotation_rule": "never repeat the same illustration twice inside one session",
        "max_height_dp": 200,
        "width": "fillMaxWidth",
        "max_height_dp_note": "height is capped at 200dp; aspect 3:4 means the WebP is scaled, "
                              "never cropped",
        "placement": "inside the scrolling content flow",
        "forbidden": ["overlay on top of content", "full-screen background"],
    },
    "portrait_header": {
        "usage": "settings header illustration",
        "max_height_dp": 140,
        "width": "fillMaxWidth",
        "placement": "below the title row, never behind it",
        "forbidden": ["covering the title text", "full-screen background"],
    },
    "scene_a": {
        "usage": "conversation empty state rotation slot 1",
        "rotation_group": ["portrait_room", "scene_a", "scene_b"],
        "rotation_rule": "never repeat the same illustration twice inside one session",
        "max_height_dp": 200,
        "width": "fillMaxWidth",
        "placement": "inside the scrolling content flow",
        "forbidden": ["overlay on top of content", "full-screen background"],
    },
    "scene_b": {
        "usage": "conversation empty state rotation slot 2",
        "rotation_group": ["portrait_room", "scene_a", "scene_b"],
        "rotation_rule": "never repeat the same illustration twice inside one session",
        "max_height_dp": 200,
        "width": "fillMaxWidth",
        "placement": "inside the scrolling content flow",
        "forbidden": ["overlay on top of content", "full-screen background"],
    },
    "thumb": {
        "usage": "small icon for conversation / workspace cards",
        "max_height_dp": 72,
        "width": "fillMaxWidth",
        "placement": "56-72dp square, circular or 24dp rounded corner crop, subject centred",
        "forbidden": ["large background", "full-screen background"],
    },
}

# Per-theme character direction: same person across all five kinds, mood fixed per theme.
CHARACTERS = {
    "deep_night": dict(
        mood="night sailing, sea wind, cool restrained",
        room="a cool-eyed adult woman with short black hair and a fitted dark navy turtleneck "
             "sweater standing at a dark pier railing with her body half turned away, she looks back "
             "at the viewer over her shoulder with a cool sidelong gaze and the faintest smile, one "
             "hand resting on the railing, wind lifting her hair, cool blue mono night palette, the "
             "dark sea and a distant lighthouse beam behind her, salt mist sheen on her skin",
        header="the same adult woman with short black hair in a dark navy high-collar knit sweater "
               "standing at a lit railing on the right side of the frame, medium shot from the waist "
               "up, half turned toward the viewer with a cool level stare, warm lantern rim light "
               "along her face and hair, dark sea and two slow lighthouse beams on the left",
        scene_a="the same adult woman sitting on a mooring bollard at a night harbour, elbows on "
                "her knees, chin on her hand, watching a small sailboat with a warm lantern, a "
                "duffel coat over her shoulders, cold blue moonlight and wet stone",
        scene_b="close-up of the same adult woman's face half turned to the viewer, high-collar "
                "navy sweater zipped to the throat and covering her shoulders, eyes narrowed against "
                "the salt wind, a strand of hair across her lips, faint tired smile, deep blue bokeh "
                "of harbour lamps, glistening skin on her cheek",
        thumb="head and shoulders of the same adult woman, face centred and large, calm steady gaze "
              "straight at the viewer, navy sweater collar, plain deep blue gradient background",
    ),
    "sakura_night": dict(
        mood="sailor uniform, night lanterns, sweet",
        room="a sweet-faced adult university student in a neat navy sailor-collar uniform with a "
             "loosely tied scarf, she sits on a stone step with her knees together and tilts her "
             "head, cheeks slightly flushed, shy warm smile, cherry petals in her hair, glowing "
             "paper lantern light behind her",
        header="the same adult student in her navy sailor-collar uniform leaning on a school "
               "corridor railing at night, one hand at her chest, an embarrassed but pleased smile, "
               "pink petals drifting past, warm lantern glow rimming her silhouette",
        scene_a="the same adult student walking home under cherry trees at night, a buttoned navy "
                "school blazer with a white sailor collar and long opaque sleeves, a school bag on "
                "one shoulder, she glances up at the lanterns, petals caught in her hair, warm "
                "paper-lantern light and a dark blue sky",
        scene_b="close-up of the same adult student looking up at the viewer with bright gentle "
                "eyes and a shy smile, one petal on her cheek, warm lantern glow across a softly lit "
                "face, navy sailor collar buttoned to the throat, soft plum bokeh",
        thumb="head and shoulders of the same sweet-faced adult student, whole head inside the "
              "frame, a soft pleased smile and a small hair clip, navy sailor collar buttoned to the "
              "throat, plain dark plum background",
    ),
    "morning_bento": dict(
        mood="kitchen apron, morning light, energetic",
        room="a cheerful adult woman in a cream linen apron over a rolled-sleeve shirt, leaning on "
             "a wooden kitchen counter toward the camera with a bright grin, a fingertip of flour "
             "on her cheek, hair tied back in a loose bun, warm morning sunlight and soft steam "
             "behind her",
        header="the same cheerful adult woman turning from a kitchen counter with a bright laugh, "
               "wooden spoon in hand, hair falling loose from a bun, backlit by warm shoji-screen "
               "morning light",
        scene_a="the same adult woman sitting at a low wooden table with a handmade bento box, "
                "chopsticks raised, laughing at something off frame, warm morning sun through paper "
                "screens, steam rising",
        scene_b="close-up of the same adult woman laughing toward the viewer, eyes squeezed shut, "
                "flour on her cheek, hair escaping its bun, warm backlight and drifting steam",
        thumb="head and shoulders of the same cheerful adult woman, face centred and large, bright "
              "open smile, apron strap visible, plain warm cream background",
    ),
    "ink_moon": dict(
        mood="ink wash, understated, elegant",
        room="an adult woman drawn in delicate sumi-e ink wash, a pale grey robe with a soft sash "
             "fully covering her, one hand holding the sleeve closed at her collar, hair in a low "
             "loose knot, calm sidelong glance, sparse ink strokes and faint watercolour washes, "
             "plain rice paper background",
        header="the same adult woman in delicate sumi-e ink wash, pale robe and soft sash, holding a "
               "paper umbrella low behind her shoulder, walking past a faint ink mountain ridge, "
               "serene sidelong glance at the viewer, dry brush texture and empty rice paper",
        scene_a="the same adult woman standing on a stone bridge in ink wash, one sleeve lifted to "
                "her chin, mist and a faint mountain ridge behind her, a pale moon, sparse dry "
                "brush strokes on rice paper",
        scene_b="close-up of the same adult woman in ink wash, a faint half smile and lowered eyes, "
                "one loose strand of hair, minimal brush strokes, a single soft grey wash",
        thumb="head and shoulders of the same adult woman in delicate ink wash, whole head inside "
              "the frame with the full hairstyle visible, serene expression, plain off-white rice "
              "paper background",
    ),
    "paper_minimal": dict(
        mood="flat paper-cut, minimal lines, cool",
        room="an adult woman in flat paper-cut illustration style, thin clean outlines, modern "
             "slate-grey blazer over a simple top, arms folded, short bob hair, a cool aloof "
             "sideways look, one muted teal accent, plain off-white background",
        header="the same adult woman in flat paper-cut illustration style, thin crisp outlines, "
               "slate blazer and simple high-neck top, standing with arms loosely crossed and a "
               "composedly indifferent expression, one muted teal accent panel behind her, layered "
               "paper shadows, plain off-white background",
        scene_a="close-up portrait of one adult woman with a clear simple friendly face and calm "
                "eyes, flat paper-cut illustration style, clean thin outlines, slate blazer, she "
                "stands beside a small potted plant, one muted teal accent, plain off-white "
                "background, head and shoulders, arms loosely folded",
        scene_b="close-up portrait of one adult woman with a clear simple friendly face and large "
                "expressive eyes, flat paper-cut illustration style, clean thin outlines, half-lidded "
                "cool gaze to one side, one muted teal accent, plain off-white background, head and "
                "shoulders",
        thumb="head and shoulders of the same adult woman in flat paper-cut style, face centred and "
              "large, neutral composed expression, thin outlines, plain off-white background",
    ),
}


KIND_KEY = {"portrait_room": "room", "portrait_header": "header", "scene_a": "scene_a",
            "scene_b": "scene_b", "thumb": "thumb"}


def char_text(theme_id, kind):
    return CHARACTERS[theme_id][KIND_KEY[kind]]


def prompt_for(kind, theme_id):
    text = char_text(theme_id, kind)
    return v2.STYLE + ", " + BOUNDARY_POSITIVE + ", " + KINDS[kind]["composition"] + ", " + text


def negative_for(kind):
    extra = {"portrait_room": ROOM_NEGATIVE, "thumb": THUMB_NEGATIVE}.get(kind, "")
    return v2.NEGATIVE + ", " + BOUNDARY + (", " + extra if extra else "")


def theme_specs():
    # neon_cyber is archived (removed from JarvisThemes.all) and stays out of this set.
    return [s for s in v2.THEMES if s["id"] in CHARACTERS]


def seed_for(spec, index, kind, offset=0):
    return spec["seed"] + index + 700 + 40 * KIND_ORDER.index(kind) + offset


def generate(kind, spec, index, seed_offset=0):
    cfg = KINDS[kind]
    gen_size = cfg["gen"]
    seed = seed_for(spec, index, kind, seed_offset)
    prompt = prompt_for(kind, spec["id"])
    payload = {"prompt": prompt, "negative_prompt": negative_for(kind), "seed": seed,
               "steps": v2.STEPS, "cfg_scale": v2.CFG, "sampler_name": v2.SAMPLER,
               "scheduler": v2.SCHEDULER, "width": gen_size[0], "height": gen_size[1],
               "batch_size": 1, "n_iter": 1, "send_images": True, "save_images": False,
               "override_settings": {"sd_model_checkpoint": v2.MODEL}}
    started = time.time()
    result = v2.request("/sdapi/v1/txt2img", payload)
    elapsed = time.time() - started
    if elapsed > 480:
        raise SystemExit("single image took %.0fs (>480s budget): %s/%s" % (elapsed, spec["id"], kind))
    source = ART / spec["id"] / (kind + ".png")
    source.parent.mkdir(parents=True, exist_ok=True)
    source.write_bytes(base64.b64decode(result["images"][0].split(",")[-1]))
    info = {"prompt": prompt, "negative_prompt": negative_for(kind), "seed": seed,
            "gen_size": list(gen_size), "seconds": round(elapsed, 1),
            "request": {k: payload[k] for k in ("steps", "cfg_scale", "sampler_name", "scheduler")},
            "content_rating": CONTENT_RATING, "character": char_text(spec["id"], kind)}
    # Sidecar: a later --offline run rebuilds the manifest from this, so the recorded seed keeps
    # pointing at the PNG actually on disk instead of the theme's base seed.
    source.with_suffix(".json").write_text(json.dumps(info, ensure_ascii=False, indent=2),
                                           encoding="utf-8")
    return source, info, elapsed


def headroom(image, band_ratio=0.33, overlap=56):
    """Guarantee the empty top third the UI needs by extending the background upward.

    SDXL obeys 'leave the top third empty' for roughly one seed in five, so the layout is made
    deterministic instead: the artwork is scaled down slightly and the freed band is filled with a
    gradient built from the artwork's own top row, feathered into the seam. Only background is
    touched; no character detail is invented or removed.
    """
    width, height = image.size
    band = int(height * band_ratio)
    keep = height - band + overlap
    body = image.resize((width, keep), Image.Resampling.LANCZOS)
    out = Image.new("RGB", (width, height))
    rows = band + overlap
    # Stretching real background pixels smears any bloom into the band, so the band is instead a
    # smooth atmospheric gradient derived from the artwork's own top row: it continues the sky
    # seamlessly at the seam and stays verifiably quiet for text.
    row0 = body.crop((0, 0, width, 1)).resize((width, rows), Image.Resampling.LANCZOS)
    dark_row = row0.point(lambda v: int(v * 0.62))
    ramp = Image.linear_gradient("L").resize((width, rows))
    out.paste(Image.composite(row0, dark_row, ramp).filter(ImageFilter.GaussianBlur(6)), (0, 0))
    top = height - keep
    mask = Image.new("L", (width, overlap))
    for y in range(overlap):
        mask.paste(int(255 * y / max(1, overlap - 1)), (0, y, width, y + 1))
    out.paste(body.crop((0, 0, width, overlap)), (0, top), mask)
    out.paste(body.crop((0, overlap, width, keep)), (0, top + overlap))
    return out


def icon_fit(image, margin=0.05):
    """Shrink into a margin so a 56-72dp crop never clips the head, on a blurred own-image backdrop."""
    size = image.size
    inner = max(1, int(size[0] * (1 - 2 * margin)))
    backdrop = image.filter(ImageFilter.GaussianBlur(max(8, size[0] // 12)))
    out = backdrop.copy()
    out.paste(image.resize((inner, inner), Image.Resampling.LANCZOS),
              ((size[0] - inner) // 2, (size[1] - inner) // 2))
    return out


def subject_centroid(image):
    """Edge-energy centroid of the subject, measured above the bottom clutter (railing, floor).

    v2.right_bias() averages whole-frame edges, which a long railing drags to 0.5 even when the
    character sits at 0.30, so the header uses this narrower measurement instead.
    """
    edges = image.convert("L").filter(ImageFilter.FIND_EDGES)
    width, height = edges.size
    band = edges.crop((0, int(height * 0.08), width, int(height * 0.78)))
    step = max(1, band.width // 256)
    columns = [sum(band.crop((x, 0, x + step, band.height)).convert("L")
                   .resize((1, 1)).getdata()) for x in range(0, band.width, step)]
    total = sum(columns) or 1
    centroid = sum(i * c for i, c in enumerate(columns)) / float(total * max(1, len(columns) - 1))
    return round(centroid, 3)


def place_right(image, target=0.76, overlap=40):
    """Slide the artwork so the character lands in the right third the settings header needs.

    SDXL treats 'subject on the right third' as a mood, not an instruction (five seeds landed
    between 0.30 and 0.52), and mirroring cannot move a centred subject. So the frame is shifted
    and the vacated left side is rebuilt from the artwork's own left column, which continues floor
    and railing lines naturally, then held slightly dark so the title text stays legible.
    """
    centroid = subject_centroid(image)
    if centroid >= target - 0.06:
        return image, centroid, 0
    width, height = image.size
    shift = int(round((target - centroid) * width))
    out = Image.new("RGB", (width, height))
    fill = image.crop((0, 0, 1, height)).resize((shift + overlap, height),
                                                Image.Resampling.LANCZOS)
    out.paste(fill, (0, 0))
    edge = image.crop((0, 0, overlap, height))
    mask = Image.new("L", (overlap, height))
    for x in range(overlap):
        mask.paste(int(255 * x / max(1, overlap - 1)), (x, 0, x + 1, height))
    out.paste(edge, (shift, 0), mask)
    rest = image.crop((overlap, 0, width, height))
    out.paste(rest, (shift + overlap, 0))
    ramp = Image.linear_gradient("L").rotate(270).resize((width, height))
    return Image.composite(out, out.point(lambda v: int(v * 0.72)), ramp), centroid, shift


def _stats(image):
    stat = ImageStat.Stat(image)
    return {"mean": round(stat.mean[0], 1), "stddev": round(stat.stddev[0], 1)}


def quiet_zone(image, kind):
    """Luma stats for the region the UI writes text on, plus the wider region it must stay out of.

    text_band is the area that actually carries a label (top 22% of a vertical frame, left 45% of
    the header); region is the whole reserved area. Both are recorded so the verdict is checkable.
    """
    gray = image.convert("L")
    if kind == "portrait_header":
        region = gray.crop((0, 0, int(gray.width * 0.45), gray.height))
        text = gray.crop((0, 0, int(gray.width * 0.45), int(gray.height * 0.75)))
    elif kind == "thumb":
        region = text = gray.crop((0, 0, gray.width, int(gray.height * 0.18)))
    else:
        region = gray.crop((0, 0, gray.width, gray.height // 3))
        text = gray.crop((0, 0, gray.width, int(gray.height * 0.22)))
    return {"region": _stats(region), "text_band": _stats(text)}


QUIET_MAX_SD = 16.0


def build(spec, index, kinds, force, offline=False, seed_offset=0):
    images = {}
    for kind in KIND_ORDER:
        if kinds and kind not in kinds:
            continue
        cfg = KINDS[kind]
        source = ART / spec["id"] / (kind + ".png")
        if offline and not source.exists():
            raise SystemExit("--offline needs an existing source PNG: %s" % source)
        if force and offline:
            raise SystemExit("--force and --offline contradict each other")
        if force or not source.exists():
            print("  generating %s/%s" % (spec["id"], kind), flush=True)
            source, info, elapsed = generate(kind, spec, index, seed_offset)
            print("    seed %d in %.0fs" % (info["seed"], elapsed), flush=True)
        else:
            info = None
        final = cfg["final"]
        with Image.open(source) as opened:
            art = opened.convert("RGB")
            art = v2.tint(art, spec["palette"], 0.08, final)
            native = quiet_zone(art, kind)
            layout = "native"
            # A close-up (scene_b) has nothing but the face up there: extending it upward would
            # invent a giant blurred head, so only the wider vertical framings get the extension.
            if cfg["vertical"] and kind != "scene_b" and native["region"]["stddev"] > QUIET_MAX_SD:
                art = headroom(art)
                layout = "headroom-extend"
        centroid, shift = None, 0
        if kind == "portrait_header":
            # Measured centroid reads a bright wall as saliency, so a header the visual pass found
            # on the wrong side is mirrored by explicit per-theme override rather than a metric.
            if HEADER_FIX.get(spec["id"]) == "mirror":
                art = art.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
                centroid, shift = subject_centroid(art), -1
            else:
                art, centroid, shift = place_right(art, spec.get("header_target", 0.76))
        if kind == "thumb":
            art = icon_fit(art)
        path = RES / ("theme_%s_%s.webp" % (spec["id"], kind))
        quality, size = v2.save_budgeted(art, path, cfg["quality"], cfg["budget"])
        if info is None:
            sidecar = source.with_suffix(".json")
            if sidecar.exists():
                info = json.loads(sidecar.read_text(encoding="utf-8"))
            else:
                info = {"prompt": prompt_for(kind, spec["id"]),
                        "negative_prompt": negative_for(kind),
                        "seed": seed_for(spec, index, kind), "gen_size": list(cfg["gen"]),
                        "content_rating": CONTENT_RATING, "character": char_text(spec["id"], kind)}
            info["note"] = "reused existing source %s" % source.name
        entry = dict(info)
        entry.update({"size": list(final), "source": str(source.relative_to(ROOT)),
                      "output": str(path.relative_to(ROOT)), "bytes": size,
                      "webp_quality": quality, "ui_budget": UI_BUDGET[kind],
                      "style_generation": "v2" if entry["prompt"] == prompt_for(kind, spec["id"])
                      else "v1"})
        with Image.open(path) as saved:
            real = saved.size
            zone = quiet_zone(saved.convert("RGB"), kind)
            # Only the kinds that carry an overlaid label need a quiet band; a close-up or an icon
            # has no text on top, so the measurement is reported without a pass/fail verdict.
            needs_quiet = kind in ("portrait_room", "portrait_header", "scene_a")
            entry["qa"] = dict(zone, width=real[0], height=real[1], layout=layout,
                               quiet_max_stddev=QUIET_MAX_SD,
                               text_band_ok=(zone["text_band"]["stddev"] <= QUIET_MAX_SD
                                             if needs_quiet else None),
                               text_band_verdict=("measured" if needs_quiet
                                                  else "not required: no overlaid text"),
                               boundary="pending visual review")
        if centroid is not None:
            entry["subject_centroid_before"] = centroid
            entry["subject_shift_px"] = shift
            entry["subject_centroid_after"] = subject_centroid(art)
        images[kind] = entry
        print("  %s %dx%d %dKB q%d %s" % (path.name, real[0], real[1], round(size / 1024), quality,
                                          layout), flush=True)
    return images


def main():
    parser = argparse.ArgumentParser(description="Generate the per-theme character illustrations.")
    parser.add_argument("--theme", action="append", metavar="ID")
    parser.add_argument("--kind", choices=KIND_ORDER, action="append")
    parser.add_argument("--force", action="store_true")
    parser.add_argument("--offline", action="store_true")
    parser.add_argument("--seed-offset", type=int, default=0, metavar="N")
    parser.add_argument("--list", action="store_true")
    args = parser.parse_args()

    if args.list:
        for spec in theme_specs():
            print("%-14s %s" % (spec["id"], CHARACTERS[spec["id"]]["mood"]))
        return
    unknown = [t for t in (args.theme or []) if t not in CHARACTERS]
    if unknown:
        raise SystemExit("unknown theme id(s): " + ", ".join(unknown))

    RES.mkdir(parents=True, exist_ok=True)
    if not args.offline:
        v2.wait_for_api()
        v2.wait_idle()
        print("A1111 ready and idle, model %s" % v2.MODEL, flush=True)

    manifest = v2.load_manifest() or {"provider": "local A1111", "api": v2.API}
    manifest["portrait_generator"] = "tools/generate-portraits-v2.py"
    manifest["portrait_note"] = (
        "Character illustration set: portrait_room / scene_a / scene_b (768x1024, conversation "
        "empty-state rotation), portrait_header (1536x640, settings header), thumb (512x512, small "
        "card icon). Every character is authored as an adult woman and the negative prompt carries "
        "an explicit no-nudity / no-minor block; content_rating 'suggestive-non-explicit' is the "
        "audited ceiling for this set.")
    manifest["ui_budget"] = UI_BUDGET
    by_id = {t.get("id"): t for t in manifest.get("themes", [])}
    for index, spec in enumerate(theme_specs()):
        if args.theme and spec["id"] not in args.theme:
            continue
        print("[%s] %s" % (spec["id"], spec["label"]), flush=True)
        images = build(spec, index, args.kind, args.force, args.offline, args.seed_offset)
        merged = dict(by_id.get(spec["id"], {}))
        merged["images"] = dict(merged.get("images", {}), **images)
        merged["content_rating"] = CONTENT_RATING
        by_id[spec["id"]] = merged
    order = [t.get("id") for t in manifest.get("themes", [])]
    order += [i for i in by_id if i not in order]
    manifest["themes"] = [by_id[i] for i in order if i in by_id]
    manifest["generated_at"] = time.strftime("%Y-%m-%dT%H:%M:%S")
    out = ART / "manifest.json"
    out.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print("manifest written: %s" % out, flush=True)


if __name__ == "__main__":
    main()
