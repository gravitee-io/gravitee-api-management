export type AgentInput = {
  instructions: string;
  documents?: Array<{ type: string; content: string; filename?: string }>;
};

export type AgentOutput = {
  result: string;
  metadata: Record<string, unknown>;
};

export type Message = {
  role: "user" | "assistant" | "system";
  content: string;
};
