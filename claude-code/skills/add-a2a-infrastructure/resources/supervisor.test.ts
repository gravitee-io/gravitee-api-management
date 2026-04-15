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
import { describe, it, expect, vi } from "vitest";

// Mock the LLM provider for testing
vi.mock("../src/lib/llm/provider", () => ({
  createChatModel: () => ({
    invoke: vi.fn().mockResolvedValue({
      content: JSON.stringify({
        skill: "SKILL_1",
        reasoning: "Test reasoning",
      }),
    }),
  }),
}));

describe("Supervisor Agent", () => {
  it("should plan a task", async () => {
    // Import after mocking
    const { planTask } = await import("../src/agents/supervisor");

    const result = await planTask({ instructions: "Test task" });

    expect(result).toHaveProperty("nextStep");
    expect(result).toHaveProperty("reasoning");
  });
});
