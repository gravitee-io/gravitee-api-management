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

export enum ZeeResourceType {
  FLOW = 'FLOW',
  PLAN = 'PLAN',
  API = 'API',
  ENDPOINT = 'ENDPOINT',
  ENTRYPOINT = 'ENTRYPOINT',
}

export interface ZeeResourceAdapter<TGenerated = any, TSavePayload = any> {
  transform(generated: TGenerated, context?: Record<string, any>): TSavePayload;
  previewLabel: string;
}

export interface ZeeGenerateRequest {
  resourceType: ZeeResourceType;
  prompt: string;
  contextData?: Record<string, any>;
}

export interface ZeeGenerateResponse {
  resourceType: string;
  generated: any;
  metadata: {
    model: string;
    tokensUsed: number;
  };
}
