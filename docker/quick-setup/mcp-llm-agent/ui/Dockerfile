FROM python:3.13-slim

# Install socat
RUN apt-get update && apt-get install -y socat && apt-get clean && rm -rf /var/lib/apt/lists/*

# Install uv
RUN pip install --no-cache-dir uv

# Copy both hub and ui (from root build context)
WORKDIR /app
COPY ui/ ./ui
COPY hub/ ./hub

# Switch into the UI folder
WORKDIR /app/ui

# Setup the virtual environment and install dependencies
RUN rm -rf .venv venv \
 && uv venv \
 && uv sync --no-cache-dir

EXPOSE 12000

# Start socat in background, then run your app
CMD socat TCP-LISTEN:10000,fork TCP:hub:10000 & \
    uv run main.py --host 0.0.0.0 --port 12000
