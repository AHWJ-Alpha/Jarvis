# Lists the v2 theme assets with real WebP dimensions and size in KB: 18 base assets
# (banner/avatar/texture) plus 25 character illustrations (5 kinds x 5 live themes).
# Dimensions are decoded by Pillow via the WebUI runtime (the system Python has no Pillow);
# the PowerShell bit-shift path was unreliable, so the decoder lives in Python.
$py = 'D:\sd-webui-aki(1)\sd-webui-aki-v4.11.1-cu128\python\python.exe'
$code = @'
from pathlib import Path
from PIL import Image
root = Path(r"<ROOT>")
ids = ["deep_night", "sakura_night", "neon_cyber", "morning_bento", "ink_moon", "paper_minimal"]
budget = {"banner": 220.0, "avatar": 80.0, "texture": 120.0,
          "portrait_room": 260.0, "portrait_header": 220.0, "scene_a": 260.0,
          "scene_b": 260.0, "thumb": 80.0}
# neon_cyber is archived and has no character illustrations.
illustrated = [tid for tid in ids if tid != "neon_cyber"]
ok = True
kinds = [(tid, "banner") for tid in ids] + [(tid, "avatar") for tid in ids] + \
        [(tid, "texture") for tid in ids] + \
        [(tid, k) for tid in illustrated
         for k in ("portrait_room", "portrait_header", "scene_a", "scene_b", "thumb")]
for tid, kind in kinds:
    if True:
        p = root / ("theme_%s_%s.webp" % (tid, kind))
        if not p.exists():
            print("%-32s MISSING" % p.name); ok = False; continue
        with Image.open(p) as im:
            w, h = im.size
        kb = p.stat().st_size / 1024.0
        flag = "ok" if kb <= budget[kind] else "OVER"
        if kb > budget[kind]: ok = False
        print("%-32s %5d x %-5d %7.1f KB  %s (<=%.0fKB)" % (p.name, w, h, kb, flag, budget[kind]))
print("BUDGET_ALL_OK" if ok else "BUDGET_VIOLATION")
'@
$code = $code.Replace('<ROOT>', (Resolve-Path (Join-Path $PSScriptRoot '..\..\app\src\main\res\drawable-nodpi')).Path)
$code | & $py -
$seam = @'
from pathlib import Path
from PIL import Image, ImageChops, ImageStat
root = Path(r"<ROOT>")
print("texture metrics (delivered WebP): contrast = stddev/255, black = share of L<64")
print("%-14s %-6s %-6s %-6s %-8s %-6s %-8s %s" % (
    "theme", "KB", "mean", "stddev", "contrast%", "black%", "seam_lr", "seam_tb/baseline"))
for tid in ["deep_night", "sakura_night", "neon_cyber", "morning_bento", "ink_moon", "paper_minimal"]:
    p = root / ("theme_%s_texture.webp" % tid)
    im = Image.open(p).convert("RGB")
    w, h = im.size
    g = im.convert("L")
    stat = ImageStat.Stat(g)
    hist = g.histogram()
    black = 100.0 * sum(hist[:64]) / float(w * h)
    lr = ImageStat.Stat(ImageChops.difference(im.crop((0, 0, 1, h)),
                                              im.crop((w - 1, 0, w, h))).convert("L")).mean[0]
    tb = ImageStat.Stat(ImageChops.difference(im.crop((0, 0, w, 1)),
                                              im.crop((0, h - 1, w, h))).convert("L")).mean[0]
    ref = ImageStat.Stat(ImageChops.difference(im.crop((0, 0, 1, h)),
                                               im.crop((w // 2, 0, w // 2 + 1, h))).convert("L")).mean[0]
    print("%-14s %-6.1f %-6.1f %-6.2f %-8.2f %-6.2f %-8.1f %.1f / %.1f" % (
        tid, p.stat().st_size / 1024.0, stat.mean[0], stat.stddev[0],
        100.0 * stat.stddev[0] / 255.0, black, lr, tb, ref))
    contrast = 100.0 * stat.stddev[0] / 255.0
    if tid in ("morning_bento", "ink_moon", "paper_minimal"):
        # Light themes sit under body text: near-white, 3-6% contrast, no dark blocks.
        verdict = "OK" if black < 2.0 and 3.0 <= contrast <= 6.0 else "OUT OF BAND"
    else:
        verdict = "dark theme (contrast band not applicable)"
    print("               seam axis max %.1f / baseline %.1f -> %s | %s" % (
        max(lr, tb), ref, verdict, "vertical seam risk" if tb > max(3.0, ref) else "no seam risk"))
'@
$seam = $seam.Replace('<ROOT>', (Resolve-Path (Join-Path $PSScriptRoot '..\..\app\src\main\res\drawable-nodpi')).Path)
$seam | & $py -