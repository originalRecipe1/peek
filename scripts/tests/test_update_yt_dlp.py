"""Exercise the real updater against an isolated release fixture."""
import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("update_yt_dlp", Path(__file__).parents[1] / "update_yt_dlp.py")
updater = importlib.util.module_from_spec(spec)
spec.loader.exec_module(updater)


class VersionUpdateTest(unittest.TestCase):
    def run_update(self, name="1.0.0", code=7, current="2026.08.19", new="2026.09.12", new_hash="b" * 64):
        root = Path(self.directory.name)
        versions, build, readme = root / "versions.toml", root / "build.gradle.kts", root / "README.md"
        versions.write_text(f'ytDlpEngine = "{current}"\n')
        build.write_text(f'val ytDlpReleaseSha256 = "{"a" * 64}"\nversionCode = {code}\nversionName = "{name}"\n')
        readme.write_text(f"Pinned extractor: {current}\n")
        with patch.multiple(updater, ROOT=root, VERSION_FILE=versions, BUILD_FILE=build, README_FILE=readme), patch("sys.argv", ["update_yt_dlp.py", new, new_hash]):
            updater.main()
        return root, build.read_text()

    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)

    def test_patch_version_is_independent_of_android_build_number(self):
        root, build = self.run_update(name="1.2.9", code=37)
        self.assertIn('versionName = "1.2.10"', build)
        self.assertIn('versionCode = 38', build)
        self.assertIn("2026.09.12", (root / "README.md").read_text())
        self.assertTrue((root / "fastlane/metadata/android/en-US/changelogs/38.txt").is_file())

    def test_first_public_release_updates_to_first_patch(self):
        _, build = self.run_update()
        self.assertIn('versionName = "1.0.1"', build)
        self.assertIn('versionCode = 8', build)

    def test_current_extractor_does_not_bump_app_version(self):
        root, build = self.run_update(new="2026.08.19", new_hash="a" * 64)
        self.assertIn('versionCode = 7', build)
        self.assertFalse((root / "fastlane").exists())

    def test_rejects_downgrade_and_immutable_hash_change(self):
        with self.assertRaises(SystemExit):
            self.run_update(new="2026.08.18")
        with self.assertRaises(SystemExit):
            self.run_update(new="2026.08.19")

    def test_does_not_guess_a_patch_for_legacy_experimental_versions(self):
        with self.assertRaises(RuntimeError):
            self.run_update(name="0.1.0-experiment.6", code=6)
        self.assertIn('versionCode = 6', (Path(self.directory.name) / "build.gradle.kts").read_text())


if __name__ == "__main__":
    unittest.main()
