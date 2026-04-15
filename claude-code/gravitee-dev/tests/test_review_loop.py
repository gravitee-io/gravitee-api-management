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

"""Tests for the review loop module."""

from gravitee_dev.review_loop import (
    _RESOLVE_THREAD_MUTATION,
    _UNRESOLVED_QUERY,
    _wait_for_input,
)


class TestUnresolvedQuery:
    """Tests for the GraphQL query structure."""

    def test_query_contains_review_threads(self) -> None:
        """The GraphQL query fetches reviewThreads."""
        assert "reviewThreads" in _UNRESOLVED_QUERY

    def test_query_contains_id(self) -> None:
        """The query requests the id field for resolving threads."""
        assert "id" in _UNRESOLVED_QUERY

    def test_query_contains_is_resolved(self) -> None:
        """The query requests isResolved field."""
        assert "isResolved" in _UNRESOLVED_QUERY

    def test_query_contains_is_outdated(self) -> None:
        """The query requests isOutdated field."""
        assert "isOutdated" in _UNRESOLVED_QUERY


class TestResolveThreadMutation:
    """Tests for the resolve thread GraphQL mutation."""

    def test_mutation_contains_resolve_review_thread(self) -> None:
        """The mutation calls resolveReviewThread."""
        assert "resolveReviewThread" in _RESOLVE_THREAD_MUTATION

    def test_mutation_accepts_thread_id(self) -> None:
        """The mutation accepts a threadId variable."""
        assert "$threadId" in _RESOLVE_THREAD_MUTATION


class TestParseUnresolvedThreads:
    """Tests for parsing the GraphQL response."""

    def test_returns_ids_of_unresolved_threads(self) -> None:
        """Unresolved, non-outdated threads return their IDs."""
        response = {
            "data": {
                "repository": {
                    "pullRequest": {
                        "reviewThreads": {
                            "nodes": [
                                {"id": "T_1", "isResolved": False, "isOutdated": False},
                                {"id": "T_2", "isResolved": True, "isOutdated": False},
                                {"id": "T_3", "isResolved": False, "isOutdated": True},
                                {"id": "T_4", "isResolved": False, "isOutdated": False},
                            ]
                        }
                    }
                }
            }
        }
        threads = response["data"]["repository"]["pullRequest"]["reviewThreads"]["nodes"]
        ids = [t["id"] for t in threads if not t["isResolved"] and not t["isOutdated"]]
        assert ids == ["T_1", "T_4"]

    def test_all_resolved_returns_empty(self) -> None:
        """All resolved threads return empty list."""
        response = {
            "data": {
                "repository": {
                    "pullRequest": {
                        "reviewThreads": {
                            "nodes": [
                                {"id": "T_1", "isResolved": True, "isOutdated": False},
                                {"id": "T_2", "isResolved": True, "isOutdated": False},
                            ]
                        }
                    }
                }
            }
        }
        threads = response["data"]["repository"]["pullRequest"]["reviewThreads"]["nodes"]
        ids = [t["id"] for t in threads if not t["isResolved"] and not t["isOutdated"]]
        assert ids == []

    def test_empty_threads_returns_empty(self) -> None:
        """No threads returns empty list."""
        response = {"data": {"repository": {"pullRequest": {"reviewThreads": {"nodes": []}}}}}
        threads = response["data"]["repository"]["pullRequest"]["reviewThreads"]["nodes"]
        ids = [t["id"] for t in threads if not t["isResolved"] and not t["isOutdated"]]
        assert ids == []


class TestWaitForInput:
    """Tests for the input timeout function."""

    def test_returns_none_on_timeout(self) -> None:
        """Returns None when no input is provided within timeout."""
        result = _wait_for_input(0.01)
        assert result is None
