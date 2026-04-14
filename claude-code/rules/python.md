# Python Style & Structure

## Core Principles

- **Version**: Target Python 3.12+.
- **Package Manager**: Use `uv` exclusively — never use `pip`, `pip install`, or `python -m pip`.
- **Type Hints**: ALL functions must have complete type annotations.

## Styling

- **Format**: use `ruff fmt`.
- **Docstrings**: Use Google-style docstrings for all public functions.

## Scripts & Dependencies

- **Running scripts**: Always use `uv run <script.py>` instead of `python <script.py>`.
- **Inline dependencies**: For standalone scripts, declare dependencies with [PEP 723](https://peps.python.org/pep-0723/) inline metadata so `uv run` resolves them automatically:
  ```python
  # /// script
  # requires-python = ">=3.12"
  # dependencies = [
  #     "typer>=0.15",
  #     "httpx>=0.28",
  # ]
  # ///
  ```
- **Installing packages**: Use `uv add <package>` (project) or `uv pip install` (virtual env) — never bare `pip install`.
- **Virtual envs**: Let `uv` manage the venv. Do not create or activate venvs manually.
- **CLI scripts**: Use `typer` with descriptive help text.
- **Web**: FastAPI for REST APIs, with Pydantic models for request/response.
- **ML/Training**:
  - `tqdm` for progress bars
  - `ThreadPoolExecutor` for parallel API calls
  - Incremental writes to prevent data loss

## Error Handling

- **Retries**: Use exponential backoff for API calls (e.g., `2 ** retry_count` seconds).
- **Graceful Fallback**: When compression/transformation fails, fall back to original value rather than crashing.
