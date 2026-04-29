import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import type { Config } from "../config.js";

interface CatalogAsset {
  id: string;
  name: string;
  type: string;
  description: string;
  owner: string;
  version: string;
  tags: string[];
}

interface MatchResult {
  id: string;
  name: string;
  type: string;
  description: string;
  relevance_score: number;
  reason: string;
}

const __dirname = dirname(fileURLToPath(import.meta.url));

function loadCatalog(): CatalogAsset[] {
  const raw = readFileSync(join(__dirname, "..", "catalog.json"), "utf-8");
  return JSON.parse(raw);
}

const SYSTEM_INSTRUCTION = `You are an API catalog routing assistant for the Gravitee platform.
Given a catalog of assets and a user intent, return the top 2 assets that best match the intent.

Respond with valid JSON in this exact format:
{
  "matches": [
    {
      "id": "<asset id>",
      "name": "<asset name>",
      "type": "<asset type>",
      "description": "<asset description>",
      "relevance_score": <0.0 to 1.0>,
      "reason": "<one sentence explaining why this matches>"
    }
  ]
}

Only return assets that are genuinely relevant. If fewer than 2 match, return fewer.`;

function ollamaChatUrl(baseUrl: string): string {
  return `${baseUrl}/api/chat`;
}

export async function searchCatalog(
  args: { intent: string },
  config: Config,
): Promise<{ content: Array<{ type: "text"; text: string }> }> {
  const catalog = loadCatalog();
  const url = ollamaChatUrl(config.ollamaBaseUrl);
  const userContent = `## Available Assets\n\n${JSON.stringify(catalog, null, 2)}\n\n## Intent\n\n${args.intent}`;

  let raw: string;
  try {
    const res = await fetch(url, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        model: config.ollamaModel,
        stream: false,
        format: "json",
        messages: [
          { role: "system", content: SYSTEM_INSTRUCTION },
          { role: "user", content: userContent },
        ],
        options: { temperature: 0 },
      }),
    });

    if (!res.ok) {
      const errBody = await res.text();
      return {
        content: [
          {
            type: "text" as const,
            text: JSON.stringify(
              {
                error: "Ollama request failed",
                ollama_status: res.status,
                ollama_url: url,
                ollama_model: config.ollamaModel,
                details: errBody.slice(0, 2000),
                hint: "Is Ollama running? Run `ollama serve` and `ollama pull " + config.ollamaModel + "`.",
              },
              null,
              2,
            ),
          },
        ],
      };
    }

    const data = (await res.json()) as { message?: { content?: string } };
    raw = data.message?.content?.trim() ?? "";
  } catch (e) {
    const message = e instanceof Error ? e.message : String(e);
    return {
      content: [
        {
          type: "text" as const,
          text: JSON.stringify(
            {
              error: "Could not reach Ollama",
              ollama_url: url,
              ollama_model: config.ollamaModel,
              message,
              hint: `Start Ollama, then: ollama pull ${config.ollamaModel}`,
            },
            null,
            2,
          ),
        },
      ],
    };
  }

  let matches: MatchResult[] = [];
  if (raw) {
    try {
      const parsed = JSON.parse(raw) as { matches?: MatchResult[] };
      matches = Array.isArray(parsed.matches) ? parsed.matches : [];
    } catch {
      try {
        const unwrapped = raw.replace(/^```(?:json)?\s*/i, "").replace(/```\s*$/i, "");
        const parsed = JSON.parse(unwrapped) as { matches?: MatchResult[] };
        matches = Array.isArray(parsed.matches) ? parsed.matches : [];
      } catch {
        matches = [];
      }
    }
  }

  return {
    content: [
      {
        type: "text" as const,
        text: JSON.stringify(
          {
            intent: args.intent,
            model: config.ollamaModel,
            ollama_base_url: config.ollamaBaseUrl,
            results: matches,
            total_catalog_size: catalog.length,
          },
          null,
          2,
        ),
      },
    ],
  };
}
