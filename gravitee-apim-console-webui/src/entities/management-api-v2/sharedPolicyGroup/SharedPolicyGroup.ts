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

import { ApiType, StepV4 } from '../api';
import { ExecutionPhase } from '../plugin';

export interface SharedPolicyGroup {
  id: string;
  crossId: string;
  name: string;
  description?: string;
  prerequisiteMessage?: string;
  lifecycleState?: 'DEPLOYED' | 'UNDEPLOYED' | 'PENDING';
  version?: string;
  apiType: ApiType;
  phase: ExecutionPhase;
  steps?: StepV4[];
  deployedAt?: Date;
  createdAt?: Date;
  updatedAt?: Date;
}
