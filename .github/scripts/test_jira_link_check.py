import os
import unittest
from unittest import mock

import jira_link_check as check


def event(title="", branch="", body="", login="aqib", labels=None):
    return {
        "pull_request": {
            "title": title,
            "body": body,
            "head": {"ref": branch},
            "user": {"login": login},
            "labels": [{"name": name} for name in (labels or [])],
        }
    }


def issue(project="APIM", team="0e6a33e4", subtask=False, parent=None):
    fields = {
        "project": {"key": project},
        "issuetype": {"subtask": subtask},
        "customfield_10001": {"id": team, "name": "APIM"} if team else None,
    }
    if parent:
        fields["parent"] = {"key": parent}
    return fields


class ExemptionTest(unittest.TestCase):
    def test_no_jira_label_does_not_call_jira(self):
        def transport(key):
            raise AssertionError(key)

        message = check.evaluate(event(labels=["no-jira"]), transport, {"PEN"})
        self.assertEqual(message, "PASS: exempt, label no-jira")

    def test_automated_version_bump_label(self):
        message = check.evaluate(event(labels=["automated-version-bump"]), lambda key: None, {"PEN"})
        self.assertEqual(message, "PASS: exempt, label automated-version-bump")

    def test_bot_author(self):
        message = check.evaluate(event(login="renovate[bot]"), lambda key: None, {"PEN"})
        self.assertEqual(message, "PASS: exempt, author renovate[bot]")


class CandidateTest(unittest.TestCase):
    def test_no_key(self):
        message = check.evaluate(event(title="chore: bump orb", branch="chore-bump-orb"), lambda key: None, {"PEN"})
        self.assertIn("no Jira key in the title, head branch, or body", message)
        self.assertIn(check.EXTERNAL_CONTRIBUTOR, message)

    def test_template_placeholder_is_not_a_key(self):
        self.assertEqual(check.candidate_keys("APIM-XXX", "", ""), [])

    def test_branch_is_uppercased(self):
        self.assertEqual(check.candidate_keys("", "feat/cj-5407-widget", ""), ["CJ-5407"])

    def test_order_is_title_then_branch_then_body_and_deduped(self):
        keys = check.candidate_keys("AM-1", "feat/am-1-and-APIM-2", "see APIM-2 and GMA-3")
        self.assertEqual(keys, ["AM-1", "APIM-2", "GMA-3"])

    def test_at_most_ten_are_looked_up(self):
        title = " ".join(f"APIM-{number}" for number in range(1, 15))
        seen = []

        def transport(key):
            seen.append(key)
            return None

        check.evaluate(event(title=title), transport, {"PEN"})
        self.assertEqual(seen, [f"APIM-{number}" for number in range(1, 11)])


class LookupTest(unittest.TestCase):
    def test_not_found_then_a_real_key_with_a_team(self):
        def transport(key):
            if key == "SHA-256":
                return None
            if key == "APIM-15565":
                return issue()
            raise AssertionError(key)

        message = check.evaluate(event(title="SHA-256", body="APIM-15565"), transport, {"PEN"})
        self.assertEqual(message, "PASS: APIM-15565 has an Atlassian Team")

    def test_every_candidate_missing(self):
        message = check.evaluate(event(title="SHA-256"), lambda key: None, {"PEN"})
        self.assertIn("`SHA-256` was not found in Jira or not visible", message)
        self.assertIn(check.EXTERNAL_CONTRIBUTOR, message)

    def test_existing_issue_without_a_team(self):
        message = check.evaluate(event(title="APIM-15244"), lambda key: issue(team=None), {"PEN"})
        self.assertIn("`APIM-15244` has no Team", message)
        self.assertIn("https://gravitee.atlassian.net/browse/APIM-15244", message)
        self.assertIn("on the parent if it is a subtask", message)

    def test_keeps_looking_after_an_issue_with_no_team(self):
        def transport(key):
            if key == "APIM-1":
                return issue(team=None)
            if key == "APIM-2":
                return issue()
            return None

        message = check.evaluate(event(title="APIM-1 APIM-2"), transport, {"PEN"})
        self.assertEqual(message, "PASS: APIM-2 has an Atlassian Team")

    def test_reports_the_first_existing_issue_when_none_has_a_team(self):
        def transport(key):
            if key == "APIM-1":
                return issue(team=None)
            return None

        message = check.evaluate(event(title="APIM-1 SHA-256"), transport, {"PEN"})
        self.assertIn("`APIM-1` has no Team", message)

    def test_subtask_reads_the_parent_team(self):
        def transport(key):
            if key == "APIM-14632":
                return issue(team=None, subtask=True, parent="APIM-14184")
            if key == "APIM-14184":
                return issue()
            raise AssertionError(key)

        message = check.evaluate(event(branch="fix/apim-14632-widget"), transport, {"PEN"})
        self.assertEqual(message, "PASS: APIM-14632 has an Atlassian Team")

    def test_pen_passes_with_a_key_alone(self):
        message = check.evaluate(event(title="PEN-88"), lambda key: issue(project="PEN", team=None), {"PEN"})
        self.assertEqual(message, "PASS: PEN-88 is in PEN, which needs no Team")

    def test_team_string_counts_as_set(self):
        fields = issue()
        fields["customfield_10001"] = "0e6a33e4-3e04-4a5c-b993-ff63538afe9f"
        message = check.evaluate(event(title="APIM-1"), lambda key: fields, {"PEN"})
        self.assertTrue(message.startswith("PASS"))


