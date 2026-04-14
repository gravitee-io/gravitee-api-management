import { ChatGoogleGenerativeAI } from "@langchain/google-genai";
import { ChatAnthropic } from "@langchain/anthropic";
import { ChatOpenAI } from "@langchain/openai";
import type { BaseChatModel } from "@langchain/core/language_models/chat_models";

export type LLMProvider = "google" | "anthropic" | "openai";

export class LLMProviderError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "LLMProviderError";
  }
}

export const detectProvider = (): LLMProvider => {
  if (process.env.GEMINI_API_KEY) return "google";
  if (process.env.ANTHROPIC_API_KEY) return "anthropic";
  if (process.env.OPENAI_API_KEY) return "openai";
  throw new LLMProviderError(
    "No LLM API key found. Set GEMINI_API_KEY, ANTHROPIC_API_KEY, or OPENAI_API_KEY",
  );
};

export const createChatModel = (): BaseChatModel => {
  const provider = detectProvider();
  switch (provider) {
    case "google":
      return new ChatGoogleGenerativeAI({ model: "gemini-2.0-flash-exp" });
    case "anthropic":
      return new ChatAnthropic({ model: "claude-3-5-sonnet-20241022" });
    case "openai":
      return new ChatOpenAI({ model: "gpt-4o" });
  }
};

export const isLLMConfigured = (): boolean => {
  return !!(
    process.env.GEMINI_API_KEY ||
    process.env.ANTHROPIC_API_KEY ||
    process.env.OPENAI_API_KEY
  );
};
