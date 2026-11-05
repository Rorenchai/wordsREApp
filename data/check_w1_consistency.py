# -*- coding: utf-8 -*-
"""静态核对: Web/index.html 内嵌 20 词 vs Study assets/words.json 一致性 (W-1 交接前检查)"""
import json
import re
from collections import Counter

ROOT = r"E:\wordsREApp"

android = json.load(open(ROOT + r"\Study\app\src\main\assets\words.json", encoding="utf-8"))["words"]

html = open(ROOT + r"\Web\index.html", encoding="utf-8").read()
m = re.search(r"const WORDS = \[(.*?)\n  \];", html, re.S)
body = m.group(1)
pat = re.compile(r"spelling:\s*'([^']*)',\s*meaning:\s*'([^']*)',\s*level:\s*'([^']*)'")
web = [{"spelling": a, "meaning": b, "level": c} for a, b, c in pat.findall(body)]

print("Android %d 词, Web 提取 %d 词" % (len(android), len(web)))
print("逐条一致(顺序+内容):", android == web)
if android != web:
    for i in range(max(len(android), len(web))):
        a = android[i] if i < len(android) else None
        w = web[i] if i < len(web) else None
        if a != w:
            print("  差异@%d:" % i, a, "vs", w)
print("Web level 分布:", dict(Counter(w["level"] for w in web)))
print("拼写互不重复:", len({w["spelling"] for w in web}) == len(web))

# localStorage 键名与按钮文案对齐检查
print("键名 wordsre_mastered:", "KEY_MASTERED = 'wordsre_mastered'" in html)
print("键名 wordsre_viewed:", "KEY_VIEWED = 'wordsre_viewed'" in html)
for btn in ["下一个", "已掌握", "重置", "取消掌握"]:
    print("按钮/文案「%s」:" % btn, btn in html)
