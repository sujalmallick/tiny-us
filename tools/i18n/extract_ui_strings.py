"""Moves hard-coded UI text into res/values/strings.xml (plan 04, D1).

Finds plain string literals used as Text("...") or as text/title/subtitle/label/
contentDescription/placeholder = "..." in the UI files (not the world-drawing files), adds each
distinct text once to strings.xml under a ui_* key, and replaces the literal with
stringResource(R.string.key). Literals built from variables ("${...}") are left alone.

    python tools/i18n/extract_ui_strings.py apply      # rewrite the sources, write the mapping
    python tools/i18n/extract_ui_strings.py revert F:L # put a literal back (file:line from a compile error)
"""
import glob
import json
import os
import re
import sys

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
UI = os.path.join(ROOT, "app", "src", "main", "java", "com", "example", "ui")
STRINGS = os.path.join(ROOT, "app", "src", "main", "res", "values", "strings.xml")
MAPPING = os.path.join(ROOT, "tools", "i18n", "ui_strings_map.json")

LITERAL = r'"((?:[^"\\$]|\\.)*)"'
PATTERNS = [
    re.compile(r'(?<![A-Za-z])(Text\(\s*)' + LITERAL),
    re.compile(r'(\b(?:text|title|subtitle|label|contentDescription|placeholder)\s*=\s*)' + LITERAL),
]
SKIP_FILES = ("World", "PixelWorldView.kt", "LowResWorldBuffer.kt")


def kotlin_unescape(s):
    return s.replace('\\"', '"').replace("\\$", "$").replace("\\\\", "\\")


def xml_escape(raw):
    """The literal exactly as written in Kotlin. Kotlin's escapes (backslash-n, backslash-quote,
    double backslash, backslash-u) are also valid Android string escapes, so they stay; only
    backslash-dollar becomes a dollar sign, apostrophes get a backslash, and XML specials become
    entities."""
    s = raw.replace("\\$", "$")
    s = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    s = s.replace("'", "\\'")
    if s.startswith("@") or s.startswith("?"):
        s = "\\" + s
    return s


def slug(text):
    words = re.findall(r"[a-z0-9]+", text.lower())
    out = "_".join(words)[:40].strip("_")
    return out or "text"


def apply():
    xml = open(STRINGS, encoding="utf-8").read()
    existing_keys = set(re.findall(r'<string name="([^"]+)"', xml))
    by_text = {}
    new_entries = []
    changes = []
    for path in sorted(glob.glob(os.path.join(UI, "*.kt"))):
        name = os.path.basename(path)
        if name.startswith(SKIP_FILES) or name in SKIP_FILES:
            continue
        lines = open(path, encoding="utf-8").read().split("\n")
        touched = False
        for i, line in enumerate(lines):
            if "stringResource(" in line and "R.string." in line and not any(p.search(line) for p in PATTERNS):
                continue
            # Animation labels and test tags are names for tools, not text people read.
            if "animate" in line or "Transition" in line or "testTag" in line:
                continue
            def repl(m):
                raw = m.group(2)
                text = kotlin_unescape(raw)
                if len(text.strip()) < 2 or not re.search(r"[A-Za-z]", text):
                    return m.group(0)
                key = by_text.get(text)
                if key is None:
                    base = "ui_" + slug(text)
                    key = base
                    n = 2
                    while key in existing_keys:
                        key = f"{base}_{n}"
                        n += 1
                    existing_keys.add(key)
                    by_text[text] = key
                    formatted = ' formatted="false"' if "%" in text else ""
                    new_entries.append(f'    <string name="{key}"{formatted}>{xml_escape(raw)}</string>')
                changes.append({"file": name, "line": i + 1, "key": key, "literal": raw})
                return f"{m.group(1)}stringResource(R.string.{key})"
            new = line
            for p in PATTERNS:
                new = p.sub(repl, new)
            if new != line:
                lines[i] = new
                touched = True
        if touched:
            src = "\n".join(lines)
            for imp in ("import androidx.compose.ui.res.stringResource", "import com.example.R"):
                if imp not in src:
                    src = re.sub(r"(\nimport [^\n]+\n)", lambda mm: "\n" + imp + mm.group(1), src, count=1)
            open(path, "w", encoding="utf-8", newline="\n").write(src)
    if new_entries:
        xml = xml.replace("</resources>", "    <!-- UI text moved out of the code (tools/i18n/extract_ui_strings.py) -->\n" + "\n".join(new_entries) + "\n</resources>")
        open(STRINGS, "w", encoding="utf-8", newline="\n").write(xml)
    json.dump(changes, open(MAPPING, "w", encoding="utf-8"), indent=1)
    print(f"{len(changes)} literals replaced, {len(new_entries)} new strings")


def revert(spec):
    changes = json.load(open(MAPPING, encoding="utf-8"))
    file, line = spec.rsplit(":", 1)
    line = int(line)
    path = os.path.join(UI, file)
    lines = open(path, encoding="utf-8").read().split("\n")
    # Match by what is on the line now (imports added at the top shift the recorded line numbers).
    hits = []
    for key in sorted(set(re.findall(r"stringResource\(R\.string\.(ui_\w+)\)", lines[line - 1]))):
        c = next((c for c in changes if c["file"] == file and c["key"] == key), None)
        if c:
            lines[line - 1] = lines[line - 1].replace(f"stringResource(R.string.{key})", '"' + c["literal"] + '"')
            hits.append(c)
    open(path, "w", encoding="utf-8", newline="\n").write("\n".join(lines))
    print("reverted", len(hits), "at", spec)


if __name__ == "__main__":
    if sys.argv[1] == "apply":
        apply()
    else:
        for spec in sys.argv[2:]:
            revert(spec)
