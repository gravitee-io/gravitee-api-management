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

import type { AuthType, ProxyTemplate, VirtualHostEntry, WizardFormState, WizardMode, WizardState } from '../types/wizard';

// ─── Initial state ────────────────────────────────────────────────────────────

const INITIAL_FORM: WizardFormState = {
    apiName: '',
    apiVersion: '1.0.0',
    apiDescription: '',
    contextPath: '',
    virtualHostsEnabled: false,
    virtualHosts: [{ host: '', path: '/', overrideAccess: false }],
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

const INITIAL_STATE: WizardState = {
    wizardMode: 'picker',
    templatesOpen: false,
    selectedTemplate: null,
    step: 0,
    form: INITIAL_FORM,
};

// ─── Actions ──────────────────────────────────────────────────────────────────

type WizardAction =
    | { type: 'SET_MODE'; mode: WizardMode }
    | { type: 'SET_STEP'; step: number }
    | { type: 'SET_TEMPLATES_OPEN'; open: boolean }
    | { type: 'SELECT_TEMPLATE'; template: ProxyTemplate }
    | { type: 'SELECT_SCRATCH' }
    | { type: 'UPDATE_FORM'; patch: Partial<WizardFormState> }
    | { type: 'ADD_VIRTUAL_HOST' }
    | { type: 'REMOVE_VIRTUAL_HOST'; index: number }
    | { type: 'UPDATE_VIRTUAL_HOST'; index: number; patch: Partial<VirtualHostEntry> };

// ─── Reducer ──────────────────────────────────────────────────────────────────

function wizardReducer(state: WizardState, action: WizardAction): WizardState {
    switch (action.type) {
        case 'SET_MODE':
            return { ...state, wizardMode: action.mode, step: 0 };

        case 'SET_STEP':
            return { ...state, step: action.step };

        case 'SET_TEMPLATES_OPEN':
            return { ...state, templatesOpen: action.open };

        case 'SELECT_TEMPLATE': {
            const d = action.template.defaults;
            return {
                ...state,
                wizardMode: 'template',
                selectedTemplate: action.template,
                step: 0,
                form: {
                    ...state.form,
                    authType: d.authType as AuthType,
                    ...(d.apiKeyPlanName !== undefined && { apiKeyPlanName: d.apiKeyPlanName }),
                    ...(d.jwtPlanName !== undefined && { jwtPlanName: d.jwtPlanName }),
                    ...(d.jwtSignature !== undefined && { jwtSignature: d.jwtSignature }),
                    ...(d.jwtJwksResolver !== undefined && { jwtJwksResolver: d.jwtJwksResolver }),
                    ...(d.jwtResolverParameter !== undefined && { jwtResolverParameter: d.jwtResolverParameter }),
                    ...(d.oauth2PlanName !== undefined && { oauth2PlanName: d.oauth2PlanName }),
                    ...(d.oauth2Resource !== undefined && { oauth2Resource: d.oauth2Resource }),
                    ...(d.mtlsPlanName !== undefined && { mtlsPlanName: d.mtlsPlanName }),
                    ...(d.descriptionHint !== undefined && { apiDescription: d.descriptionHint }),
                    virtualHostsEnabled: false,
                },
            };
        }

        case 'SELECT_SCRATCH':
            return { ...state, wizardMode: 'scratch', selectedTemplate: null, step: 0 };

        case 'UPDATE_FORM':
            return { ...state, form: { ...state.form, ...action.patch } };

        case 'ADD_VIRTUAL_HOST':
            return {
                ...state,
                form: {
                    ...state.form,
                    virtualHosts: [...state.form.virtualHosts, { host: '', path: '/', overrideAccess: false }],
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

        default:
            return state;
    }
}

// ─── Context ──────────────────────────────────────────────────────────────────

interface WizardContextValue {
    state: WizardState;
    dispatch: React.Dispatch<WizardAction>;
}

const WizardContext = createContext<WizardContextValue | null>(null);

export function WizardProvider({ children }: { children: ReactNode }) {
    const [state, dispatch] = useReducer(wizardReducer, INITIAL_STATE);
    return <WizardContext.Provider value={{ state, dispatch }}>{children}</WizardContext.Provider>;
}

export function useWizard(): WizardContextValue {
    const ctx = useContext(WizardContext);
    if (!ctx) throw new Error('useWizard must be used inside WizardProvider');
    return ctx;
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

export function slugify(name: string): string {
    return name
        .toLowerCase()
        .trim()
        .replace(/[^a-z0-9\s-]/g, '')
        .replace(/\s+/g, '-')
        .replace(/-+/g, '-')
        .replace(/^-|-$/g, '');
}
