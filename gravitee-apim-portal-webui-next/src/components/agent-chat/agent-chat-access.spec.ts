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
import { chatAccess, ChatEligibility } from './agent-chat-access';
import { Api } from '../../entities/api/api';

const anAgent = (overrides: Partial<Api> = {}): Api =>
  ({
    id: 'agent-1',
    name: 'Incident Commander',
    version: '1',
    description: '',
    definitionVersion: 'V4',
    type: 'A2A_PROXY',
    entrypoints: ['https://gw.test/agent'],
    ...overrides,
  }) as Api;

const eligibility = (overrides: Partial<ChatEligibility> = {}): ChatEligibility => ({
  api: anAgent(),
  apiLoading: false,
  apiKey: 'key-1',
  subscriptionLoading: false,
  ...overrides,
});

describe('chatAccess', () => {
  it('grants access to a subscriber of an a2a agent that has a gateway entrypoint', () => {
    expect(chatAccess(eligibility())).toBe('granted');
  });

  it('waits while the api is still loading', () => {
    expect(chatAccess(eligibility({ api: undefined, apiLoading: true }))).toBe('loading');
  });

  it('waits while the subscription is still loading', () => {
    expect(chatAccess(eligibility({ apiKey: null, subscriptionLoading: true }))).toBe('loading');
  });

  it('refuses a non-agent api without waiting for a subscription', () => {
    expect(chatAccess(eligibility({ api: anAgent({ type: 'PROXY' }), subscriptionLoading: true }))).toBe('not-eligible');
  });

  it('refuses an agent that publishes no gateway entrypoint', () => {
    expect(chatAccess(eligibility({ api: anAgent({ entrypoints: [] }) }))).toBe('not-eligible');
  });

  it('refuses a viewer with no api key', () => {
    expect(chatAccess(eligibility({ apiKey: null }))).toBe('not-eligible');
  });

  it('refuses when there is no api at all', () => {
    expect(chatAccess(eligibility({ api: null }))).toBe('not-eligible');
  });
});
