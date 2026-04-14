import { createChatModel } from "../lib/llm/provider.js";
import type { AgentInput } from "../types/agent.js";

const SYSTEM_PROMPT = `You are <AGENT_NAME>, a supervisor agent that routes tasks to specialized sub-agents.

Available skills:
- SKILL_1: <Description of skill 1>
- SKILL_2: <Description of skill 2>

Analyze the user's request and determine which skill to invoke.
Respond with JSON: { "skill": "SKILL_1" | "SKILL_2", "reasoning": "..." }`;

export const planTask = async (input: AgentInput) => {
  const model = createChatModel();

  const response = await model.invoke([
    { role: "system", content: SYSTEM_PROMPT },
    { role: "user", content: input.instructions },
  ]);

  // Parse response (use structured output in production)
  const content = response.content as string;

  return {
    nextStep: "SKILL_1" as const,
    reasoning: content,
    parameters: {},
  };
};
