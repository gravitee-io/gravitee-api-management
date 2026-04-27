import { createContext, useCallback, useContext, useMemo, useReducer } from 'react';

import type { ApiCreationMode, ApiCreationState, SecurityConfig } from './models';
import type { StepId } from './schema';
import { validateStep } from './schema';
import { buildSteps, defaultSecurityConfig, getTemplateById } from './stepRegistry';
import type { StepConfig } from './stepRegistry';

type ApiCreationStoreState = {
    readonly mode: ApiCreationMode;
    readonly templateId?: string;
    readonly activeStepId?: StepId;
    readonly steps: readonly StepConfig[];
    readonly data: ApiCreationState;
    readonly validationErrors: Record<string, string>;
};

type Action =
    | { type: 'start'; mode: ApiCreationMode; templateId?: string }
    | { type: 'setStep'; stepId: StepId }
    | { type: 'nextStep' }
    | { type: 'previousStep' }
    | { type: 'updateField'; path: string; value: unknown }
    | { type: 'setValidationErrors'; errors: Record<string, string> };

const initialData = (): ApiCreationState => ({
    details: { name: '', version: '1.0.0', description: '' },
    proxy: { contextPath: '', targetUrl: '', enableVirtualHosts: false, virtualHosts: [{ host: '', path: '/', overrideAccess: false }] },
    security: defaultSecurityConfig(),
    deployImmediately: true,
});

const applyDefaults = (data: ApiCreationState, defaults: Partial<ApiCreationState>): ApiCreationState => ({
    ...data,
    ...defaults,
    details: { ...data.details, ...defaults.details },
    proxy: { ...data.proxy, ...defaults.proxy },
    security: (defaults.security ?? data.security) as SecurityConfig,
});

const getAtPath = (obj: unknown, path: string): unknown => {
    const parts = path.split('.').filter(Boolean);
    let cur: any = obj;
    for (const p of parts) {
        if (cur == null) return undefined;
        cur = cur[p];
    }
    return cur;
};

const setAtPath = <T,>(obj: T, path: string, value: unknown): T => {
    const parts = path.split('.').filter(Boolean);
    if (parts.length === 0) return obj;

    const clone: any = Array.isArray(obj) ? [...(obj as any)] : { ...(obj as any) };
    let cur: any = clone;

    for (let i = 0; i < parts.length - 1; i++) {
        const k = parts[i];
        const next = cur[k];
        cur[k] = Array.isArray(next) ? [...next] : { ...(next ?? {}) };
        cur = cur[k];
    }

    cur[parts.at(-1)!] = value;
    return clone as T;
};

function coerceSecurityForType(current: SecurityConfig, nextType: string): SecurityConfig {
    if (current.type === nextType) return current;

    switch (nextType) {
        case 'keyless':
            return { type: 'keyless' };
        case 'api-key':
            return { type: 'api-key', planName: '' };
        case 'jwt':
            return { type: 'jwt', planName: '', signature: '', jwksResolver: '', resolverParam: '' };
        case 'oauth2':
            return { type: 'oauth2', planName: '', resource: '' };
        case 'mtls':
            return { type: 'mtls', planName: '' };
        default:
            return current;
    }
}

