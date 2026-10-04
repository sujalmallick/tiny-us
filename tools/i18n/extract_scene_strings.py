"""Moves the scene engine's messages into res/values/strings.xml (plan 04, D1b).

Handles showMessage("...") and boySpeechText/girlSpeechText = "..." in the given Kotlin files.
Template insertions ("${boy.name}", "$chosen") become format arguments (%1$s, %2$s, ...), so

    showMessage("${boy.name} & ${girl.name} - Forever and always", duration = 5.0f)

becomes

    showMessage(GameText.get(R.string.scene_forever_and_always, boy.name, girl.name), duration = 5.0f)

Literals joined to other text with + (or otherwise not the whole value) are left alone.

    python tools/i18n/extract_scene_strings.py <file.kt> [...]
"""
import os
import re
import sys

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
STRINGS = os.path.join(ROOT, "app", "src", "main", "res", "values", "strings.xml")
START = re.compile(r'(showMessage\(\s*|\b(?:boy|girl)SpeechText\s*=\s*)"')


def parse_literal(src, i):
    """Parses a Kotlin string literal whose opening quote is at src[i]. Returns
    (end_index_after_quote, parts) where parts is a list of ("text", raw) / ("expr", code),
    or None when the literal can't be handled safely."""
    assert src[i] == '"'
    if src.startswith('"""', i):
        return None
    j = i + 1
    parts = []
    buf = []
    while j < len(src):
        c = src[j]
        if c == "\\":
            buf.append(src[j:j + 2])
            j += 2
            continue
        if c == '"':
            if buf:
                parts.append(("text", "".join(buf)))
            return j + 1, parts
        if c == "\n":
            return None
        if c == "$" and j + 1 < len(src) and src[j + 1] == "{":
            depth = 1
            k = j + 2
            while k < len(src) and depth:
                if src[k] in "\"'\n":
                    return None
                if src[k] == "{":
                    depth += 1
                elif src[k] == "}":
                    depth -= 1
                k += 1
            if depth:
                return None
            if buf:
                parts.append(("text", "".join(buf)))
                buf = []
            parts.append(("expr", src[j + 2:k - 1].strip()))
            j = k
            continue
        if c == "$" and j + 1 < len(src) and (src[j + 1].isalpha() or src[j + 1] == "_"):
            k = j + 1
            while k < len(src) and (src[k].isalnum() or src[k] == "_"):
                k += 1
            if buf:
                parts.append(("text", "".join(buf)))
                buf = []
            parts.append(("expr", src[j + 1:k]))
            j = k
            continue
        buf.append(c)
        j += 1
    return None


def xml_text(parts):
    """Resource text: Kotlin escapes kept (Android understands them), insertions as %n$s."""
    has_args = any(kind == "expr" for kind, _ in parts)
    out = []
    n = 0
    for kind, val in parts:
        if kind == "expr":
            n += 1
            out.append(f"%{n}$s")
        else:
            t = val.replace("\\$", "$")
            if has_args:
                t = t.replace("%", "%%")
            t = t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "\\'")
            out.append(t)
    s = "".join(out)
    if s.startswith("@") or s.startswith("?"):
        s = "\\" + s
    return s


def slug(parts):
    text = " ".join(v for k, v in parts if k == "text")
    words = re.findall(r"[a-z0-9]+", text.lower())
    return "_".join(words)[:40].strip("_") or "message"


def main(files):
    xml = open(STRINGS, encoding="utf-8").read()
    keys = set(re.findall(r'<string name="([^"]+)"', xml))
    by_text = {}
    entries = []
    total = 0
    for path in files:
        src = open(path, encoding="utf-8").read()
        out = []
        pos = 0
        replaced = 0
        for m in START.finditer(src):
            q = m.end() - 1
            if q < pos:
                continue
            parsed = parse_literal(src, q)
            if not parsed:
                continue
            end, parts = parsed
            # Must be the whole value: next non-space character closes the argument or the line.
            rest = src[end:end + 40].lstrip(" ")
            if not (rest.startswith(",") or rest.startswith(")") or rest.startswith("\n") or rest.startswith("\r")):
                continue
            if not any(re.search(r"[A-Za-z]", v) for k, v in parts if k == "text"):
                continue
            text = xml_text(parts)
            key = by_text.get(text)
            if key is None:
                base = "scene_" + slug(parts)
                key = base
                n = 2
                while key in keys:
                    key = f"{base}_{n}"
                    n += 1
                keys.add(key)
                by_text[text] = key
                fmt = ' formatted="false"' if ("%" in text and not any(k == "expr" for k, _ in parts)) else ""
                entries.append(f'    <string name="{key}"{fmt}>{text}</string>')
            args = [v for k, v in parts if k == "expr"]
            call = "GameText.get(R.string." + key + ("" if not args else ", " + ", ".join(args)) + ")"
            out.append(src[pos:q])
            out.append(call)
            pos = end
            replaced += 1
        out.append(src[pos:])
        new = "".join(out)
        if replaced:
            for imp in ("import com.example.R", "import com.example.engine.GameText"):
                if imp not in new and not (imp.endswith("GameText") and "package com.example.engine" in new):
                    new = re.sub(r"(\nimport [^\n]+\n)", lambda mm: "\n" + imp + mm.group(1), new, count=1)
            open(path, "w", encoding="utf-8", newline="\n").write(new)
        print(f"{os.path.basename(path)}: {replaced} messages")
        total += replaced
    if entries:
        xml = xml.replace("</resources>", "    <!-- Scene messages (tools/i18n/extract_scene_strings.py) -->\n" + "\n".join(entries) + "\n</resources>")
        open(STRINGS, "w", encoding="utf-8", newline="\n").write(xml)
    print(f"total {total} replaced, {len(entries)} new strings")


if __name__ == "__main__":
    main(sys.argv[1:])
