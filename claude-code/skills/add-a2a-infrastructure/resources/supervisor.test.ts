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
