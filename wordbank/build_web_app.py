#!/usr/bin/env python3
"""把 Web/src 模板 + Android 完整词库打包为单文件 Web/index.html（离线双击即用）。

用法：python -I WorkBuddy/wordbank/build_web_app.py
约束：产物单文件、无框架/CDN；词库来自 Study/app/src/main/assets/wordbank_*.json。
"""
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
ASSETS = ROOT / "Study" / "app" / "src" / "main" / "assets"
SRC = ROOT / "Web" / "src"
OUT = ROOT / "Web" / "index.html"

EXPECTED = {"CET-4": 4544, "CET-6": 3991}


def load_bank(name: str):
    path = ASSETS / name
    data = json.loads(path.read_text(encoding="utf-8"))
    library = data[0]["library"]
    if len(data) != EXPECTED[library]:
        raise SystemExit(f"{library} 词数不符：{len(data)} != {EXPECTED[library]}")
    required = {"word", "phonetic", "pos", "meaning", "example", "exampleZh", "phrase", "phraseZh", "library"}
    for item in data:
        missing = required - set(item)
        if missing:
            raise SystemExit(f"{library} 字段缺失 {missing}: {item.get('word')}")
    return data


def main() -> int:
    cet4 = load_bank("wordbank_cet4.json")
    cet6 = load_bank("wordbank_cet6.json")
    bank = {"CET-4": cet4, "CET-6": cet6}
    bank_js = json.dumps(bank, ensure_ascii=False, separators=(",", ":"))

    html = (SRC / "app.html").read_text(encoding="utf-8")
    css = (SRC / "styles.css").read_text(encoding="utf-8")
    app = (SRC / "app.js").read_text(encoding="utf-8")
    script = "const WORDBANK = " + bank_js + ";\n" + app

    if "/*__STYLES__*/" not in html or "/*__SCRIPT__*/" not in html:
        raise SystemExit("模板缺少占位符 /*__STYLES__*/ 或 /*__SCRIPT__*/")
    html = html.replace("/*__STYLES__*/", css)
    if "/*__SCRIPT__*/" not in html:
        raise SystemExit("模板缺少脚本占位符")
    html = html.replace("/*__SCRIPT__*/", script)

    OUT.write_text(html, encoding="utf-8")
    words = len(cet4) + len(cet6)
    print(f"Web/index.html 已生成：{OUT.stat().st_size} bytes，词库 {words} 词（CET-4 {len(cet4)} / CET-6 {len(cet6)}）")
    return 0


if __name__ == "__main__":
    sys.exit(main())