const reducer = (state: ApiCreationStoreState, action: Action): ApiCreationStoreState => {
    switch (action.type) {
        case 'start': {
            const template = action.mode === 'template' ? getTemplateById(action.templateId) : undefined;
            const seeded = template ? applyDefaults(initialData(), template.defaults) : initialData();

            const steps = buildSteps(action.mode, action.templateId, seeded);
            return {
                ...state,
                mode: action.mode,
                templateId: action.templateId,
                activeStepId: steps[0]?.id,
                steps,
                data: seeded,
                validationErrors: {},
            };
        }
        case 'setStep':
            return { ...state, activeStepId: action.stepId, validationErrors: {} };
        case 'previousStep': {
            if (!state.activeStepId) return state;
            const idx = state.steps.findIndex((s) => s.id === state.activeStepId);
            const prev = idx > 0 ? state.steps[idx - 1] : undefined;
            return prev ? { ...state, activeStepId: prev.id, validationErrors: {} } : state;
        }
        case 'nextStep': {
            if (!state.activeStepId) return state;
            const idx = state.steps.findIndex((s) => s.id === state.activeStepId);
            const next = idx >= 0 ? state.steps[idx + 1] : undefined;
            return next ? { ...state, activeStepId: next.id, validationErrors: {} } : state;
        }
        case 'updateField': {
            if (action.path === 'security.type') {
                const nextSecurity = coerceSecurityForType(state.data.security, String(action.value));
                const nextData = setAtPath(state.data, 'security', nextSecurity);
                const nextSteps = buildSteps(state.mode, state.templateId, nextData);
                const activeStepId = nextSteps.some((s) => s.id === state.activeStepId) ? state.activeStepId : nextSteps[0]?.id;

                return {
                    ...state,
                    data: nextData,
                    steps: nextSteps,
                    activeStepId,
                };
            }

            const nextData = setAtPath(state.data, action.path, action.value);
            const nextSteps = buildSteps(state.mode, state.templateId, nextData);
            const activeStepId = nextSteps.some((s) => s.id === state.activeStepId) ? state.activeStepId : nextSteps[0]?.id;

            return {
                ...state,
                data: nextData,
                steps: nextSteps,
                activeStepId,
            };
        }
        case 'setValidationErrors':
            return { ...state, validationErrors: action.errors };
    }
};

const StoreContext = createContext<{
    readonly state: ApiCreationStoreState;
    readonly dispatch: (action: Action) => void;
} | null>(null);

const initialStoreState: ApiCreationStoreState = {
    mode: 'scratch',
    templateId: undefined,
    activeStepId: undefined,
    steps: [],
    data: initialData(),
    validationErrors: {},
};

export function ApiCreationStoreProvider({ children }: Readonly<{ children: React.ReactNode }>) {
    const [state, dispatch] = useReducer(reducer, initialStoreState);
    const value = useMemo(() => ({ state, dispatch }), [state]);
    return <StoreContext.Provider value={value}>{children}</StoreContext.Provider>;
}

export function useApiCreationStore() {
    const ctx = useContext(StoreContext);
    if (!ctx) throw new Error('useApiCreationStore must be used within ApiCreationStoreProvider');

    const { state, dispatch } = ctx;

    const setStep = useCallback((stepId: StepId) => dispatch({ type: 'setStep', stepId }), [dispatch]);
    const nextStep = useCallback(() => dispatch({ type: 'nextStep' }), [dispatch]);
    const previousStep = useCallback(() => dispatch({ type: 'previousStep' }), [dispatch]);

    const startScratch = useCallback(() => dispatch({ type: 'start', mode: 'scratch' }), [dispatch]);
    const startTemplate = useCallback((templateId: string) => dispatch({ type: 'start', mode: 'template', templateId }), [dispatch]);

    const updateField = useCallback((path: string, value: unknown) => dispatch({ type: 'updateField', path, value }), [dispatch]);

    const validateActiveStep = useCallback((): boolean => {
        if (!state.activeStepId) return false;
        const result = validateStep(state.activeStepId, state.data);
        dispatch({ type: 'setValidationErrors', errors: result.success ? {} : result.errors });
        return result.success;
    }, [dispatch, state.activeStepId, state.data]);

    const activeStep = useMemo(
        () => (state.activeStepId ? state.steps.find((s) => s.id === state.activeStepId) : undefined),
        [state.activeStepId, state.steps],
    );
    const activeStepIndex = useMemo(
        () => (state.activeStepId ? Math.max(0, state.steps.findIndex((s) => s.id === state.activeStepId)) : 0),
        [state.activeStepId, state.steps],
    );

    const getValue = useCallback((path: string) => getAtPath(state.data, path), [state.data]);

    return {
        state,
        actions: {
            getValue,
            nextStep,
            previousStep,
            setStep,
            startScratch,
            startTemplate,
            updateField,
            validateActiveStep,
        },
        selectors: {
            activeStep,
            activeStepIndex,
        },
    };
}

