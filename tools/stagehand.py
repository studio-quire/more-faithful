#!/usr/bin/env python3
"""Drives Stagehand (docs/stagehand.md): sends a script to a running game and prints what happened.

  python tools/stagehand.py tp tower nearest floor=3
  python tools/stagehand.py "tp 100 70 -40; kit iron; heal; status"
  python tools/stagehand.py -f setup.txt
  python tools/stagehand.py --json status
  python tools/stagehand.py where              which game folder it would use
  python tools/stagehand.py result <id>        a result again, or one it stopped waiting for

Options, before the command:
  --dir DIR        the game's folder (.minecraft, or a server's). Or STAGEHAND_DIR. Otherwise the Prism instance with Stagehand in it, the latest played if several.
  --as NAME        who @me is, for a server with several players
  --timeout SECS   how long to wait for it to finish (default 30). It carries on in the game; `result` reads it later.
  --world-wait SECS  keep trying this long while no world is open or nobody's in it (default 0: say so at once)
  --no-wait        send it and print its id
  --json           the result as the mod wrote it
  --keep           leave the result file behind

Exit status: 0 it all ran; 1 the script failed; 2 nothing answered, or still running at the timeout; 3 this tool's own usage.
"""

import json
import os
import sys
import tempfile
import time
import uuid
from pathlib import Path

PICKUP_SECONDS = 10.0  # The mod looks once a second, and leaves a file alone for a moment after it's touched.
NOT_YET = {"no_world", "no_player"}


def usage(message):
    print(f"stagehand: {message}", file=sys.stderr)
    return 3


def prism_instances():
    roots = []
    for env, sub in (("APPDATA", "PrismLauncher"), ("LOCALAPPDATA", "PrismLauncher"), ("HOME", ".local/share/PrismLauncher")):
        base = os.environ.get(env)
        if base:
            roots.append(Path(base) / sub / "instances")
    for root in roots:
        if not root.is_dir():
            continue
        for instance in root.iterdir():
            for name in (".minecraft", "minecraft"):
                game = instance / name
                if game.is_dir():
                    yield game


def has_stagehand(game):
    for mods in ("mods", "mods-library"):
        folder = game / mods
        if folder.is_dir() and any(p.name.startswith("stagehand") and p.suffix == ".jar" for p in folder.iterdir()):
            return True
    return False


def last_played(game):
    newest = 0.0
    for name in ("logs/purefaithful/latest.log", "logs/latest.log", "options.txt"):
        path = game / name
        if path.exists():
            newest = max(newest, path.stat().st_mtime)
    return newest


def game_dir(given):
    if given:
        return Path(given)
    if os.environ.get("STAGEHAND_DIR"):
        return Path(os.environ["STAGEHAND_DIR"])
    found = [g for g in prism_instances() if has_stagehand(g)]
    if not found:
        raise SystemExit(usage("no Prism instance has Stagehand in it: say which game with --dir or STAGEHAND_DIR (docs/stagehand.md has how to install it)"))
    return max(found, key=last_played)


def send(game, text):
    inbox = game / "stagehand" / "in"
    inbox.mkdir(parents=True, exist_ok=True)
    script_id = time.strftime("%Y%m%d-%H%M%S-") + uuid.uuid4().hex[:6]
    # Whole or not at all: written beside the folder's scripts, then moved in.
    handle, temp = tempfile.mkstemp(dir=inbox, suffix=".tmp")
    with os.fdopen(handle, "w", encoding="utf-8", newline="\n") as out:
        out.write(text)
    os.replace(temp, inbox / f"{script_id}.txt")
    return script_id


def read_result(game, script_id):
    path = game / "stagehand" / "out" / f"{script_id}.json"
    # On Windows the mod can be replacing it just as this reads, and a reader can be refused for that moment.
    for _ in range(20):
        try:
            return json.loads(path.read_text(encoding="utf-8"))
        except FileNotFoundError:
            return None
        except (PermissionError, json.JSONDecodeError):
            time.sleep(0.05)
    return None


def take_back(game, script_id):
    """True if the script was still lying there, so nothing will run it."""
    try:
        (game / "stagehand" / "in" / f"{script_id}.txt").unlink()
        return True
    except OSError:
        return False


def remove_result(game, script_id):
    try:
        (game / "stagehand" / "out" / f"{script_id}.json").unlink()
    except OSError:
        pass


