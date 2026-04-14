import { Router, Request, Response } from "express";
import { v4 as uuidv4 } from "uuid";
import { executeGraph } from "../graph/index.js";
import { agentCard } from "./agent-card.js";
import type { AgentInput } from "../types/agent.js";

type Task = {
  id: string;
  status: "pending" | "running" | "completed" | "failed";
  input: AgentInput;
  result?: unknown;
  error?: string;
};

const tasks = new Map<string, Task>();

export const createA2ARouter = (): Router => {
  const router = Router();

  // Agent info
  router.get("/agent", (req: Request, res: Response) => {
    res.json(agentCard);
  });

  // Create task
  router.post("/tasks", async (req: Request, res: Response) => {
    const { instructions, documents } = req.body;
    const taskId = uuidv4();

    const task: Task = {
      id: taskId,
      status: "pending",
      input: { instructions, documents },
    };
    tasks.set(taskId, task);

    res.status(201).json({ taskId, status: task.status });

    // Execute async
    task.status = "running";
    try {
      const result = await executeGraph(task.input);
      task.status = "completed";
      task.result = result;
    } catch (error) {
      task.status = "failed";
      task.error = error instanceof Error ? error.message : "Unknown error";
    }
  });

  // Get task
  router.get("/tasks/:taskId", (req: Request, res: Response) => {
    const task = tasks.get(req.params.taskId);
    if (!task) {
      return res.status(404).json({ error: "Task not found" });
    }
    res.json(task);
  });

  // List tasks
  router.get("/tasks", (req: Request, res: Response) => {
    res.json(Array.from(tasks.values()));
  });

  return router;
};
