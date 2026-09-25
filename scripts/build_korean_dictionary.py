#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Build the bundled Korean suggestion dictionary from the Leipzig Corpora Collection.

Source: Korean Wikipedia 2021, 1M sentences
    https://downloads.wortschatz-leipzig.de/corpora/kor_wikipedia_2021_1M.tar.gz
License: CC BY 4.0 (Leipzig Corpora Collection / Wortschatz Leipzig)

The words file is tokenized into eojeol (space-separated forms, so a noun plus
its particle stays one entry). Only Hangul-syllable tokens are kept. Frequencies
stay inside the 66–222 band used by the other bundled dictionaries. The head of
the list is kept at or above 150: SuggestionEngine drops prefix completions
whose raw frequency is below 150 when the typed word is one or two characters,
and a Korean syllable is one character.

Usage:
    python scripts/build_korean_dictionary.py --words /path/to/kor_wikipedia_2021_1M-words.txt
"""

import argparse
import json
import math
import os
import sys

HANGUL_SYLLABLE_MIN = 0xAC00
HANGUL_SYLLABLE_MAX = 0xD7A3
FREQ_TOP = 222
FREQ_BOTTOM = 66
# SuggestionEngine.suggestInternal rejects prefix completions below this raw
# frequency when the input is 1–2 characters. Keep this many headwords above it.
HEAD_RANK = 10000
HEAD_FLOOR = 150


def is_hangul_word(token: str, max_len: int) -> bool:
    if not token or len(token) > max_len:
        return False
    return all(HANGUL_SYLLABLE_MIN <= ord(ch) <= HANGUL_SYLLABLE_MAX for ch in token)


def load_leipzig_words(path: str):
    counts = {}
    with open(path, "r", encoding="utf-8") as handle:
        for line in handle:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            parts = line.split("\t") if "\t" in line else line.split()
            if len(parts) < 2:
                continue
            # Leipzig words.txt is "id<TAB>word<TAB>freq". A plain "word freq" list
            # is accepted too.
            if len(parts) >= 3 and parts[0].isdigit():
                word, freq_text = parts[1], parts[2]
            else:
                word, freq_text = parts[0], parts[-1]
            try:
                freq = int(freq_text)
            except ValueError:
                continue
            if freq <= 0:
                continue
            counts[word] = counts.get(word, 0) + freq
    return counts


def scale_frequency(rank: int, total: int) -> int:
    if total <= 1 or rank <= 1:
        return FREQ_TOP
    if rank <= HEAD_RANK:
        t = math.log(rank) / math.log(HEAD_RANK)
        scaled = FREQ_TOP - (FREQ_TOP - HEAD_FLOOR) * t
    else:
        span = max(2, total - HEAD_RANK)
        t = math.log(rank - HEAD_RANK + 1) / math.log(span)
        scaled = (HEAD_FLOOR - 1) - ((HEAD_FLOOR - 1) - FREQ_BOTTOM) * t
    return max(FREQ_BOTTOM, min(FREQ_TOP, int(round(scaled))))


def build_entries(counts: dict, max_words: int, max_len: int):
    ranked = sorted(
        ((word, freq) for word, freq in counts.items() if is_hangul_word(word, max_len)),
        key=lambda item: (-item[1], item[0]),
    )
    ranked = ranked[:max_words]
    total = len(ranked)
    return [
        {"w": word, "f": scale_frequency(index, total)}
        for index, (word, _freq) in enumerate(ranked, start=1)
    ]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--words", required=True, help="Leipzig *-words.txt path")
    parser.add_argument(
        "--output",
        default="app/src/main/assets/common/dictionaries/ko_base.json",
    )
    parser.add_argument("--max-words", type=int, default=50000)
    parser.add_argument("--max-len", type=int, default=10)
    args = parser.parse_args()

    if not os.path.exists(args.words):
        print(f"ERROR: words file not found: {args.words}", file=sys.stderr)
        return 1

    counts = load_leipzig_words(args.words)
    entries = build_entries(counts, args.max_words, args.max_len)
    if not entries:
        print("ERROR: no Hangul entries produced", file=sys.stderr)
        return 1

    os.makedirs(os.path.dirname(args.output), exist_ok=True)
    with open(args.output, "w", encoding="utf-8") as handle:
        handle.write("[\n")
        for index, entry in enumerate(entries):
            line = json.dumps(entry, ensure_ascii=False, separators=(", ", ": "))
            suffix = ",\n" if index < len(entries) - 1 else "\n"
            handle.write(f"  {line}{suffix}")
        handle.write("]\n")

    print(
        f"Wrote {len(entries)} entries to {args.output} "
        f"(freq {entries[-1]['f']}–{entries[0]['f']})"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
