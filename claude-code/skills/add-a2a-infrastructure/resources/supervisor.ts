/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
