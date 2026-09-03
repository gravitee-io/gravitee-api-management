import contextlib
import io
import os
import subprocess
import tempfile
import unittest
from pathlib import Path

import bump_module_version as bump


class IsNewerTest(unittest.TestCase):
    def assert_newer(self, new, current):
        self.assertTrue(bump.is_newer(new, current), f"{new} should be newer than {current}")

    def assert_not_newer(self, new, current):
        self.assertFalse(bump.is_newer(new, current), f"{new} should not be newer than {current}")

    def test_plain_semver(self):
        self.assert_newer("1.13.1", "1.13.0")
        self.assert_newer("1.21.0", "1.16.0")
        self.assert_newer("2.0.0", "1.99.99")
        self.assert_not_newer("1.13.0", "1.13.1")

    def test_equal_is_not_newer(self):
        self.assert_not_newer("4.3.0-alpha.26", "4.3.0-alpha.26")
        self.assert_not_newer("1.2.0", "1.2.0")

    def test_maintenance_release_is_below_development_line(self):
        # The reported bug: AIM 1.x release must not touch master, which follows 4.3.0-alpha.x
        self.assert_not_newer("1.13.1", "4.3.0-alpha.26")
        # AuthZ 1.2.x release must not touch master, which is on 1.16.0
        self.assert_not_newer("1.2.1", "1.16.0")

    def test_prerelease_ordering(self):
        self.assert_newer("4.3.0-alpha.24", "4.3.0-alpha.23")
        self.assert_newer("1.0.0-alpha.10", "1.0.0-alpha.9")
        self.assert_newer("4.3.0", "4.3.0-alpha.23")
        self.assert_not_newer("4.3.0-alpha.23", "4.3.0")
        self.assert_newer("1.5.0-alpha.10", "1.4.2")
        self.assert_not_newer("1.4.2", "1.5.0-alpha.5")
        self.assert_newer("1.0.0-beta.1", "1.0.0-alpha.9")
        self.assert_newer("1.0.0-rc.1", "1.0.0-beta.3")

    def test_qualifiers_compare_case_insensitively(self):
        self.assert_newer("1.0.0-RC.1", "1.0.0-beta.1")
        self.assert_not_newer("1.0.0-Alpha.3", "1.0.0-alpha.3")

    def test_missing_segments_mean_zero_like_maven(self):
        self.assert_not_newer("1.13", "1.13.0")
        self.assert_not_newer("1.13.0", "1.13")
        self.assert_newer("1.13.1", "1.13")
        self.assert_not_newer("1.2.3", "1.2.3.0")
        self.assert_newer("1.7.15.2", "1.7.15.1")

    def test_rejects_malformed_versions(self):
        for bad in ["1.x", "v1.13.1", "1.0.0+build.1", "1.0.0-alpha 1", r"\1", "1.0.0</x>", "", "8.0.2.Final"]:
            with self.subTest(bad=bad), self.assertRaises(ValueError):
                bump.parse_version(bad)


class ValidateInputsTest(unittest.TestCase):
    def test_accepts_real_shapes(self):
        for module, version in [
            ("gravitee-gamma-module-aim", "4.3.0-alpha.26"),
            ("gravitee-common-mcp", "2.0.0"),
            ("gravitee-policy-mcp-acl", "1.0.0-rc.2"),
        ]:
            bump.validate_inputs(module, version)

    def test_rejects_hostile_inputs(self):
        for module, version in [
            ("gravitee-gamma-module-aim", r"\1"),
            ("gravitee-gamma-module-aim", "1.0.0</prop><inject/>"),
            ("gravitee-gamma-module-aim", '1.0.0" ; rm -rf /'),
            ("gravitee-gamma-module-aim", "v1.13.1"),
            ("module with spaces", "1.0.0"),
            ("../pom", "1.0.0"),
            ("", "1.0.0"),
        ]:
            with self.subTest(module=module, version=version), self.assertRaises(ValueError):
                bump.validate_inputs(module, version)


