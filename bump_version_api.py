#!/usr/bin/env python3
"""
bump_version_api.py — version manager for safepoint-api only.
Run from the safepoint-api directory.

Keywords in commit message:
  [MAJOR] → bumps major, resets minor and patch to 0
  [MINOR] → bumps minor, resets patch to 0
  (none)  → patch = current git commit count
"""

import re
import subprocess
from datetime import date
from pathlib import Path

POM_XML         = Path("pom.xml")
APPLICATION_YML = Path("src/main/resources/application.yml")

def read_current_version() -> str:
    text = POM_XML.read_text(encoding="utf-8")
    m = re.search(
        r"<artifactId>safe-point-api</artifactId>\s*<version>(\d+\.\d+\.\d+)</version>",
        text
    )
    return m.group(1) if m else "0.8.0"

def get_git_commit_count() -> int:
    try:
        result = subprocess.run(
            ["git", "rev-list", "--count", "HEAD"],
            capture_output=True, text=True, check=True
        )
        return int(result.stdout.strip())
    except Exception:
        return 0

def get_last_commit_message() -> str:
    try:
        result = subprocess.run(
            ["git", "log", "-1", "--pretty=%B"],
            capture_output=True, text=True, check=True
        )
        return result.stdout.strip()
    except Exception:
        return ""

def compute_new_version(current: str) -> tuple[str, str]:
    major, minor, _ = map(int, current.split("."))
    msg = get_last_commit_message().upper()

    if "[MAJOR]" in msg:
        return f"{major + 1}.0.0", "major"
    if "[MINOR]" in msg:
        return f"{major}.{minor + 1}.0", "minor"

    patch = get_git_commit_count()
    return f"{major}.{minor}.{patch}", "patch"

def update_pom(new_version: str):
    text = POM_XML.read_text(encoding="utf-8")
    text = re.sub(
        r"(<artifactId>safe-point-api</artifactId>\s*<version>)\d+\.\d+\.\d+(</version>)",
        lambda m: f"{m.group(1)}{new_version}{m.group(2)}",
        text
    )
    POM_XML.write_text(text, encoding="utf-8")
    print(f"  pom.xml          -> {new_version}")

def update_yml(new_version: str):
    if not APPLICATION_YML.exists():
        print("  application.yml  -> not found, skipping")
        return

    today = date.today().isoformat()
    text  = APPLICATION_YML.read_text(encoding="utf-8")

    # Update version
    if re.search(r"version:\s*[\d.]+", text):
        text = re.sub(r"(version:\s*)[\d.]+", f"\\g<1>{new_version}", text)
    else:
        text += f"\napp:\n  version: {new_version}\n"

    # Update or add updated-at
    if re.search(r"updated-at:\s*\S+", text):
        text = re.sub(r"(updated-at:\s*)\S+", f"\\g<1>{today}", text)
    else:
        text = re.sub(r"(version:\s*[\d.]+)", f"\\g<1>\n  updated-at: {today}", text)

    APPLICATION_YML.write_text(text, encoding="utf-8")
    print(f"  application.yml  -> {new_version} ({today})")

def main():
    current = read_current_version()
    new_version, part = compute_new_version(current)

    print(f"Current: {current}")
    print(f"Bump:    {part}")
    print(f"New:     {new_version}\n")
    print("Updating files:")

    update_pom(new_version)
    update_yml(new_version)

if __name__ == "__main__":
    main()
