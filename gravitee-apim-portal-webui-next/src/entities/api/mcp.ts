/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
export interface Mcp {
  mcpPath: string;
  tools?: {
    toolDefinition: McpTool;
  }[];
}

/**
 * Interface representing an MCP (Model Context Protocol) Tool.
 *
 * This interface is used to define the structure of an MCP tool object and can
 * include optional properties for specifying the name and description of the tool.
 *
 * Each tool can extend this structure with additional properties by utilizing
 * the index signature that allows unknown key-value pairs.
 *
 * @interface
 * @extends Record<string, unknown>
 *
 * @property {string} [name] - The name of the MCP tool.
 * @property {string} [description] - A brief description of the MCP tool.
 */
export interface McpTool extends Record<string, unknown> {
  name?: string;
  description?: string;
}