class BumpIfNewerTest(unittest.TestCase):
    MODULE = "gravitee-gamma-module-aim"
    PROP = f"{MODULE}.version"
    PENDING_BRANCH = f"bot/bump-{MODULE}"

    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        (self.root / "gravitee-apim-distribution").mkdir()
        self.dist_pom = self.root / "gravitee-apim-distribution" / "pom.xml"
        self.root_pom = self.root / "pom.xml"
        self.github_env = self.root / "github_env"
        self.write_dist_pom("4.3.0-alpha.26")
        self.root_pom.write_text("<project><properties></properties></project>\n")
        self.cwd = os.getcwd()
        os.chdir(self.root)
        self.git("init", "-q", "-b", "master")
        self.git("-c", "user.name=t", "-c", "user.email=t@t", "commit", "-q", "--allow-empty", "-m", "init")
        self.git("add", ".")
        self.git("-c", "user.name=t", "-c", "user.email=t@t", "commit", "-q", "-m", "poms")

    def tearDown(self):
        os.chdir(self.cwd)
        self.tmp.cleanup()

    def git(self, *args):
        subprocess.run(["git", *args], check=True, capture_output=True)

    def write_dist_pom(self, aim_version):
        self.dist_pom.write_text(
            "<project>\n"
            "  <properties>\n"
            f"    <{self.PROP}>{aim_version}</{self.PROP}>\n"
            "    <gravitee-gamma-module-authz.version>1.16.0</gravitee-gamma-module-authz.version>\n"
            "  </properties>\n"
            "</project>\n"
        )

    def make_pending_branch(self, aim_version):
        self.git("checkout", "-q", "-b", self.PENDING_BRANCH)
        self.write_dist_pom(aim_version)
        self.git("-c", "user.name=t", "-c", "user.email=t@t", "commit", "-q", "-am", "pending bump")
        self.git("checkout", "-q", "master")

    def run_bump(self, module, version, pending_ref=None):
        out = io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(io.StringIO()):
            outcome = bump.bump_if_newer(module, version, str(self.github_env), pending_ref)
        self.last_output = out.getvalue()
        return outcome

    def github_env_lines(self):
        return self.github_env.read_text().splitlines()

    def test_newer_version_updates_pom_and_reports_file(self):
        outcome = self.run_bump(self.MODULE, "4.3.0-alpha.27")
        self.assertEqual(outcome, bump.UPDATED)
        self.assertIn(f"<{self.PROP}>4.3.0-alpha.27</{self.PROP}>", self.dist_pom.read_text())
        self.assertIn("BUMP_POM_FILE=gravitee-apim-distribution/pom.xml", self.github_env_lines())
        self.assertIn("BUMP_SKIPPED=false", self.github_env_lines())

    def test_older_version_is_skipped_with_a_visible_notice(self):
        before = self.dist_pom.read_text()
        outcome = self.run_bump(self.MODULE, "1.13.1")
        self.assertEqual(outcome, bump.SKIPPED)
        self.assertEqual(self.dist_pom.read_text(), before)
        self.assertEqual(self.github_env_lines(), ["BUMP_SKIPPED=true"])
        self.assertIn("::notice title=Bump skipped::", self.last_output)
        self.assertIn("1.13.1 is not newer than 4.3.0-alpha.26 on master", self.last_output)

    def test_same_version_is_skipped(self):
        outcome = self.run_bump(self.MODULE, "4.3.0-alpha.26")
        self.assertEqual(outcome, bump.SKIPPED)
        self.assertIn("BUMP_SKIPPED=true", self.github_env_lines())

    def test_whitespace_around_pom_value_is_ignored(self):
        self.write_dist_pom("  4.3.0-alpha.26\n    ")
        outcome = self.run_bump(self.MODULE, "4.3.0-alpha.26")
        self.assertEqual(outcome, bump.SKIPPED)

    def test_only_the_requested_module_changes(self):
        self.run_bump(self.MODULE, "4.3.0-alpha.27")
        self.assertIn("<gravitee-gamma-module-authz.version>1.16.0<", self.dist_pom.read_text())

    def test_only_the_first_occurrence_is_rewritten(self):
        self.dist_pom.write_text(
            f"<a><{self.PROP}>4.3.0-alpha.26</{self.PROP}></a>\n"
            f"<b><{self.PROP}>4.3.0-alpha.26</{self.PROP}></b>\n"
        )
        self.run_bump(self.MODULE, "4.3.0-alpha.27")
        self.assertEqual(self.dist_pom.read_text().count("4.3.0-alpha.27"), 1)

    def test_falls_back_to_root_pom(self):
        self.root_pom.write_text(
            "<project><properties>"
            "<gravitee-common-mcp.version>2.0.0</gravitee-common-mcp.version>"
            "</properties></project>\n"
        )
        outcome = self.run_bump("gravitee-common-mcp", "2.1.0")
        self.assertEqual(outcome, bump.UPDATED)
        self.assertIn("<gravitee-common-mcp.version>2.1.0<", self.root_pom.read_text())
        self.assertIn("BUMP_POM_FILE=pom.xml", self.github_env_lines())

    def test_distribution_pom_wins_when_both_define_the_property(self):
        self.root_pom.write_text(
            f"<project><properties><{self.PROP}>1.0.0</{self.PROP}></properties></project>\n"
        )
        outcome = self.run_bump(self.MODULE, "4.3.0-alpha.27")
        self.assertEqual(outcome, bump.UPDATED)
        self.assertIn("4.3.0-alpha.27", self.dist_pom.read_text())
        self.assertIn(f"<{self.PROP}>1.0.0<", self.root_pom.read_text())

    def test_pending_bump_branch_is_not_overwritten_by_an_older_dispatch(self):
        self.make_pending_branch("4.3.0-alpha.28")
        before = self.dist_pom.read_text()
        outcome = self.run_bump(self.MODULE, "4.3.0-alpha.27", pending_ref=self.PENDING_BRANCH)
        self.assertEqual(outcome, bump.SKIPPED)
        self.assertEqual(self.dist_pom.read_text(), before)
        self.assertIn(f"4.3.0-alpha.27 is not newer than 4.3.0-alpha.28 on {self.PENDING_BRANCH}", self.last_output)

    def test_newer_than_pending_bump_proceeds(self):
        self.make_pending_branch("4.3.0-alpha.28")
        outcome = self.run_bump(self.MODULE, "4.3.0-alpha.29", pending_ref=self.PENDING_BRANCH)
        self.assertEqual(outcome, bump.UPDATED)
        self.assertIn("4.3.0-alpha.29", self.dist_pom.read_text())

    def test_missing_pending_branch_falls_back_to_master(self):
        outcome = self.run_bump(self.MODULE, "4.3.0-alpha.27", pending_ref="origin/bot/bump-nope")
        self.assertEqual(outcome, bump.UPDATED)

    def test_unknown_module_fails(self):
        with self.assertRaises(SystemExit) as ctx:
            self.run_bump("gravitee-gamma-module-nope", "1.0.0")
        self.assertNotEqual(ctx.exception.code, 0)

    def test_hostile_version_input_fails_before_touching_anything(self):
        before = self.dist_pom.read_text()
        with self.assertRaises(SystemExit) as ctx:
            self.run_bump(self.MODULE, r"\1")
        self.assertNotEqual(ctx.exception.code, 0)
        self.assertEqual(self.dist_pom.read_text(), before)
        self.assertFalse(self.github_env.exists())

    def test_unparseable_current_value_is_skipped_with_a_warning(self):
        self.write_dist_pom("${aim.version}")
        before = self.dist_pom.read_text()
        outcome = self.run_bump(self.MODULE, "4.3.0-alpha.27")
        self.assertEqual(outcome, bump.SKIPPED)
        self.assertEqual(self.dist_pom.read_text(), before)
        self.assertEqual(self.github_env_lines(), ["BUMP_SKIPPED=true"])
        self.assertIn("::warning title=Bump skipped::", self.last_output)


if __name__ == "__main__":
    unittest.main()
