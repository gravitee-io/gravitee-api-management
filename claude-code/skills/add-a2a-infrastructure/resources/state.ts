import { Annotation } from "@langchain/langgraph";
import type { Message, AgentOutput } from "../types/agent.js";

export const AgentStateAnnotation = Annotation.Root({
  messages: Annotation<Message[]>({
    reducer: (prev, next) => next,
    default: () => [],
  }),
  nextStep: Annotation<string | undefined>({
    reducer: (prev, next) => next || prev,
  }),
  result: Annotation<AgentOutput | undefined>({
    reducer: (prev, next) => next || prev,
  }),
  error: Annotation<string | null>({
    reducer: (prev, next) => next,
    default: () => null,
  }),
});

export type AgentState = typeof AgentStateAnnotation.State;