def wait_for(game, script_id, timeout):
    """The result once it's over, or the last one seen, or None if nothing ever took the script."""
    picked_by = time.monotonic() + PICKUP_SECONDS
    ends = time.monotonic() + timeout
    latest = None
    while True:
        result = read_result(game, script_id)
        if result is not None:
            latest = result
            if result.get("state") != "running":
                return latest
        elif time.monotonic() > picked_by:
            take_back(game, script_id)
            result = read_result(game, script_id)
            return result
        if time.monotonic() > ends:
            return latest
        time.sleep(0.1)


def brief(data):
    return " ".join(f"{k}={json.dumps(v, ensure_ascii=False)}" for k, v in data.items())


def show(result, as_json):
    if as_json:
        print(json.dumps(result, indent=2, ensure_ascii=False))
        return
    for step in result.get("steps", []):
        mark = "ok  " if step.get("ok") else "FAIL"
        print(f"{mark} {step.get('command')}")
        if step.get("data"):
            print(f"     {brief(step['data'])}")
    error = result.get("error")
    state = result.get("state")
    if state == "running":
        print(f"still running: {result.get('id')}, on: {result.get('waiting_on')}" + (" (the game is paused)" if result.get("paused") else ""))
    elif state != "done":
        for problem in result.get("errors") or ([error] if error else []):
            line = f"line {problem['line']}: " if "line" in problem else ""
            print(f"{state}: {line}{problem.get('code')}: {problem.get('message')}")
        if result.get("not_run"):
            print(f"{result['not_run']} more line(s) not run")


def status_of(result):
    if result is None:
        return 2
    if result.get("state") == "running":
        return 2
    return 0 if result.get("ok") else 1


def main(argv):
    options = {"dir": None, "as": None, "timeout": 30.0, "world-wait": 0.0, "no-wait": False, "json": False, "keep": False, "file": None}
    args = list(argv)
    while args and args[0].startswith("-") and not args[0].lstrip("-")[:1].isdigit():
        flag = args.pop(0)
        name = flag.lstrip("-")
        if name in ("h", "help"):
            print(__doc__)
            return 0
        if name in ("no-wait", "json", "keep"):
            options[name] = True
        elif name in ("dir", "as", "timeout", "world-wait", "f", "file"):
            if not args:
                return usage(f"{flag} wants a value")
            value = args.pop(0)
            if name in ("timeout", "world-wait"):
                try:
                    value = float(value)
                except ValueError:
                    return usage(f"{flag} wants a number of seconds")
            options["file" if name == "f" else name] = value
        else:
            return usage(f"no option {flag}; --help says what there is")

    game = game_dir(options["dir"])
    if args[:1] == ["where"]:
        print(game)
        return 0
    if args[:1] == ["result"]:
        if len(args) != 2:
            return usage("result wants an id")
        result = read_result(game, args[1])
        if result is None:
            print(f"no result for {args[1]} in {game}", file=sys.stderr)
            return 2
        show(result, options["json"])
        if result.get("state") != "running" and not options["keep"]:
            remove_result(game, args[1])
        return status_of(result)

    if options["file"]:
        if args:
            return usage("either -f or a command, not both")
        text = sys.stdin.read() if options["file"] == "-" else Path(options["file"]).read_text(encoding="utf-8")
    elif args:
        # One argument with semicolons is several lines. A `say` holding a semicolon needs a file.
        text = "\n".join(line.strip() for line in " ".join(args).split(";")) + "\n"
    else:
        print(__doc__)
        return 3
    if options["as"]:
        text = f"as {options['as']}\n{text}"

    keep_trying_until = time.monotonic() + options["world-wait"]
    while True:
        script_id = send(game, text)
        if options["no-wait"]:
            print(script_id)
            return 0
        result = wait_for(game, script_id, options["timeout"])
        code = (result or {}).get("error", {}).get("code") if result else None
        if code in NOT_YET and time.monotonic() < keep_trying_until:
            remove_result(game, script_id)
            time.sleep(1.0)
            continue
        break

    if result is None:
        print(f"nothing took the script in {PICKUP_SECONDS:.0f} seconds, so it's been taken back. Is the game running from {game}, with Stagehand in its mods?", file=sys.stderr)
        return 2
    show(result, options["json"])
    if result.get("state") != "running" and not options["keep"]:
        remove_result(game, script_id)
    return status_of(result)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
