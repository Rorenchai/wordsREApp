#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把参考 Web 应用的 CET-4 / CET-6 词库转换成 Android assets 用的纯 JSON。

安全约定（不可信数据隔离）：
- 参考应用代码与数据位于 _ref_zips/ 目录，本项目不运行、不 import 其中任何代码；
- 本脚本只把 .js 文件当作**纯文本数据**读取：用正则取出 `window.WordBankData.cetX = [...]`
  里的 JSON 数组后交给 json.loads 解析，绝不 eval / 执行任何 JS；
- 建议用隔离模式运行：python -I WorkBuddy/wordbank/convert_ref_wordbank.py

输出：
  Study/app/src/main/assets/wordbank_cet4.json
  Study/app/src/main/assets/wordbank_cet6.json
每个文件是纯 JSON 数组，元素字段固定为：
  word, phonetic, pos, meaning, example, exampleZh, phrase, phraseZh, library
"""

import json
import os
import re
import sys

# 参考数据与输出位置（相对工作区根目录）
WORKSPACE = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
REF_DATA_DIR = os.path.join(
    WORKSPACE, "_ref_zips", "english-word-cards", "ref-app", "app", "data"
)
OUT_DIR = os.path.join(WORKSPACE, "Study", "app", "src", "main", "assets")

FIELDS = [
    "word",
    "phonetic",
    "pos",
    "meaning",
    "example",
    "exampleZh",
    "phrase",
    "phraseZh",
    "library",
]

# 预期词数（与参考应用 CREDITS.md 声明一致）
EXPECTED = {"cet4": 4544, "cet6": 3991}
EXPECTED_LIBRARY = {"cet4": "CET-4", "cet6": "CET-6"}


def extract_array_text(js_text, key):
    """从 `window.WordBankData.<key> = [ ... ];` 中取出数组文本。"""
    pattern = re.compile(
        r"window\.WordBankData\." + re.escape(key) + r"\s*=\s*(\[.*\])\s*;?\s*$",
        re.DOTALL,
    )
    match = pattern.search(js_text)
    if not match:
        raise ValueError("未能在 %s.js 中找到 window.WordBankData.%s 数组" % (key, key))
    return match.group(1)


def load_words(key):
    path = os.path.join(REF_DATA_DIR, key + ".js")
    if not os.path.exists(path):
        raise FileNotFoundError("参考词库文件不存在: " + path)
    with open(path, "r", encoding="utf-8") as handle:
        js_text = handle.read()

    array_text = extract_array_text(js_text, key)
    # 只做 JSON 解析（数据），不执行任何脚本
    raw = json.loads(array_text)
    if not isinstance(raw, list):
        raise ValueError("%s 解析结果不是数组" % key)

    words = []
    for index, item in enumerate(raw):
        if not isinstance(item, dict):
            raise ValueError("%s 第 %d 条不是对象" % (key, index))
        missing = [field for field in FIELDS if field not in item]
        if missing:
            raise ValueError("%s 第 %d 条缺少字段: %s" % (key, index, missing))
        word = str(item["word"]).strip()
        if not word:
            raise ValueError("%s 第 %d 条 word 为空" % (key, index))
        entry = {field: str(item[field]) for field in FIELDS}
        if entry["library"] != EXPECTED_LIBRARY[key]:
            raise ValueError(
                "%s 第 %d 条 library 异常: %s" % (key, index, entry["library"])
            )
        words.append(entry)

    expected_count = EXPECTED[key]
    if len(words) != expected_count:
        raise ValueError(
            "%s 词数不符：实际 %d，预期 %d" % (key, len(words), expected_count)
        )
    spellings = [entry["word"] for entry in words]
    if len(set(spellings)) != len(spellings):
        raise ValueError("%s 存在重复单词" % key)
    return words


def write_json(words, filename):
    os.makedirs(OUT_DIR, exist_ok=True)
    path = os.path.join(OUT_DIR, filename)
    # 保持 UTF-8、不转义中文；紧凑输出以减小 assets 体积
    text = json.dumps(words, ensure_ascii=False, separators=(",", ":"))
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        handle.write(text)
    return path, len(text.encode("utf-8"))


def verify_output(path, expected_library, expected_count):
    """回读校验：条数、字段、library、单词唯一、UTF-8 无 BOM。"""
    with open(path, "rb") as handle:
        raw_bytes = handle.read()
    if raw_bytes.startswith(b"\xef\xbb\xbf"):
        raise ValueError("输出文件带 BOM: " + path)
    data = json.loads(raw_bytes.decode("utf-8"))
    if len(data) != expected_count:
        raise ValueError("回读条数不符: %s" % path)
    spellings = set()
    for item in data:
        for field in FIELDS:
            if field not in item:
                raise ValueError("回读缺少字段 %s: %s" % (field, path))
        if item["library"] != expected_library:
            raise ValueError("回读 library 异常: %s" % path)
        if item["word"] in spellings:
            raise ValueError("回读发现重复单词: %s" % path)
        spellings.add(item["word"])
    return len(data)


def main():
    print("参考数据目录:", REF_DATA_DIR)
    print("输出目录:", OUT_DIR)
    summary = []
    for key in ("cet4", "cet6"):
        words = load_words(key)
        filename = "wordbank_%s.json" % key
        path, byte_size = write_json(words, filename)
        verified = verify_output(path, EXPECTED_LIBRARY[key], EXPECTED[key])
        summary.append((filename, verified, byte_size))
        print(
            "OK  %-22s 词数=%-5d 大小=%.2f MB library=%s"
            % (filename, verified, byte_size / 1024.0 / 1024.0, EXPECTED_LIBRARY[key])
        )
    print("全部完成：", ", ".join("%s(%d)" % (name, count) for name, count, _ in summary))
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as error:  # 明确失败原因，便于排查
        print("ERROR:", error, file=sys.stderr)
        sys.exit(1)
