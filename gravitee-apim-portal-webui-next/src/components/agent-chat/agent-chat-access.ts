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
import { Api } from '../../entities/api/api';

export type ChatAccess = 'loading' | 'granted' | 'not-eligible';

export interface ChatEligibility {
  api: Api | null | undefined;
  apiLoading: boolean;
  apiKey: string | null | undefined;
  subscriptionLoading: boolean;
}

// The chat is offered only where it can work: an a2a agent reachable through the gateway,
// with an api key from the viewer's own subscription to authenticate with.
export function chatAccess({ api, apiLoading, apiKey, subscriptionLoading }: ChatEligibility): ChatAccess {
  if (apiLoading) {
    return 'loading';
  }
  if (!api || api.type !== 'A2A_PROXY' || !api.entrypoints?.[0]) {
    return 'not-eligible';
  }
  if (subscriptionLoading) {
    return 'loading';
  }
  return apiKey ? 'granted' : 'not-eligible';
}
