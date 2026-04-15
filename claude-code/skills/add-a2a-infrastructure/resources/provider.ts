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
