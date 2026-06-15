#!/usr/bin/env python3
"""Tiny OpenAI-compatible mock upstream for the AI Products PoC.

Implements POST /v1/chat/completions (returns a canned answer with token usage —
~40 input + 60 output tokens per call, so a 300-token budget exhausts in ~3 calls)
and GET /v1/models. Run:  python3 ai-poc-demo/mock-llm.py [port]
"""
import json
import sys
from http.server import BaseHTTPRequestHandler, HTTPServer

PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 9099

# Default model echoed back when a request doesn't specify one.
MODEL = "gpt-4o-mini"

# Full catalog advertised by GET /v1/models. Mirrors the model names created by
# create-llm-proxies.sh so the mock's catalog matches the six LLM proxies.
# The mock still answers chat completions for ANY model name (see do_POST).
MODELS = [
    # OpenAI
    "gpt-4o-mini", "gpt-4o", "o3-mini",
    # Anthropic
    "claude-sonnet-4-5", "claude-opus-4-1", "claude-haiku-4-5",
    # Mistral
    "mistral-large-latest", "mistral-small-latest",
    # Gemini
    "gemini-2.5-pro", "gemini-2.5-flash",
    # Llama
    "llama-3.3-70b", "llama-3.1-8b",
    # Cohere
    "command-r-plus", "command-r",
]


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
                        "message": {"role": "assistant", "content": "Hello from the mock LLM behind the Gravitee gateway!"},
                        "finish_reason": "stop",
                    }
                ],
                "usage": {"prompt_tokens": 40, "completion_tokens": 60, "total_tokens": 100},
            },
        )

    def log_message(self, fmt, *args):
        sys.stderr.write("mock-llm: %s\n" % (fmt % args))


class ReusableHTTPServer(HTTPServer):
    # Avoid "Address already in use" on quick restarts.
    allow_reuse_address = True


if __name__ == "__main__":
    print(f"mock-llm listening on :{PORT} ({len(MODELS)} models, default {MODEL})")
    ReusableHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
