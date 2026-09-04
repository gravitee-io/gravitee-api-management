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
import { isChattableAgent, resolveChatTarget } from './agent-chat-access';
import { fakeApi } from '../../entities/api/api.fixtures';

const anAgent = () => fakeApi({ type: 'A2A_PROXY', entrypoints: ['https://gw.test/agent'] });

describe('isChattableAgent', () => {
  it('accepts an a2a agent that publishes a gateway entrypoint', () => {
    expect(isChattableAgent(anAgent())).toBe(true);
  });

  it('refuses an api that is not an agent', () => {
    expect(isChattableAgent(fakeApi({ type: 'PROXY' }))).toBe(false);
  });

  it('refuses an agent that publishes no gateway entrypoint', () => {
    expect(isChattableAgent(fakeApi({ type: 'A2A_PROXY', entrypoints: [] }))).toBe(false);
  });

  it('refuses a missing api, which is also what a still-loading one looks like', () => {
    expect(isChattableAgent(undefined)).toBe(false);
    expect(isChattableAgent(null)).toBe(false);
  });
});

describe('resolveChatTarget', () => {
  it('points at the gateway entrypoint with the viewer own api key', () => {
    expect(resolveChatTarget(anAgent(), 'key-1')).toEqual({ endpoint: 'https://gw.test/agent', apiKey: 'key-1' });
  });

  it('yields nothing without an api key, so a non-subscriber gets no button', () => {
    expect(resolveChatTarget(anAgent(), null)).toBeNull();
    expect(resolveChatTarget(anAgent(), undefined)).toBeNull();
  });

  it('yields nothing for an api that is not a chattable agent', () => {
    expect(resolveChatTarget(fakeApi({ type: 'PROXY' }), 'key-1')).toBeNull();
    expect(resolveChatTarget(fakeApi({ type: 'A2A_PROXY', entrypoints: [] }), 'key-1')).toBeNull();
  });

  it('yields nothing while the api is still loading', () => {
    expect(resolveChatTarget(undefined, 'key-1')).toBeNull();
  });
});
