/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
export interface EntityTypeColor {
  background: string;
  foreground: string;
  icon: string;
  label: string;
}

export const ENTITY_TYPE_COLORS: Record<string, EntityTypeColor> = {
  agent: { background: '#009999', foreground: '#ffffff', icon: 'smart_toy', label: 'Agent' },
  llm: { background: '#876fec', foreground: '#ffffff', icon: 'psychology', label: 'LLM' },
  mcp_server: { background: '#494b61', foreground: '#ffffff', icon: 'dns', label: 'MCP Server' },
  api: { background: '#006fb9', foreground: '#ffffff', icon: 'api', label: 'API' },
  topic: { background: '#fe733f', foreground: '#ffffff', icon: 'topic', label: 'Topic' },
  rag: { background: '#0de598', foreground: '#1c1e39', icon: 'search', label: 'RAG' },
};
