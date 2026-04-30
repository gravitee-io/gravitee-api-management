import type { Config } from "../config.js";

interface CatalogSearchResult {
  id: string;
  title: string;
  description: string;
  type: string;
  owner: string;
  tags: string[];
  paths: string[];
  entrypointTypes: string[];
  endpointTypes: string[];
  categories: string[];
  listenerTypes: string[];
  score: number;
}

interface CatalogSearchResponse {
  mode: string;
  totalResults: number;
  results: CatalogSearchResult[];
}

export async function searchCatalog(
  args: { intent: string },
  config: Config,
): Promise<{ content: Array<{ type: "text"; text: string }> }> {
  const params = new URLSearchParams({ intent: args.intent, mode: "semantic" });
  const url = `${config.portalApiBaseUrl}/catalog/search?${params}`;

  try {
    const res = await fetch(url, {
      method: "GET",
      headers: {
        Accept: "application/json",
        Authorization: `Basic ${btoa(`${config.portalApiUsername}:${config.portalApiPassword}`)}`,
      },
    });

    if (!res.ok) {
      const errBody = await res.text();
      return {
        content: [
          {
            type: "text" as const,
            text: JSON.stringify(
              {
                error: "Portal API request failed",
                status: res.status,
                url,
                details: errBody.slice(0, 2000),
              },
              null,
              2,
            ),
          },
        ],
      };
    }

    const data = (await res.json()) as CatalogSearchResponse;

    return {
      content: [
        {
          type: "text" as const,
          text: JSON.stringify(
            {
              intent: args.intent,
              mode: data.mode,
              total_results: data.totalResults,
              results: data.results,
            },
            null,
            2,
          ),
        },
      ],
    };
  } catch (e) {
    const message = e instanceof Error ? e.message : String(e);
    return {
      content: [
        {
          type: "text" as const,
          text: JSON.stringify(
            {
              error: "Could not reach Portal API",
              url,
              message,
            },
            null,
            2,
          ),
        },
      ],
    };
  }
}
