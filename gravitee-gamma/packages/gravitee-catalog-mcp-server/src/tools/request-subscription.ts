import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { randomUUID } from "node:crypto";
import type { Config } from "../config.js";

interface CatalogAsset {
  id: string;
  name: string;
  type: string;
  description: string;
  owner: string;
}

const __dirname = dirname(fileURLToPath(import.meta.url));

function loadCatalog(): CatalogAsset[] {
  const raw = readFileSync(join(__dirname, "..", "catalog.json"), "utf-8");
  return JSON.parse(raw);
}

export async function requestSubscription(
  args: { asset_id: string; justification: string },
  config: Config,
): Promise<{ content: Array<{ type: "text"; text: string }> }> {
  const catalog = loadCatalog();
  const asset = catalog.find((a) => a.id === args.asset_id);

  if (!asset) {
    return {
      content: [
        {
          type: "text" as const,
          text: JSON.stringify(
            {
              status: "404 Not Found",
              error: `No asset found with id "${args.asset_id}". Use search_gravitee_catalog to find valid asset IDs.`,
            },
            null,
            2,
          ),
        },
      ],
    };
  }

  const requestId = randomUUID();

  return {
    content: [
      {
        type: "text" as const,
        text: JSON.stringify(
          {
            status: "202 Accepted",
            request_id: requestId,
            asset: {
              id: asset.id,
              name: asset.name,
              type: asset.type,
            },
            justification: args.justification,
            message:
              `Subscription request ${requestId} has been emitted to Gravitee Access Management. ` +
              `The application owner (${asset.owner}) will review your justification and approve or deny access. ` +
              `You will be notified when a decision is made. ` +
              `In the meantime, you cannot use this asset.`,
            gravitee_console_url: `${config.graviteeGammaBaseUrl}/access-requests/${requestId}`,
          },
          null,
          2,
        ),
      },
    ],
  };
}
