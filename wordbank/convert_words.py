# -*- coding: utf-8 -*-
"""把有道词书 JSON 转换为项目 words.json 结构的词库草稿。

源数据: WorkBuddy/data/extracted/*.json (kajweb/dict 开源仓库, 每行一个词条)
输出:   WorkBuddy/wordbank/words_<LEVEL>_<日期>.json, 结构 {"words": [{"spelling","meaning","level"}]}
"""
import json
import re
import datetime
import os

SRC = r"E:\wordsREApp\WorkBuddy\data\extracted"
DST = r"E:\wordsREApp\WorkBuddy\wordbank"
TODAY = "2026-11-05"


def load_book(name):
    """读取词书, 返回 [(wordRank, headWord, meaning)] 列表, meaning 已格式化为 'pos. 释义；释义' 风格"""
    entries = []
    with open(os.path.join(SRC, name + ".json"), encoding="utf-8") as fh:
        for line in fh:
            line = line.strip()
            if not line:
                continue
            d = json.loads(line)
            rank = d.get("wordRank", 0)
            word = d.get("headWord", "").strip()
            trans = (d.get("content", {}).get("word", {})
                      .get("content", {}).get("trans", []))
            entries.append((rank, word, format_meaning(trans)))
    return entries


def clean_cn(text):
    """清理中文释义文本: 去多余空格, 逗号分隔改分号"""
    text = text.strip()
    text = re.sub(r"\s+", " ", text)
    text = re.sub(r"\s*，\s*", "；", text)
    text = re.sub(r"\s*,\s*", "；", text)
    text = text.replace("； ", "；")
    return text


def format_meaning(trans):
    """把 trans 数组转成与现有 words.json 一致的 meaning 字符串。

    现有风格示例: 'adj. 美丽的；漂亮的' / 'v. 借；借用'
    规则: 每个词性一条 'pos. 释义', 最多取 3 条, 用 '；' 连接;
          过滤含 '人名' 的义项(词书噪声, 如 'n. (Borrow)人名；(英)博罗')。
    """
    parts = []
    for t in trans[:4]:
        cn = clean_cn(t.get("tranCn") or "")
        if not cn or "人名" in cn:
            continue
        pos = (t.get("pos") or "").strip()
        if pos:
            pos = pos if pos.endswith(".") else pos + "."
            parts.append("%s %s" % (pos, cn))
        else:
            parts.append(cn)
    return "；".join(parts[:3])


def build_level(book_entries_list, level, drop_words=None):
    """多本书合并去重(按小写拼写), 可选剔除 drop_words 集合"""
    seen = {}
    result = []
    dropped = 0
    for entries in book_entries_list:
        for rank, word, meaning in entries:
            key = word.lower()
            if drop_words and key in drop_words:
                dropped += 1
                continue
            if not meaning:
                continue  # 无释义的词条丢弃
            if key in seen:
                continue  # 去重, 先到先得(按书序)
            seen[key] = True
            result.append({"spelling": word, "meaning": meaning, "level": level})
    result.sort(key=lambda x: x["spelling"].lower())
    return result, dropped


def dump(level, words):
    path = os.path.join(DST, "words_%s_%s.json" % (level, TODAY))
    with open(path, "w", encoding="utf-8") as fh:
        json.dump({"words": words}, fh, ensure_ascii=False, indent=2)
    print("%s -> %d 词 (%.1f KB)" % (path, len(words), os.path.getsize(path) / 1024))
    return path


def main():
    # PETS(三级, 原口径 CET3, 2026-11-05 豆包拍板更名) <- 高中英语词汇(正序版) 3665:
    # 词量落在 PETS/三级大纲区间, 覆盖现有全部三级样例词
    pets, _ = build_level([load_book("GaoZhong_2")], "PETS")
    dump("PETS", pets)

    # CET4 <- 四级英语词汇(正序版) 3739: 教育部大学英语课程教学要求口径
    cet4, _ = build_level([load_book("CET4_2")], "CET4")
    dump("CET4", cet4)

    # CET6 <- 有道六级正序(2078) 为主, 补充六级真题核心词(1228) + 新东方六级(2345), 合并去重
    cet6, _ = build_level([load_book("CET6_2"), load_book("CET6_1"), load_book("CET6_3")], "CET6")
    # 手工补录: 现有 assets/words.json 样例词, 八本词书均未收录
    cet6.append({"spelling": "meticulous",
                 "meaning": "adj. 一丝不苟的；细致的",
                 "level": "CET6"})
    dump("CET6", cet6)

    # 校验
    print("\n--- 校验 ---")
    samples = {
        "PETS": ["beautiful", "borrow", "careful", "improve", "invite", "village", "weather"],
        "CET4": ["abandon", "absorb", "accurate", "adapt", "benefit", "capable", "concentrate"],
        "CET6": ["ambiguous", "coherent", "deteriorate", "inevitable", "meticulous", "prudent"],
    }
    for level, words in [("PETS", pets), ("CET4", cet4), ("CET6", cet6)]:
        idx = {w["spelling"].lower(): w for w in words}
        missing = [s for s in samples[level] if s.lower() not in idx]
        empty_meaning = sum(1 for w in words if not w["meaning"].strip())
        dup = len(words) - len(idx)
        print("%s: 样例缺失=%s, 空释义=%d, 重复=%d" % (level, missing or "无", empty_meaning, dup))
        for s in samples[level][:2]:
            if s.lower() in idx:
                print("   样例 %s -> %s" % (s, idx[s.lower()]["meaning"]))
    # 交叉重叠统计
    s3 = {w["spelling"].lower() for w in pets}
    s4 = {w["spelling"].lower() for w in cet4}
    s6 = {w["spelling"].lower() for w in cet6}
    print("\n重叠: PETS∩C4=%d, PETS∩C6=%d, C4∩C6=%d" % (len(s3 & s4), len(s3 & s6), len(s4 & s6)))


if __name__ == "__main__":
    main()
