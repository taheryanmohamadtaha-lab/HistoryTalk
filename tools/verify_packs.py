#!/usr/bin/env python3
"""اعتبارسنجی ساختاری پک‌های داده‌ی شخصیت‌ها (سریع؛ پیش از بیلد در CI اجرا می‌شود).
بررسی‌های عمیق‌تر (بازیابی، فیلتر ایمنی) در تست واحد FigurePacksTest انجام می‌شود.
اجرا: python3 tools/verify_packs.py [پوشه]   — پیش‌فرض app/src/main/assets/figures
"""
import glob, json, os, re, sys

REQ = ["id", "name", "lifespan", "era", "eraGroup", "civilization", "role", "roleGroup", "nationality", "summary"]
ERA = {"ANCIENT", "MEDIEVAL", "EARLY_MODERN", "MODERN"}
ROLE = {"RULER", "THINKER", "LEADER"}
CATS = {"life", "war", "politics", "family", "beliefs", "science", "legacy", "crimes", "era", "general"}
QTYPES = {"DOCUMENTED", "ATTRIBUTED", "RECREATED"}


def norm(s):
    s = s.replace("ي", "ی").replace("ك", "ک").replace("‌", " ")
    return re.sub(r"\s+", " ", re.sub(r"[^\w\s]", " ", s)).strip().lower()


def main(d):
    errors, ids = 0, set()

    def err(msg):
        nonlocal errors
        errors += 1
        print("خطا:", msg)

    files = sorted(glob.glob(os.path.join(d, "*.json")))
    for p in files:
        try:
            f = json.load(open(p, encoding="utf-8"))
        except Exception as e:  # noqa: BLE001
            err(f"{p}: JSON نامعتبر ({e})")
            continue
        pid = f.get("id", "?")
        for k in REQ:
            if not f.get(k):
                err(f"{pid}: فیلد الزامی «{k}» خالی است")
        if pid in ids:
            err(f"{pid}: id تکراری")
        ids.add(pid)
        if pid != os.path.basename(p)[:-5]:
            err(f"{pid}: نام فایل با id یکی نیست")
        if not re.fullmatch(r"[a-z0-9_]{2,40}", pid):
            err(f"{pid}: id نامعتبر")
        if f.get("eraGroup") not in ERA:
            err(f"{pid}: eraGroup نامعتبر")
        if f.get("roleGroup") not in ROLE:
            err(f"{pid}: roleGroup نامعتبر")
        if f.get("voice", "persona") not in {"persona", "historian"}:
            err(f"{pid}: voice نامعتبر")
        if len(f.get("knowledge", [])) < 8:
            err(f"{pid}: دانش کمتر از ۸ مورد")
        for i, k in enumerate(f.get("knowledge", [])):
            if k.get("category") not in CATS:
                err(f"{pid}#{i}: دسته‌ی نامعتبر {k.get('category')}")
            if not k.get("text") or not k.get("keywords"):
                err(f"{pid}#{i}: متن/کلیدواژه خالی")
        if len(f.get("timeline", [])) < 4:
            err(f"{pid}: خط زمانی کمتر از ۴ مورد")
        for q in f.get("quotes", []):
            if q.get("type") not in QTYPES:
                err(f"{pid}: نوع نقل‌قول نامعتبر")
            if q.get("type") == "DOCUMENTED" and not q.get("source"):
                err(f"{pid}: نقل مستند بدون منبع")
        for c in f.get("challenges", []):
            if not any(norm(k) in norm(c.get("explanation", "")) for k in c.get("keywords", [])):
                err(f"{pid}: کلیدواژه‌ی چالش «{c.get('question')}» در توضیح نیست")
        if len(f.get("suggestions", [])) < 4:
            err(f"{pid}: پیشنهاد سؤال کمتر از ۴")
    if len(ids) < 15:
        err(f"تعداد شخصیت‌ها {len(ids)} است (حداقل ۱۵)")
    print(f"{len(ids)} شخصیت بررسی شد، {errors} خطا")
    return errors


if __name__ == "__main__":
    sys.exit(1 if main(sys.argv[1] if len(sys.argv) > 1 else "app/src/main/assets/figures") else 0)
