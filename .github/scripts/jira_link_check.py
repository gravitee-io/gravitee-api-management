"""Fail a pull request unless it cites a Jira issue that has an Atlassian Team.

The key may be in the title, the head branch, or the body. One key may cover
many pull requests. This script never chooses a team: it only checks that
Atlassian Team (customfield_10001) is set, reading the parent when the issue
is a subtask.
"""

import base64
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request

KEY_PATTERN = re.compile(r"\b[A-Z][A-Z0-9]+-\d+\b")
MAX_CANDIDATES = 10
# One attempt plus three retries. A 401 or 403 never retries.
MAX_ATTEMPTS = 4
EXEMPT_LABELS = ("no-jira", "automated-version-bump")
TEAM_FIELD = "customfield_10001"
ISSUE_FIELDS = "issuetype,parent,project,customfield_10001"

EXTERNAL_CONTRIBUTOR = "External contributor? A maintainer will add the `no-jira` label."
CREDENTIALS_REJECTED = "FAIL: Jira credentials rejected, the JIRA_API_TOKEN secret needs attention"
JIRA_UNREACHABLE = "FAIL: Jira unreachable, rerun the check"


class JiraRejected(Exception):
    """401 or 403. An expired token must not be reported as a missing issue."""


class JiraUnreachable(Exception):
    """The retries are exhausted on a 429, a 5xx, or a connection error."""


def candidate_keys(title, branch, body):
    """Keys from the title, then the uppercased branch, then the body. At most 10."""
    found = []
    seen = set()
    for text in (title or "", (branch or "").upper(), body or ""):
        for key in KEY_PATTERN.findall(text):
            if key in seen:
                continue
            seen.add(key)
            found.append(key)
            if len(found) == MAX_CANDIDATES:
                return found
    return found


def team_is_set(value):
    if value is None or value == "":
        return False
    if isinstance(value, str):
        return bool(value.strip())
    if isinstance(value, dict):
        return bool(value.get("id") or value.get("name") or value.get("value"))
    if isinstance(value, list):
        return any(team_is_set(item) for item in value)
    return True


def exempt_reason(event):
    pull_request = event.get("pull_request") or {}
    labels = {label.get("name") for label in pull_request.get("labels") or []}
    for name in EXEMPT_LABELS:
        if name in labels:
            return f"PASS: exempt, label {name}"
    login = ((pull_request.get("user") or {}).get("login")) or ""
    if login.endswith("[bot]"):
        return f"PASS: exempt, author {login}"
    return None


def _project_key(fields):
    project = fields.get("project") or {}
    return project.get("key") or ""


def _is_subtask(fields):
    issue_type = fields.get("issuetype") or {}
    return bool(issue_type.get("subtask"))


def _parent_key(fields):
    parent = fields.get("parent") or {}
    return parent.get("key") or ""


def evaluate(event, transport, exempt_projects):
    """Return the line to print. transport(key) returns the issue fields, or None on 404.

    It raises JiraRejected or JiraUnreachable. The first issue with a Team passes.
    If none has a Team but one exists, the failure names the first existing one.
    """
    reason = exempt_reason(event)
    if reason:
        return reason

    pull_request = event.get("pull_request") or {}
    keys = candidate_keys(
        pull_request.get("title"),
        (pull_request.get("head") or {}).get("ref"),
        pull_request.get("body"),
    )
    if not keys:
        return (
            "FAIL: no Jira key in the title, head branch, or body. "
            "Add a key such as APIM-1234 to the title or the head branch. "
            + EXTERNAL_CONTRIBUTOR
        )

    missing = []
    first_without_team = None
    for key in keys:
        fields = transport(key)
        if fields is None:
            missing.append(key)
            continue
        if _project_key(fields) in exempt_projects:
            return f"PASS: {key} is in {_project_key(fields)}, which needs no Team"
        team_fields = fields
        if _is_subtask(fields):
            parent = _parent_key(fields)
            if not parent:
                if first_without_team is None:
                    first_without_team = key
                continue
            parent_fields = transport(parent)
            if parent_fields is None:
                if first_without_team is None:
                    first_without_team = key
                continue
            team_fields = parent_fields
        if team_is_set(team_fields.get(TEAM_FIELD)):
            return f"PASS: {key} has an Atlassian Team"
        if first_without_team is None:
            first_without_team = key

    if first_without_team is not None:
        url = f"https://gravitee.atlassian.net/browse/{first_without_team}"
        return (
            f"FAIL: `{first_without_team}` has no Team; set Atlassian Team on {url} "
            "(on the parent if it is a subtask), then rerun"
        )

    quoted = ", ".join(f"`{key}`" for key in missing)
    verb = "was" if len(missing) == 1 else "were"
    return (
        f"FAIL: {quoted} {verb} not found in Jira or not visible to the check's account. "
        + EXTERNAL_CONTRIBUTOR
    )


