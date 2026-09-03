"""Update a bundled plugin's version property in the APIM poms, but only upwards.

Reads MODULE, VERSION, GITHUB_ENV and optionally BUMP_PENDING_REF (a git ref holding
an earlier, still open bump) from the environment; see bump-from-module.yml.
Writes BUMP_SKIPPED=true|false to GITHUB_ENV and, when updated, BUMP_POM_FILE=<path>.
"""

import os
import re
import subprocess
import sys

# Bundled plugin versions live in the distribution pom; engine and third-party
# ones stay in the root pom. Look in both, distribution first.
POM_CANDIDATES = ["gravitee-apim-distribution/pom.xml", "pom.xml"]

# Inputs come from workflow_dispatch and end up in a regex, XML, a branch name and a
# commit message, so anything outside a Maven artifact id or version is refused.
MODULE_RE = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._-]*$")
VERSION_RE = re.compile(r"^\d+(\.\d+){0,3}(-[A-Za-z0-9]+(\.[A-Za-z0-9]+)*)?$")

UPDATED = "updated"
SKIPPED = "skipped"


def validate_inputs(module, version):
    if not MODULE_RE.match(module):
        raise ValueError(f"module is not a Maven artifact id: {module!r}")
    if not VERSION_RE.match(version):
        raise ValueError(f"version is not like 1.2.3 or 1.2.3-alpha.4: {version!r}")


def parse_version(version):
    """Return a sortable key for a Maven/semver-style version.

    Missing segments count as zero (1.13 == 1.13.0). A release sorts above any
    prerelease of the same base; prerelease labels compare case-insensitively and
    their numeric parts numerically (alpha.10 > alpha.9).
    """
    if not VERSION_RE.match(version):
        raise ValueError(f"unparseable version: {version!r}")
    base, _, qualifier = version.partition("-")
    numbers = tuple(int(part) for part in base.split("."))
    numbers += (0,) * (4 - len(numbers))
    if not qualifier:
        return (numbers, 1, ())
    parts = tuple((0, int(p)) if p.isdigit() else (1, p.lower()) for p in qualifier.split("."))
    return (numbers, 0, parts)


def is_newer(new, current):
    return parse_version(new) > parse_version(current)


def find_pom(prop_pattern):
    for path in POM_CANDIDATES:
        with open(path) as fh:
            if prop_pattern.search(fh.read()):
                return path
    return None


def version_at_ref(ref, path, prop_pattern):
    """The property's value in `path` on git ref `ref`, or None if either is absent."""
    shown = subprocess.run(["git", "show", f"{ref}:{path}"], capture_output=True, text=True)
    match = prop_pattern.search(shown.stdout) if shown.returncode == 0 else None
    return match.group(1).strip() if match else None


def append_github_env(github_env, **values):
    with open(github_env, "a") as fh:
        for key, value in values.items():
            fh.write(f"{key}={value}\n")


def fail(message):
    print(f"ERROR: {message}", file=sys.stderr)
    sys.exit(1)


def skip(github_env, level, message):
    print(f"::{level} title=Bump skipped::{message}")
    append_github_env(github_env, BUMP_SKIPPED="true")
    return SKIPPED


def bump_if_newer(module, version, github_env, pending_ref=None):
    try:
        validate_inputs(module, version)
    except ValueError as err:
        fail(str(err))

    prop = f"{module}.version"
    pattern = re.compile(rf"<{re.escape(prop)}>([^<]+)</{re.escape(prop)}>")
    target = find_pom(pattern)
    if target is None:
        fail(f"<{prop}> not found in {' or '.join(POM_CANDIDATES)}")

    with open(target) as fh:
        content = fh.read()
    current, where = pattern.search(content).group(1).strip(), "master"
    if not VERSION_RE.match(current):
        return skip(github_env, "warning", f"{target} has <{prop}> {current!r}, which this script cannot compare. Bump by hand if needed.")

    # An earlier release may already be waiting in an open PR; never regress it.
    pending = version_at_ref(pending_ref, target, pattern) if pending_ref else None
    if pending and VERSION_RE.match(pending) and is_newer(pending, current):
        current, where = pending, pending_ref

    if not is_newer(version, current):
        return skip(github_env, "notice", f"{module} {version} is not newer than {current} on {where}. If this release is for a maintenance branch, bump that branch by hand.")

    with open(target, "w") as fh:
        fh.write(pattern.sub(lambda _: f"<{prop}>{version}</{prop}>", content, count=1))
    print(f"Updated <{prop}> from {current} to {version} in {target}")
    append_github_env(github_env, BUMP_SKIPPED="false", BUMP_POM_FILE=target)
    return UPDATED


if __name__ == "__main__":
    bump_if_newer(
        os.environ["MODULE"],
        os.environ["VERSION"],
        os.environ["GITHUB_ENV"],
        os.environ.get("BUMP_PENDING_REF") or None,
    )
