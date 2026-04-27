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
export interface TracingNode {
  id: string;
  type: 'agent' | 'mcp_server' | 'llm' | 'api' | 'topic' | 'rag';
  label: string;
  subtitle?: string;
  status?: 'healthy' | 'error';
  badge?: string;
  icon?: string;
  metadata?: Record<string, unknown>;
}

export interface EdgeSpan {
  operation: string;
  durationNanos: number;
  status: string;
  tool?: string;
  tokens?: number;
}

export interface TracingEdge {
  from: string;
  to: string;
  spans: EdgeSpan[];
}

export interface TracingGraph {
  traceId: string;
  durationNanos: number;
  nodes: TracingNode[];
  edges: TracingEdge[];
}

export interface TraceEvent {
  name: string;
  time: string;
  attributes: Record<string, string>;
}

export interface TraceSpan {
  traceId: string;
  spanId: string;
  parentSpanId: string | null;
  operationName: string;
  serviceName: string;
  startTime: string;
  durationNanos: number;
  attributes: Record<string, string>;
  events: TraceEvent[];
  children: TraceSpan[];
}

export interface Trace {
  traceId: string;
  startTime: string;
  durationNanos: number;
  rootService: string;
  rootOperation: string;
  hasError?: boolean;
  spans?: TraceSpan[];
}
