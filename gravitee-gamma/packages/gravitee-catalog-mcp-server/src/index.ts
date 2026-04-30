import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  ListToolsRequestSchema,
  CallToolRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";
import { loadConfig } from "./config.js";
import { searchCatalog } from "./tools/search-catalog.js";

const config = loadConfig();

const server = new Server(
  { name: "gravitee-catalog", version: "0.2.0" },
  { capabilities: { tools: {} } },
);

server.setRequestHandler(ListToolsRequestSchema, async () => ({
  tools: [
    {
      name: "search_gravitee_catalog",
      description:
        "Search the Gravitee asset catalog using a natural language intent. " +
        "Returns matching assets (MCP proxies, LLM proxies, REST APIs, or async APIs) " +
        "from the organization's catalog via the Portal API semantic search.",
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
  ],
}));

server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;

  switch (name) {
    case "search_gravitee_catalog":
      return searchCatalog(args as { intent: string }, config);

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
