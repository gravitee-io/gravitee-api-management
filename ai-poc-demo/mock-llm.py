#!/usr/bin/env python3
"""OpenAI-compatible mock upstream for the AI Products PoC.

Returns configurable token usage so token-budget (429) demos are predictable.

Token profiles (env MOCK_TOKEN_PROFILE):
  fast   —  5 prompt + 10 completion = 15/call  (100 budget → ~7 calls)
  normal — 40 prompt + 60 completion = 100/call (250 budget → ~3 calls)

Usage:
  python3 mock-llm.py [port]
  MOCK_TOKEN_PROFILE=fast python3 mock-llm.py 9099

Ollama (optional, real local inference):
  docker compose --profile ollama up -d ollama
  # OpenAI-compat API: http://ollama:11434/v1  model e.g. qwen2.5:0.5b
"""
import json
import os
import sys
from http.server import BaseHTTPRequestHandler, HTTPServer

PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 9099

PROFILES = {
    "fast": (5, 10),
    "normal": (40, 60),
}

profile = os.environ.get("MOCK_TOKEN_PROFILE", "fast").strip().lower()
if profile not in PROFILES:
    profile = "fast"
PROMPT_TOKENS, COMPLETION_TOKENS = PROFILES[profile]
if os.environ.get("MOCK_PROMPT_TOKENS"):
    PROMPT_TOKENS = int(os.environ["MOCK_PROMPT_TOKENS"])
if os.environ.get("MOCK_COMPLETION_TOKENS"):
    COMPLETION_TOKENS = int(os.environ["MOCK_COMPLETION_TOKENS"])
TOTAL_TOKENS = PROMPT_TOKENS + COMPLETION_TOKENS

MODEL = "gpt-5.4-mini"

# Advertised catalog — includes OpenAI, Qwen (Together/Fireworks ids), and Ollama-style names.
# Chat completions accept ANY model id (gateway governance uses catalog query names).
MODELS = [
    # OpenAI (catalog)
    "gpt-5.4-mini", "gpt-5.4", "gpt-4o-mini",
    # Together / Qwen (catalog import ids)
    "Qwen/Qwen3.6-Plus", "Qwen/Qwen3.7-Max", "Qwen/Qwen3-Coder-Next-FP8",
    # Fireworks / Qwen (catalog import ids)
    "accounts/fireworks/models/qwen3p6-plus", "accounts/fireworks/models/qwen3p7-plus",
    # Ollama-style names (local / mock)
    "qwen2.5:0.5b", "qwen2.5:1.5b", "llama3.2:1b", "mistral:7b",
    # Legacy create-llm-proxies.sh labels
    "gpt-4o", "o3-mini", "claude-sonnet-4-5", "gemini-2.5-flash",
]


def reply_for(model: str) -> str:
    if "qwen" in model.lower() or model.startswith("Qwen"):
        return f"[mock/Qwen] Answer from {model} via Gravitee AI Product gateway."
    if "llama" in model.lower():
        return f"[mock/Ollama] Answer from {model} via Gravitee AI Product gateway."
    return f"[mock] Answer from {model} via Gravitee AI Product gateway."


class Handler(BaseHTTPRequestHandler):
    def _send(self, code, payload):
        body = json.dumps(payload).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path.rstrip("/").endswith("/models"):
            data = [{"id": m, "object": "model", "owned_by": "mock"} for m in MODELS]
            self._send(200, {"object": "list", "data": data})
        else:
            self._send(404, {"error": "not found"})

    def do_POST(self):
        length = int(self.headers.get("Content-Length", 0))
        try:
            request = json.loads(self.rfile.read(length) or b"{}")
        except json.JSONDecodeError:
            request = {}
        model = request.get("model", MODEL)
        self._send(
            200,
            {
                "id": "chatcmpl-mock-1",
                "object": "chat.completion",
                "created": 1750000000,
                "model": model,
                "choices": [
                    {
                        "index": 0,
                        "message": {"role": "assistant", "content": reply_for(model)},
                        "finish_reason": "stop",
                    }
                ],
                "usage": {
                    "prompt_tokens": PROMPT_TOKENS,
                    "completion_tokens": COMPLETION_TOKENS,
                    "total_tokens": TOTAL_TOKENS,
                },
            },
        )

    def log_message(self, fmt, *args):
        sys.stderr.write("mock-llm: %s\n" % (fmt % args))


class ReusableHTTPServer(HTTPServer):
    allow_reuse_address = True


if __name__ == "__main__":
    print(
        f"mock-llm :{PORT} profile={profile} tokens={PROMPT_TOKENS}+{COMPLETION_TOKENS}={TOTAL_TOKENS}/call "
        f"({len(MODELS)} models)"
    )
    ReusableHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
