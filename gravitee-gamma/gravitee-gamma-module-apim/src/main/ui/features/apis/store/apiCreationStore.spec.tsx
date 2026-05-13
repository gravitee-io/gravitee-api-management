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
import { act, renderHook } from '@testing-library/react';
import type { ReactNode } from 'react';

import { ApiCreationProvider, useApiCreation } from './apiCreationStore';
import type { ProxyTemplate } from '../types/apiCreation';

// ─── Shared test data ─────────────────────────────────────────────────────────

const MOCK_TEMPLATE: ProxyTemplate = {
    id: 'tpl-api-key',
    title: 'API Key Template',
    subtitle: 'Subtitle',
    description: 'Description',
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    Icon: (() => null) as any,
    color: 'blue',
    tags: ['api-key'],
    defaults: {
        authType: 'api-key',
        apiKeyPlanName: 'Template API Key Plan',
    },
};

// ─── Wrapper helpers ──────────────────────────────────────────────────────────

function defaultWrapper({ children }: { children: ReactNode }) {
    return <ApiCreationProvider>{children}</ApiCreationProvider>;
}

function templateWrapper({ children }: { children: ReactNode }) {
    return <ApiCreationProvider initialTemplate={MOCK_TEMPLATE}>{children}</ApiCreationProvider>;
}

function modeWrapper({ children }: { children: ReactNode }) {
    return <ApiCreationProvider initialMode="scratch">{children}</ApiCreationProvider>;
}

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('useApiCreation — guard', () => {
    it('throws when used outside ApiCreationProvider', () => {
        const spy = jest.spyOn(console, 'error').mockImplementation(() => {});
        expect(() => renderHook(() => useApiCreation())).toThrow('useApiCreation must be used inside ApiCreationProvider');
        spy.mockRestore();
    });
});

describe('ApiCreationProvider — initial state', () => {
    it('initialises with picker mode, step 0, and an empty form', () => {
        const { result } = renderHook(() => useApiCreation(), { wrapper: defaultWrapper });
        const { state } = result.current;

        expect(state.creationMode).toBe('picker');
        expect(state.step).toBe(0);
        expect(state.form.apiName).toBe('');
        expect(state.form.authType).toBe('keyless');
        expect(state.form.deployImmediately).toBe(true);
        expect(state.validationErrors).toEqual({});
    });

    it('applies template defaults and sets template mode when initialTemplate is provided', () => {
        const { result } = renderHook(() => useApiCreation(), { wrapper: templateWrapper });
        const { state } = result.current;

        expect(state.creationMode).toBe('template');
        expect(state.selectedTemplate?.id).toBe('tpl-api-key');
        expect(state.form.authType).toBe('api-key');
        expect(state.form.apiKeyPlanName).toBe('Template API Key Plan');
        expect(state.step).toBe(0);
    });

    it('uses the given mode when initialMode is provided', () => {
        const { result } = renderHook(() => useApiCreation(), { wrapper: modeWrapper });

        expect(result.current.state.creationMode).toBe('scratch');
        expect(result.current.state.selectedTemplate).toBeNull();
    });
});

