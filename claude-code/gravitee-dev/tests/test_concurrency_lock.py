#
# Copyright © 2015 The Gravitee team (http://gravitee.io)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""Tests for the flow concurrency lock."""

import json
from pathlib import Path

import pytest

from gravitee_dev.lock import FlowLockError, flow_lock


class TestFlowLock:
    """Tests for flow_lock context manager."""

    def test_lock_acquired_and_released(self, tmp_path: Path) -> None:
        """Lock file is created on enter and removed on exit."""
        lock_file = tmp_path / ".gravitee" / "gravitee.lock"

        with flow_lock(tmp_path) as path:
            assert path == lock_file
            assert lock_file.exists()
            data = json.loads(lock_file.read_text())
            assert "pid" in data
            assert "timestamp" in data

        assert not lock_file.exists()

    def test_raises_on_concurrent_lock(self, tmp_path: Path) -> None:
        """Second lock attempt raises FlowLockError."""
        lock_dir = tmp_path / ".gravitee"
        lock_dir.mkdir()
        lock_file = lock_dir / "gravitee.lock"
        lock_file.write_text(json.dumps({"pid": 99999, "timestamp": "2024-01-01T00:00:00Z"}))

        with pytest.raises(FlowLockError, match="already held"), flow_lock(tmp_path):
            pass  # pragma: no cover

    def test_released_on_exception(self, tmp_path: Path) -> None:
        """Lock is cleaned up even if body raises."""
        lock_file = tmp_path / ".gravitee" / "gravitee.lock"

        with pytest.raises(ValueError, match="boom"), flow_lock(tmp_path):
            assert lock_file.exists()
            raise ValueError("boom")

        assert not lock_file.exists()

    def test_creates_gravitee_directory(self, tmp_path: Path) -> None:
        """.gravitee directory is created if absent."""
        assert not (tmp_path / ".gravitee").exists()

        with flow_lock(tmp_path):
            assert (tmp_path / ".gravitee").is_dir()
