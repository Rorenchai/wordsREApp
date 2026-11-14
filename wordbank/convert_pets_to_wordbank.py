#!/usr/bin/env python3
"""把 WorkBuddy 整理的 PETS 词库转换为 Android assets 词库格式（PETS-B 默认词库）。

源文件: WorkBuddy/wordbank/words_PETS_2026-11-05.json（结构 {"words":[{spelling,meaning,level}]}）
输出:   Study/app/src/main/assets/wordbank_petsb.json（数组，字段与 wordbank_cet*.json 一致）
"""
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
SRC = ROOT / "WorkBuddy" / "wordbank" / "words_PETS_2026-11-05.json"
DST = ROOT / "Study" / "app" / "src" / "main" / "assets" / "wordbank_petsb.json"
LIBRARY = "PETS-B"


def main() -> int:
    source = json.loads(SRC.read_text(encoding="utf-8"))
    words = source.get("words", []) if isinstance(source, dict) else source
    result = []
    seen = set()
    for entry in words:
        spelling = (entry.get("spelling") or "").strip()
        meaning = (entry.get("meaning") or "").strip()
        if not spelling or not meaning:
            continue
        key = spelling.lower()
        if key in seen:
            continue
        seen.add(key)
        result.append({
            "word": spelling,
            "phonetic": "",
            "pos": "",
            "meaning": meaning,
            "example": "",
            "exampleZh": "",
            "phrase": "",
            "phraseZh": "",
            "library": LIBRARY,
        })
    DST.write_text(json.dumps(result, ensure_ascii=False, indent=None, separators=(",", ":")), encoding="utf-8")
    print(f"已生成 {DST.name}: {len(result)} 词（library={LIBRARY}）")
    return 0


if __name__ == "__main__":
    sys.exit(main())
