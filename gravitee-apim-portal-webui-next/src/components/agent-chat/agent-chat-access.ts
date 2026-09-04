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
import { ChatTarget } from './agent-chat.store';
import { Api } from '../../entities/api/api';

export function isChattableAgent(api: Api | null | undefined): boolean {
  return api?.type === 'A2A_PROXY' && !!api.entrypoints?.[0];
}

/**
 * Where to send a question, or null when the viewer may not chat. A still-loading api or
 * subscription yields null too, which is what keeps the button hidden until both are known.
 */
export function resolveChatTarget(api: Api | null | undefined, apiKey: string | null | undefined): ChatTarget | null {
  const endpoint = api?.entrypoints?.[0];
  return isChattableAgent(api) && endpoint && apiKey ? { endpoint, apiKey } : null;
}
