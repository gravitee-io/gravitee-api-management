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
import { createContext, useContext, useReducer } from 'react';
import type { ReactNode } from 'react';

import type {
    ApiCreationMode,
    ApiCreationState,
    ApiProxyDraft,
    ProxyTemplate,
    ValidationErrors,
    VirtualHostEntry,
} from '../types/apiCreation';

// ─── Initial state ────────────────────────────────────────────────────────────

const INITIAL_FORM: ApiProxyDraft = {
    apiName: '',
    apiVersion: '1.0.0',
    apiDescription: '',
    contextPath: '',
    virtualHostsEnabled: false,
    virtualHosts: [{ id: crypto.randomUUID(), host: '', path: '/', overrideAccess: false }],
    targetUrl: '',
    authType: 'keyless',
    apiKeyPlanName: 'Default API Key plan',
    jwtPlanName: 'Default JWT plan',
    jwtSignature: 'RS256',
    jwtJwksResolver: 'JWKS_URL',
    jwtResolverParameter: '',
    oauth2PlanName: 'Default OAuth2 plan',
    oauth2Resource: '',
    mtlsPlanName: 'Default mTLS plan',
    deployImmediately: true,
};

const INITIAL_STATE: ApiCreationState = {
    creationMode: 'picker',
    templatesOpen: false,
    selectedTemplate: null,
    step: 0,
    form: INITIAL_FORM,
    validationErrors: {},
    isPathVerifying: false,
};

// ─── Actions ──────────────────────────────────────────────────────────────────

type ApiCreationAction =
    | { type: 'SET_MODE'; mode: ApiCreationMode }
    | { type: 'SET_STEP'; step: number }
    | { type: 'SET_TEMPLATES_OPEN'; open: boolean }
    | { type: 'SELECT_TEMPLATE'; template: ProxyTemplate }
    | { type: 'SELECT_SCRATCH' }
    | { type: 'UPDATE_FORM'; patch: Partial<ApiProxyDraft> }
    | { type: 'ADD_VIRTUAL_HOST' }
    | { type: 'REMOVE_VIRTUAL_HOST'; index: number }
    | { type: 'UPDATE_VIRTUAL_HOST'; index: number; patch: Omit<Partial<VirtualHostEntry>, 'id'> }
    | { type: 'SET_VALIDATION_ERRORS'; errors: ValidationErrors }
    | { type: 'CLEAR_VALIDATION_ERRORS' }
    | { type: 'SET_PATH_VERIFYING'; value: boolean }
    | { type: 'SET_FIELD_ERROR'; field: string; message: string };

// ─── Reducer ──────────────────────────────────────────────────────────────────

function apiCreationReducer(state: ApiCreationState, action: ApiCreationAction): ApiCreationState {
    switch (action.type) {
        case 'SET_MODE':
            return { ...state, creationMode: action.mode, step: 0 };

        case 'SET_STEP':
            return { ...state, step: action.step };

        case 'SET_TEMPLATES_OPEN':
            return { ...state, templatesOpen: action.open };

        case 'SELECT_TEMPLATE': {
            const d = action.template.defaults;
            return {
                ...state,
                creationMode: 'template',
                selectedTemplate: action.template,
                step: 0,
                form: {
                    ...state.form,
                    authType: d.authType,
                    ...(d.apiKeyPlanName !== undefined && { apiKeyPlanName: d.apiKeyPlanName }),
                    ...(d.jwtPlanName !== undefined && { jwtPlanName: d.jwtPlanName }),
                    ...(d.jwtSignature !== undefined && { jwtSignature: d.jwtSignature }),
                    ...(d.jwtJwksResolver !== undefined && { jwtJwksResolver: d.jwtJwksResolver }),
                    ...(d.jwtResolverParameter !== undefined && { jwtResolverParameter: d.jwtResolverParameter }),
                    ...(d.oauth2PlanName !== undefined && { oauth2PlanName: d.oauth2PlanName }),
                    ...(d.oauth2Resource !== undefined && { oauth2Resource: d.oauth2Resource }),
                    ...(d.mtlsPlanName !== undefined && { mtlsPlanName: d.mtlsPlanName }),
                    virtualHostsEnabled: false,
                },
            };
        }

        case 'SELECT_SCRATCH':
            return { ...state, creationMode: 'scratch', selectedTemplate: null, step: 0 };

        case 'UPDATE_FORM': {
            const remaining = { ...state.validationErrors };
            Object.keys(action.patch).forEach(k => delete remaining[k]);
            if ('virtualHostsEnabled' in action.patch) {
                delete remaining['virtualHosts'];
                delete remaining['contextPath'];
            }
            if ('authType' in action.patch) {
                delete remaining['apiKeyPlanName'];
                delete remaining['jwtPlanName'];
                delete remaining['oauth2PlanName'];
                delete remaining['mtlsPlanName'];
            }
            return { ...state, form: { ...state.form, ...action.patch }, validationErrors: remaining };
        }

        case 'ADD_VIRTUAL_HOST':
            return {
                ...state,
                form: {
                    ...state.form,
                    virtualHosts: [...state.form.virtualHosts, { id: crypto.randomUUID(), host: '', path: '/', overrideAccess: false }],
                },
            };

        case 'REMOVE_VIRTUAL_HOST':
            return {
                ...state,
                form: {
                    ...state.form,
                    virtualHosts:
                        state.form.virtualHosts.length === 1
                            ? state.form.virtualHosts
                            : state.form.virtualHosts.filter((_, i) => i !== action.index),
                },
            };

        case 'UPDATE_VIRTUAL_HOST':
            return {
                ...state,
                form: {
                    ...state.form,
                    virtualHosts: state.form.virtualHosts.map((row, i) => (i === action.index ? { ...row, ...action.patch } : row)),
                },
            };

        case 'SET_VALIDATION_ERRORS':
            return { ...state, validationErrors: action.errors };

        case 'CLEAR_VALIDATION_ERRORS':
            return { ...state, validationErrors: {} };

        case 'SET_PATH_VERIFYING':
            return { ...state, isPathVerifying: action.value };

        case 'SET_FIELD_ERROR':
            return { ...state, validationErrors: { ...state.validationErrors, [action.field]: action.message } };

        default:
            return state;
    }
}

// ─── Context ──────────────────────────────────────────────────────────────────

interface ApiCreationContextValue {
    state: ApiCreationState;
    dispatch: React.Dispatch<ApiCreationAction>;
}

const ApiCreationContext = createContext<ApiCreationContextValue | null>(null);

interface ApiCreationProviderProps {
    children: ReactNode;
    initialTemplate?: ProxyTemplate;
    initialMode?: ApiCreationMode;
}

export function ApiCreationProvider({ children, initialTemplate, initialMode }: ApiCreationProviderProps) {
    const [state, dispatch] = useReducer(
        apiCreationReducer,
        { initialTemplate, initialMode },
        ({ initialTemplate: tmpl, initialMode: mode }) => {
            if (tmpl) return apiCreationReducer(INITIAL_STATE, { type: 'SELECT_TEMPLATE', template: tmpl });
            if (mode) return { ...INITIAL_STATE, creationMode: mode };
            return INITIAL_STATE;
        },
    );
    return <ApiCreationContext.Provider value={{ state, dispatch }}>{children}</ApiCreationContext.Provider>;
}

export function useApiCreation(): ApiCreationContextValue {
    const ctx = useContext(ApiCreationContext);
    if (!ctx) throw new Error('useApiCreation must be used inside ApiCreationProvider');
    return ctx;
}