def _retry_after_seconds(headers):
    raw = ""
    if headers:
        raw = headers.get("Retry-After") or headers.get("retry-after") or ""
    try:
        return max(0, int(raw))
    except (TypeError, ValueError):
        return 1


def fetching(call, sleeper):
    """call(key) -> (status, fields or None, headers). Retries 429 and 5xx."""

    def transport(key):
        last_error = None
        for attempt in range(MAX_ATTEMPTS):
            try:
                status, fields, headers = call(key)
            except JiraUnreachable:
                last_error = JiraUnreachable()
                status = None
                headers = {}
            else:
                if status in (401, 403):
                    raise JiraRejected()
                if status == 404:
                    return None
                if status == 200:
                    return fields or {}
                last_error = JiraUnreachable()
            if attempt == MAX_ATTEMPTS - 1:
                break
            delay = _retry_after_seconds(headers) if status == 429 else 1
            sleeper(delay)
        raise last_error or JiraUnreachable()

    return transport


def urllib_call(base_url, email, token):
    if not email or not token:
        raise JiraRejected()
    credentials = base64.b64encode(f"{email}:{token}".encode()).decode()
    root = base_url.rstrip("/")

    def call(key):
        url = f"{root}/rest/api/3/issue/{key}?fields={ISSUE_FIELDS}"
        request = urllib.request.Request(
            url,
            headers={
                "Authorization": f"Basic {credentials}",
                "Accept": "application/json",
            },
        )
        try:
            with urllib.request.urlopen(request, timeout=20) as response:
                payload = json.loads(response.read().decode())
                return 200, payload.get("fields") or {}, dict(response.headers)
        except urllib.error.HTTPError as error:
            return error.code, None, dict(error.headers or {})
        except urllib.error.URLError:
            raise JiraUnreachable()

    return call


def load_event(path):
    with open(path, encoding="utf-8") as handle:
        return json.load(handle)


def write_summary(text):
    path = os.environ.get("GITHUB_STEP_SUMMARY")
    if not path:
        return
    with open(path, "a", encoding="utf-8") as handle:
        handle.write(text + "\n")


def main(argv=None):
    event_path = os.environ.get("GITHUB_EVENT_PATH")
    if not event_path:
        print("FAIL: GITHUB_EVENT_PATH is not set", file=sys.stderr)
        return 1
    event = load_event(event_path)
    exempt = {item.strip() for item in os.environ.get("TEAM_EXEMPT_PROJECTS", "PEN").split(",") if item.strip()}
    # An exemption must not need Jira credentials.
    message = exempt_reason(event)
    if message is None:
        try:
            call = urllib_call(
                os.environ.get("JIRA_BASE_URL", "https://gravitee.atlassian.net"),
                os.environ.get("JIRA_USER_EMAIL", ""),
                os.environ.get("JIRA_API_TOKEN", ""),
            )
            message = evaluate(event, fetching(call, time.sleep), exempt)
        except JiraRejected:
            message = CREDENTIALS_REJECTED
        except JiraUnreachable:
            message = JIRA_UNREACHABLE
    print(message)
    write_summary(message)
    return 0 if message.startswith("PASS") else 1


if __name__ == "__main__":
    sys.exit(main())
