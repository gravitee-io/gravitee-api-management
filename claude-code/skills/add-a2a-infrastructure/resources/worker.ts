import { createChatModel } from "../lib/llm/provider.js";
import type { AgentInput, AgentOutput } from "../types/agent.js";

const SYSTEM_PROMPT = `You are a specialized worker agent for <SKILL_DESCRIPTION>.
Complete the user's task and provide a clear response.`;

export const executeSkill = async (input: AgentInput): Promise<AgentOutput> => {
  const model = createChatModel();

  const response = await model.invoke([
    { role: "system", content: SYSTEM_PROMPT },
    { role: "user", content: input.instructions },
  ]);

  return {
    result: response.content as string,
    metadata: {},
  };
};
