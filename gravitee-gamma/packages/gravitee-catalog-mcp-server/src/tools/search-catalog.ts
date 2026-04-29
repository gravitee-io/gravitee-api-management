import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import OpenAI from "openai";
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

export async function searchCatalog(
  args: { intent: string },
  config: Config,
): Promise<{ content: Array<{ type: "text"; text: string }> }> {
  const catalog = loadCatalog();
  const openai = new OpenAI({ apiKey: config.openaiApiKey });

  const completion = await openai.chat.completions.create({
    model: "gpt-4o-mini",
    temperature: 0,
    response_format: { type: "json_object" },
    messages: [
      {
        role: "system",
        content: `You are an API catalog routing assistant for the Gravitee platform.
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

Only return assets that are genuinely relevant. If fewer than 2 match, return fewer.`,
      },
      {
        role: "user",
        content: `## Available Assets\n\n${JSON.stringify(catalog, null, 2)}\n\n## Intent\n\n${args.intent}`,
      },
    ],
  });

  const raw = completion.choices[0]?.message?.content ?? '{"matches":[]}';
  let matches: MatchResult[];
  try {
    matches = JSON.parse(raw).matches;
  } catch {
    matches = [];
  }

  return {
    content: [
      {
        type: "text" as const,
        text: JSON.stringify(
          {
            intent: args.intent,
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
