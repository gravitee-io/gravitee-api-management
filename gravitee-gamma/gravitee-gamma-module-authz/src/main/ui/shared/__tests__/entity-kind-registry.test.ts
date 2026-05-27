/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { describe, expect, it } from 'vitest';
import { deriveServiceType, kindToUiType, uiTypeToKind } from '../entity-kind-registry';

describe('kindToUiType', () => {
    it('maps canonical kinds to camel-case UI types', () => {
        expect(kindToUiType('user')).toBe('User');
        expect(kindToUiType('group')).toBe('Group');
        expect(kindToUiType('mcp')).toBe('MCPServer');
        expect(kindToUiType('llm')).toBe('LLMRoute');
        expect(kindToUiType('agent')).toBe('AgentIdentity');
        expect(kindToUiType('api')).toBe('API');
        expect(kindToUiType('serviceaccount')).toBe('ServiceAccount');
    });

    it('honours aliases', () => {
        expect(kindToUiType('mcpserver')).toBe('MCPServer');
        expect(kindToUiType('llmroute')).toBe('LLMRoute');
        expect(kindToUiType('agentidentity')).toBe('AgentIdentity');
        expect(kindToUiType('service-account')).toBe('ServiceAccount');
        expect(kindToUiType('service_account')).toBe('ServiceAccount');
    });

    it('returns undefined for unknown kinds', () => {
        expect(kindToUiType('webhook')).toBeUndefined();
        expect(kindToUiType('')).toBeUndefined();
    });

    it('returns undefined for non-string input', () => {
        expect(kindToUiType(undefined)).toBeUndefined();
        expect(kindToUiType(null)).toBeUndefined();
        expect(kindToUiType(42)).toBeUndefined();
    });
});

describe('uiTypeToKind', () => {
    it('maps UI types to canonical lowercase kinds', () => {
        expect(uiTypeToKind('User')).toBe('user');
        expect(uiTypeToKind('MCPServer')).toBe('mcp');
        expect(uiTypeToKind('LLMRoute')).toBe('llm');
        expect(uiTypeToKind('AgentIdentity')).toBe('agent');
        expect(uiTypeToKind('ServiceAccount')).toBe('serviceaccount');
    });

    it('falls back to lowercased input for unknown types', () => {
        expect(uiTypeToKind('Webhook')).toBe('webhook');
    });
});

describe('deriveServiceType', () => {
    it('returns CUSTOM for null / undefined / empty entityId', () => {
        expect(deriveServiceType(null)).toBe('CUSTOM');
        expect(deriveServiceType(undefined)).toBe('CUSTOM');
        expect(deriveServiceType('')).toBe('CUSTOM');
    });

    it('maps known prefixes to their policy type', () => {
        expect(deriveServiceType('mcp.flight')).toBe('MCP');
        expect(deriveServiceType('agent.deploy')).toBe('AGENT');
        expect(deriveServiceType('llm.gpt')).toBe('LLM');
        expect(deriveServiceType('api.products')).toBe('API');
        expect(deriveServiceType('event.user-signup')).toBe('EVENT');
    });

    it('returns CUSTOM for kinds without a policy type (user, group, etc.)', () => {
        expect(deriveServiceType('user.alice')).toBe('CUSTOM');
        expect(deriveServiceType('group.admins')).toBe('CUSTOM');
        expect(deriveServiceType('action.read')).toBe('CUSTOM');
        expect(deriveServiceType('resource.foo')).toBe('CUSTOM');
    });

    it('returns CUSTOM for unrecognised prefixes', () => {
        expect(deriveServiceType('webhook.slack')).toBe('CUSTOM');
        expect(deriveServiceType('justsomething')).toBe('CUSTOM');
    });
});
