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

// TODO: complete the EnvironmentFlow interface when the OpenAPI is available
import { ApiType, StepV4 } from '../api';
import { ExecutionPhase } from '../plugin';

export interface EnvironmentFlow {
  id: string;
  name: string;
  apiType: ApiType;
  description?: string;
  phase: ExecutionPhase;
  policies?: StepV4[];
  updatedAt?: Date;
  deployedAt?: Date;
}
