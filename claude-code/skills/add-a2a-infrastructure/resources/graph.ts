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
import { StateGraph, END } from "@langchain/langgraph";
import { AgentStateAnnotation, type AgentState } from "./state.js";
import { planTask } from "../agents/supervisor.js";
import { executeSkill } from "../agents/worker.js";
import type { AgentInput, AgentOutput } from "../types/agent.js";

const supervisorNode = async (
  state: AgentState,
): Promise<Partial<AgentState>> => {
  console.log("[Supervisor] Planning task...");

  const lastMessage = state.messages[state.messages.length - 1];
  const plan = await planTask({ instructions: lastMessage?.content || "" });

  return {
    nextStep: plan.nextStep,
    messages: [
      ...state.messages,
      { role: "assistant", content: plan.reasoning },
    ],
  };
};

const workerNode = async (state: AgentState): Promise<Partial<AgentState>> => {
  console.log("[Worker] Executing skill...");

  const firstMessage = state.messages[0];
  const result = await executeSkill({
    instructions: firstMessage?.content || "",
  });

  return {
    result,
    messages: [
      ...state.messages,
      { role: "assistant", content: result.result },
    ],
  };
};

const routeAfterSupervisor = (state: AgentState): string => {
  return state.nextStep === "SKILL_1" ? "worker" : END;
};

export const createAgentGraph = () => {
  const workflow = new StateGraph(AgentStateAnnotation)
    .addNode("supervisor", supervisorNode)
    .addNode("worker", workerNode)
    .addEdge("__start__", "supervisor")
    .addConditionalEdges("supervisor", routeAfterSupervisor)
    .addEdge("worker", END);

  return workflow.compile();
};

export const executeGraph = async (input: AgentInput): Promise<AgentOutput> => {
  const graph = createAgentGraph();

  const result = await graph.invoke({
    messages: [{ role: "user", content: input.instructions }],
  });

  return result.result || { result: "Task completed", metadata: {} };
};