describe('ApiCreationProvider — reducer actions', () => {
    it('SELECT_TEMPLATE switches to template mode and applies defaults', () => {
        const { result } = renderHook(() => useApiCreation(), { wrapper: defaultWrapper });

        act(() => {
            result.current.dispatch({ type: 'SELECT_TEMPLATE', template: MOCK_TEMPLATE });
        });

        expect(result.current.state.creationMode).toBe('template');
        expect(result.current.state.form.authType).toBe('api-key');
        expect(result.current.state.form.apiKeyPlanName).toBe('Template API Key Plan');
        expect(result.current.state.step).toBe(0);
    });

    it('SELECT_SCRATCH resets to scratch mode and clears the template', () => {
        const { result } = renderHook(() => useApiCreation(), { wrapper: defaultWrapper });

        act(() => {
            result.current.dispatch({ type: 'SELECT_TEMPLATE', template: MOCK_TEMPLATE });
            result.current.dispatch({ type: 'SELECT_SCRATCH' });
        });

        expect(result.current.state.creationMode).toBe('scratch');
        expect(result.current.state.selectedTemplate).toBeNull();
        expect(result.current.state.step).toBe(0);
    });

    it('UPDATE_FORM clears only the validation error for the patched field', () => {
        const { result } = renderHook(() => useApiCreation(), { wrapper: defaultWrapper });

        act(() => {
            result.current.dispatch({ type: 'SET_VALIDATION_ERRORS', errors: { apiName: 'Required', targetUrl: 'Required' } });
        });
        act(() => {
            result.current.dispatch({ type: 'UPDATE_FORM', patch: { apiName: 'My API' } });
        });

        expect(result.current.state.validationErrors).not.toHaveProperty('apiName');
        expect(result.current.state.validationErrors).toHaveProperty('targetUrl');
    });

    it('UPDATE_FORM with virtualHostsEnabled clears contextPath and virtualHosts errors but keeps others', () => {
        const { result } = renderHook(() => useApiCreation(), { wrapper: defaultWrapper });

        act(() => {
            result.current.dispatch({
                type: 'SET_VALIDATION_ERRORS',
                errors: { contextPath: 'Required', virtualHosts: 'Required', apiName: 'Required' },
            });
        });
        act(() => {
            result.current.dispatch({ type: 'UPDATE_FORM', patch: { virtualHostsEnabled: true } });
        });

        expect(result.current.state.validationErrors).not.toHaveProperty('contextPath');
        expect(result.current.state.validationErrors).not.toHaveProperty('virtualHosts');
        expect(result.current.state.validationErrors).toHaveProperty('apiName');
    });

    it('UPDATE_FORM with authType clears all plan name errors but keeps others', () => {
        const { result } = renderHook(() => useApiCreation(), { wrapper: defaultWrapper });

        act(() => {
            result.current.dispatch({
                type: 'SET_VALIDATION_ERRORS',
                errors: { apiKeyPlanName: 'Required', jwtPlanName: 'Required', apiName: 'Required' },
            });
        });
        act(() => {
            result.current.dispatch({ type: 'UPDATE_FORM', patch: { authType: 'jwt' } });
        });

        expect(result.current.state.validationErrors).not.toHaveProperty('apiKeyPlanName');
        expect(result.current.state.validationErrors).not.toHaveProperty('jwtPlanName');
        expect(result.current.state.validationErrors).toHaveProperty('apiName');
    });

    it('ADD_VIRTUAL_HOST appends a blank row with default host and path', () => {
        const { result } = renderHook(() => useApiCreation(), { wrapper: defaultWrapper });
        const initialCount = result.current.state.form.virtualHosts.length;

        act(() => {
            result.current.dispatch({ type: 'ADD_VIRTUAL_HOST' });
        });

        expect(result.current.state.form.virtualHosts).toHaveLength(initialCount + 1);
        const newRow = result.current.state.form.virtualHosts[initialCount]!;
        expect(newRow.host).toBe('');
        expect(newRow.path).toBe('/');
    });

    it('REMOVE_VIRTUAL_HOST removes the row at the given index', () => {
        const { result } = renderHook(() => useApiCreation(), { wrapper: defaultWrapper });

        act(() => {
            result.current.dispatch({ type: 'ADD_VIRTUAL_HOST' });
        });
        const countBefore = result.current.state.form.virtualHosts.length;

        act(() => {
            result.current.dispatch({ type: 'REMOVE_VIRTUAL_HOST', index: 0 });
        });

        expect(result.current.state.form.virtualHosts).toHaveLength(countBefore - 1);
    });

    it('REMOVE_VIRTUAL_HOST does not remove the last remaining row', () => {
        const { result } = renderHook(() => useApiCreation(), { wrapper: defaultWrapper });
        expect(result.current.state.form.virtualHosts).toHaveLength(1);

        act(() => {
            result.current.dispatch({ type: 'REMOVE_VIRTUAL_HOST', index: 0 });
        });

        expect(result.current.state.form.virtualHosts).toHaveLength(1);
    });

    it('UPDATE_VIRTUAL_HOST patches only the row at the given index', () => {
        const { result } = renderHook(() => useApiCreation(), { wrapper: defaultWrapper });

        act(() => {
            result.current.dispatch({ type: 'ADD_VIRTUAL_HOST' });
        });
        act(() => {
            result.current.dispatch({ type: 'UPDATE_VIRTUAL_HOST', index: 0, patch: { host: 'api.example.com' } });
        });

        expect(result.current.state.form.virtualHosts[0]!.host).toBe('api.example.com');
        expect(result.current.state.form.virtualHosts[1]!.host).toBe('');
    });

    it('SET_MODE switches creation mode and resets step to 0', () => {
        const { result } = renderHook(() => useApiCreation(), { wrapper: defaultWrapper });

        act(() => {
            result.current.dispatch({ type: 'SET_STEP', step: 3 });
        });
        act(() => {
            result.current.dispatch({ type: 'SET_MODE', mode: 'scratch' });
        });

        expect(result.current.state.creationMode).toBe('scratch');
        expect(result.current.state.step).toBe(0);
    });

    it('SET_STEP advances to the given step index', () => {
        const { result } = renderHook(() => useApiCreation(), { wrapper: defaultWrapper });

        act(() => {
            result.current.dispatch({ type: 'SET_STEP', step: 2 });
        });

        expect(result.current.state.step).toBe(2);
    });

    it('SET_TEMPLATES_OPEN toggles the templates panel open/closed', () => {
        const { result } = renderHook(() => useApiCreation(), { wrapper: defaultWrapper });

        act(() => {
            result.current.dispatch({ type: 'SET_TEMPLATES_OPEN', open: true });
        });
        expect(result.current.state.templatesOpen).toBe(true);

        act(() => {
            result.current.dispatch({ type: 'SET_TEMPLATES_OPEN', open: false });
        });
        expect(result.current.state.templatesOpen).toBe(false);
    });

    it('SET_PATH_VERIFYING toggles the isPathVerifying flag', () => {
        const { result } = renderHook(() => useApiCreation(), { wrapper: defaultWrapper });

        act(() => {
            result.current.dispatch({ type: 'SET_PATH_VERIFYING', value: true });
        });
        expect(result.current.state.isPathVerifying).toBe(true);

        act(() => {
            result.current.dispatch({ type: 'SET_PATH_VERIFYING', value: false });
        });
        expect(result.current.state.isPathVerifying).toBe(false);
    });

    it('SET_FIELD_ERROR adds a single field error without clearing others', () => {
        const { result } = renderHook(() => useApiCreation(), { wrapper: defaultWrapper });

        act(() => {
            result.current.dispatch({ type: 'SET_VALIDATION_ERRORS', errors: { apiName: 'Required' } });
        });
        act(() => {
            result.current.dispatch({ type: 'SET_FIELD_ERROR', field: 'contextPath', message: 'Already in use.' });
        });

        expect(result.current.state.validationErrors['contextPath']).toBe('Already in use.');
        expect(result.current.state.validationErrors['apiName']).toBe('Required');
    });

    it('SET_VALIDATION_ERRORS replaces all errors; CLEAR_VALIDATION_ERRORS resets to empty', () => {
        const { result } = renderHook(() => useApiCreation(), { wrapper: defaultWrapper });

        act(() => {
            result.current.dispatch({ type: 'SET_VALIDATION_ERRORS', errors: { apiName: 'Required' } });
        });
        expect(result.current.state.validationErrors).toEqual({ apiName: 'Required' });

        act(() => {
            result.current.dispatch({ type: 'CLEAR_VALIDATION_ERRORS' });
        });
        expect(result.current.state.validationErrors).toEqual({});
    });
});
