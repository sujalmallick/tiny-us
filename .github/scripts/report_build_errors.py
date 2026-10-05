"""Turns Xcode, Swift and Kotlin/Gradle errors from the iOS build logs into GitHub annotations.

Job logs need a GitHub login to read; annotations are visible to anyone on a public repo,
so this makes a failed iOS build diagnosable from its check run alone.
"""
import os
import re
import sys
from pathlib import Path

WORKSPACE = os.environ.get("GITHUB_WORKSPACE", os.getcwd()).rstrip("/") + "/"
MAX_LOCATED = 9          # GitHub shows at most 10 error annotations per step
MAX_SUMMARY_LINES = 80

PATTERNS = [
    # Swift / clang / Xcode: /path/File.swift:12:5: error: message
    re.compile(r"^(?P<file>/[^:\n]+?):(?P<line>\d+):(?P<col>\d+): (?:fatal )?error: (?P<msg>.+)$"),
    # Kotlin: e: file:///path/File.kt:12:5 message
    re.compile(r"^e: file://(?P<file>/[^ \n]+?):(?P<line>\d+):(?P<col>\d+) (?P<msg>.+)$"),
]
GENERIC = re.compile(r"(^error: .+|^ld: .+|^clang: error: .+|: error: .+|^e: .+|FAILED$|^\* What went wrong:.*|^> .+|BUILD FAILED.*|\*\* (BUILD|ARCHIVE) FAILED \*\*)")


def escape(text: str) -> str:
    return text.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


def relative(path: str) -> str:
    return path[len(WORKSPACE):] if path.startswith(WORKSPACE) else path


def main(log_dir: str) -> None:
    logs = sorted(Path(log_dir).glob("*.log"), key=lambda p: p.stat().st_mtime)
    located, summary, seen = [], [], set()
    for log in logs:
        lines = log.read_text(errors="replace").splitlines()
        for raw in lines:
            line = raw.strip()
            for pattern in PATTERNS:
                match = pattern.match(line)
                if match:
                    key = (match["file"], match["line"], match["msg"])
                    if key not in seen:
                        seen.add(key)
                        located.append((relative(match["file"]), match["line"], match["col"], match["msg"]))
                    break
            else:
                if GENERIC.search(line) and line not in seen:
                    seen.add(line)
                    summary.append(f"[{log.stem}] {line[:300]}")

    for file, line, col, msg in located[:MAX_LOCATED]:
        print(f"::error file={file},line={line},col={col}::{escape(msg)}")

    report = [f"{file}:{line}:{col}: {msg}" for file, line, col, msg in located] + summary
    if not report and logs:
        tail = logs[-1].read_text(errors="replace").splitlines()[-MAX_SUMMARY_LINES:]
        report = [f"(no recognised errors; last lines of {logs[-1].name})"] + tail
    if report:
        body = "\n".join(report[:MAX_SUMMARY_LINES])
        print(f"::error title=iOS build failed ({len(located)} located errors)::{escape(body)}")
    else:
        print("::error title=iOS build failed::No build logs were produced before the failure.")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "build-logs")
