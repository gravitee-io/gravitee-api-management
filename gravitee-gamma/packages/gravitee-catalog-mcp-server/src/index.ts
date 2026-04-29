import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  ListToolsRequestSchema,
  CallToolRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";
import { loadConfig } from "./config.js";
import { searchCatalog } from "./tools/search-catalog.js";
import { requestSubscription } from "./tools/request-subscription.js";

const config = loadConfig();

const server = new Server(
  { name: "gravitee-catalog", version: "0.1.0" },
  { capabilities: { tools: {} } },
);

server.setRequestHandler(ListToolsRequestSchema, async () => ({
  tools: [
    {
      name: "search_gravitee_catalog",
      description:
        "Search the Gravitee asset catalog using a natural language intent. " +
        "Returns the top 2 matching assets (MCP proxies, LLM proxies, REST APIs, or async APIs) " +
        "from the organization's catalog. Use this to discover available services before requesting access.",
      inputSchema: {
        type: "object" as const,
        properties: {
          intent: {
            type: "string",
            description:
              "Natural language description of what you need, e.g. 'I need to process credit card payments' or 'I want to send notifications to a Slack channel'",
          },
        },
        required: ["intent"],
      },
    },
    {
      name: "request_asset_subscription",
      description:
        "Request access to a Gravitee asset by its ID. " +
        "This emits a subscription request to Gravitee Access Management, " +
        "which requires human approval by the asset owner before access is granted. " +
        "Use search_gravitee_catalog first to find valid asset IDs.",
      inputSchema: {
        type: "object" as const,
        properties: {
          asset_id: {
            type: "string",
            description:
              "The unique ID of the asset to request access to (from search results)",
          },
          justification: {
            type: "string",
            description:
              "Business justification for why access is needed. This will be reviewed by the asset owner.",
          },
        },
        required: ["asset_id", "justification"],
      },
    },
  ],
}));

server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;

  switch (name) {
    case "search_gravitee_catalog":
      return searchCatalog(args as { intent: string }, config);

    case "request_asset_subscription":
      return requestSubscription(
        args as { asset_id: string; justification: string },
        config,
      );

    default:
      return {
        content: [
          {
            type: "text" as const,
            text: `Unknown tool: ${name}`,
          },
        ],
        isError: true,
      };
  }
});

const transport = new StdioServerTransport();
await server.connect(transport);
