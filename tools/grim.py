#!/usr/bin/env python
"""
Read GrimAC detections for our dev player out of the server log.

  python grim.py            -> flags since the last mark
  python grim.py mark       -> mark current end of log (call before a test)
  python grim.py all        -> every flag in the log
  python grim.py raw 40     -> last 40 GrimAC lines, unfiltered
"""
import os
import re
import sys

LOG = r"E:/bleachhack/server-dev/logs/latest.log"
MARK = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".grim_mark")
PLAYER = "DefaultO"

# strip console color codes
ANSI = re.compile(r"\x1b\[[0-9;]*m")


def lines():
    if not os.path.exists(LOG):
        return []
    with open(LOG, encoding="utf-8", errors="replace") as f:
        return [ANSI.sub("", ln.rstrip("\n")) for ln in f]


def is_flag(ln):
    low = ln.lower()
    if PLAYER.lower() not in low:
        return False
    # GrimAC alert/verbose lines name a check and a violation level
    return any(k in low for k in ("grim", "vl:", "verbose", "flag", "violation"))


def main():
    arg = sys.argv[1] if len(sys.argv) > 1 else ""
    all_lines = lines()

    if arg == "mark":
        with open(MARK, "w") as f:
            f.write(str(len(all_lines)))
        print(f"marked at line {len(all_lines)}")
        return

    if arg == "raw":
        n = int(sys.argv[2]) if len(sys.argv) > 2 else 30
        hits = [ln for ln in all_lines if "grim" in ln.lower()]
        print("\n".join(hits[-n:]) or "(no GrimAC lines)")
        return

    start = 0
    if arg != "all" and os.path.exists(MARK):
        try:
            start = int(open(MARK).read().strip())
        except ValueError:
            start = 0

    hits = [ln for ln in all_lines[start:] if is_flag(ln)]
    if not hits:
        print(f"(no GrimAC flags for {PLAYER} since line {start})")
        return

    # summarise which checks fired, then show the detail
    checks = {}
    for ln in hits:
        # English: "X failed Check (x3)" / German: "X bestand Check nicht (x3)"
        m = (re.search(r"bestand\s+(\S+)\s+nicht", ln)
             or re.search(r"failed\s+(\S+)", ln)
             or re.search(r"([A-Za-z]+)\s+(?:VL|vl)[: ]", ln))
        name = m.group(1) if m else "?"
        checks[name] = checks.get(name, 0) + 1

    print(f"=== {len(hits)} GrimAC line(s) for {PLAYER} ===")
    if checks:
        print("checks: " + ", ".join(f"{k} x{v}" for k, v in sorted(checks.items(), key=lambda x: -x[1])))
    print("---")
    print("\n".join(hits[-60:]))


if __name__ == "__main__":
    main()
