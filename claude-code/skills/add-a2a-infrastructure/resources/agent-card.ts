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
export const agentCard = {
  name: "<AGENT_NAME>",
  description: "AI agent specializing in <DOMAIN>",
  protocolVersion: "0.3.0",
  version: "1.0.0",
  url: "http://localhost:3000/a2a/jsonrpc",
  capabilities: {
    pushNotifications: false,
    streaming: false,
    stateTransitionHistory: false,
  },
  defaultInputModes: ["text", "application/json"],
  defaultOutputModes: ["text", "application/json"],
  skills: [
    {
      id: "primary-skill",
      name: "Primary Skill",
      description: "Description of the main capability",
      tags: ["<domain>"],
      examples: ["Example request 1", "Example request 2"],
    },
  ],
  additionalInterfaces: [
    {
      url: "http://localhost:3000/a2a/jsonrpc",
      transport: "JSONRPC",
    },
  ],
};
