export const agentCard = {
  name: "<AGENT_NAME>",
  description: "AI agent specializing in <DOMAIN>",
  protocolVersion: "0.3.0",
  version: "1.0.0",
  url: "http://localhost:3000/a2a/jsonrpc",
  capabilities: {
    pushNotifications: false,
    streaming: false,
    stateTransitionHistory: false,
  },
  defaultInputModes: ["text", "application/json"],
  defaultOutputModes: ["text", "application/json"],
  skills: [
    {
      id: "primary-skill",
      name: "Primary Skill",
      description: "Description of the main capability",
      tags: ["<domain>"],
      examples: ["Example request 1", "Example request 2"],
    },
  ],
  additionalInterfaces: [
    {
      url: "http://localhost:3000/a2a/jsonrpc",
      transport: "JSONRPC",
    },
  ],
};
