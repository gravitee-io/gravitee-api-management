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
import express from "express";
import cors from "cors";
import bodyParser from "body-parser";
import * as dotenv from "dotenv";
import { createA2ARouter } from "./a2a/protocol.js";

dotenv.config();

const app = express();
const port = process.env.PORT || 3000;

app.use(cors());
app.use(bodyParser.json({ limit: "10mb" }));

// Serve agent card
app.use(express.static("public", { dotfiles: "allow" }));

// Health check
app.get("/health", (req, res) => {
  res.json({
    status: "ok",
    agent: "<AGENT_NAME>",
    timestamp: new Date().toISOString(),
  });
});

// Mount A2A routes
app.use("/", createA2ARouter());

// 404 handler
app.use((req, res) => {
  res.status(404).json({ error: "Not Found", path: req.path });
});

app.listen(port, () => {
  console.log(`
🤖 <AGENT_NAME> Agent running on port ${port}
   Agent Card: http://localhost:${port}/.well-known/agent.json
   Health: http://localhost:${port}/health
   Tasks: POST http://localhost:${port}/tasks
  `);
});

export default app;
