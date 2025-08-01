# Use a slim Python base
FROM python:3.13-slim

# Install uv for dependency management and running
RUN pip install --no-cache-dir uv

# Set working directory
WORKDIR /app

# Copy project files
COPY . .

# Remove any host-created venv so uv can recreate cleanly
RUN rm -rf .venv venv

# Pre-install all dependencies via uv (will read pyproject.toml in each subfolder)
# (optional – speeds up subsequent runs)
RUN uv venv \
 && uv pip install --no-cache-dir .

# Default to the langgraph agent; you can override at runtime
WORKDIR /app/agents/langgraph

CMD ["uv", "run", ".", "--host", "0.0.0.0", "--port", "10000"]
