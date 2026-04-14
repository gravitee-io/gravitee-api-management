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
