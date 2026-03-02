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

export const ZeeResourceType = {
  FLOW: 'FLOW',
  PLAN: 'PLAN',
  API: 'API',
  ENDPOINT: 'ENDPOINT',
  ENTRYPOINT: 'ENTRYPOINT',
} as const;

export type ZeeResourceType = (typeof ZeeResourceType)[keyof typeof ZeeResourceType];

/** Human-readable labels for each resource type, used in the preview header. */
export const RESOURCE_TYPE_LABELS: Record<ZeeResourceType, string> = {
  FLOW: 'Generated Flow',
  PLAN: 'Generated Plan',
  API: 'Generated API',
  ENDPOINT: 'Generated Endpoint',
  ENTRYPOINT: 'Generated Entrypoint',
};

export interface ZeeGenerateRequest {
  resourceType: ZeeResourceType;
  prompt: string;
  contextData?: Record<string, unknown>;
}

export interface ZeeGenerateResponse {
  resourceType: string;
  generated: unknown;
  metadata: {
    model: string;
    tokensUsed: number;
  };
}