class RetryTest(unittest.TestCase):
    def test_401_is_not_retried(self):
        calls = []

        def call(key):
            calls.append(key)
            return 401, None, {}

        with self.assertRaises(check.JiraRejected):
            check.fetching(call, lambda delay: None)("APIM-1")
        self.assertEqual(calls, ["APIM-1"])

    def test_500_retries_then_succeeds(self):
        responses = [(500, None, {}), (500, None, {}), (200, issue(), {})]
        slept = []

        def call(key):
            return responses.pop(0)

        fields = check.fetching(call, slept.append)("APIM-1")
        self.assertEqual(fields["project"]["key"], "APIM")
        self.assertEqual(slept, [1, 1])

    def test_429_honours_retry_after(self):
        responses = [(429, None, {"Retry-After": "7"}), (200, issue(), {})]
        slept = []

        def call(key):
            return responses.pop(0)

        check.fetching(call, slept.append)("APIM-1")
        self.assertEqual(slept, [7])

    def test_exhausted_retries_are_unreachable(self):
        def call(key):
            return 503, None, {}

        with self.assertRaises(check.JiraUnreachable):
            check.fetching(call, lambda delay: None)("APIM-1")


class MainTest(unittest.TestCase):
    def test_exemption_passes_without_credentials(self):
        import json
        import tempfile

        payload = event(labels=["no-jira"])
        with tempfile.NamedTemporaryFile("w", encoding="utf-8", delete=False) as handle:
            json.dump(payload, handle)
            path = handle.name
        env = {"GITHUB_EVENT_PATH": path, "JIRA_USER_EMAIL": "", "JIRA_API_TOKEN": ""}
        with mock.patch.dict(os.environ, env, clear=False), mock.patch("sys.stdout"):
            code = check.main()
        self.assertEqual(code, 0)

    def test_missing_credentials_fail_when_a_key_is_present(self):
        import json
        import tempfile

        with tempfile.NamedTemporaryFile("w", encoding="utf-8", delete=False) as handle:
            json.dump(event(title="APIM-15565"), handle)
            path = handle.name
        env = {"GITHUB_EVENT_PATH": path, "JIRA_USER_EMAIL": "", "JIRA_API_TOKEN": ""}
        with mock.patch.dict(os.environ, env, clear=False):
            with mock.patch("sys.stdout"):
                code = check.main()
        self.assertEqual(code, 1)


class SummaryTest(unittest.TestCase):
    def test_summary_gets_the_same_line(self):
        with mock.patch.dict(os.environ, {"GITHUB_STEP_SUMMARY": ""}):
            pass
        import tempfile

        with tempfile.NamedTemporaryFile("w+", encoding="utf-8") as handle:
            with mock.patch.dict(os.environ, {"GITHUB_STEP_SUMMARY": handle.name}):
                check.write_summary("PASS: exempt, label no-jira")
            handle.seek(0)
            self.assertEqual(handle.read(), "PASS: exempt, label no-jira\n")


if __name__ == "__main__":
    unittest.main()
