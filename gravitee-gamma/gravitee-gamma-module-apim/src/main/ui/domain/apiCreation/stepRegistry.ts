import type { ApiCreationMode, ApiCreationState, SecurityConfig } from './models';
import type { StepId } from './schema';
import { validateStep } from './schema';
import type { FieldConfig } from './fieldRegistry';
import { fieldRegistry } from './fieldRegistry';

export type StepConfig = {
    readonly id: StepId;
    readonly label: string;
    readonly fields: readonly FieldConfig[];
    readonly isVisible?: (state: ApiCreationState) => boolean;
    readonly validate: (state: ApiCreationState) => boolean;
};

export type ApiCreationTemplate = {
    readonly id: string;
    readonly label: string;
    readonly headline?: string;
    readonly description: string;
    readonly tags: readonly string[];
    readonly icon: 'api-key' | 'jwt' | 'oauth2' | 'keyless';
    readonly caution?: { readonly label: string; readonly description: string };
    readonly steps: readonly StepId[];
    readonly defaults: Partial<ApiCreationState>;
};

const scratchSteps: readonly StepId[] = ['api-details', 'configure-proxy', 'secure', 'review-deploy'];

export const apiCreationTemplates: readonly ApiCreationTemplate[] = [
    {
        id: 'jwt-default',
        label: 'REST API with JWT',
        headline: 'Enterprise identity provider',
        description:
            'Validate JWTs issued by your identity provider. Best for organizations with an existing IdP like Auth0, Okta, or Azure AD.',
        tags: ['REST', 'JWT', 'JWKS', 'Enterprise'],
        icon: 'jwt',
        steps: ['api-details', 'configure-proxy', 'secure', 'review-deploy'],
        defaults: {
            security: {
                type: 'jwt',
                planName: 'Default JWT plan',
                signature: 'RSA_RS256',
                jwksResolver: 'JWKS_URL',
                resolverParam: 'https://idp.example.com/.well-known/jwks.json',
            },
        },
    },
    {
        id: 'rest-oauth2',
        label: 'REST API with OAuth 2.0',
        headline: 'Token-based enterprise security',
        description: 'Enforce OAuth 2.0 access tokens with token introspection. Ideal for enterprise APIs that require delegated authorization.',
        tags: ['REST', 'OAuth 2.0', 'Introspection', 'Enterprise'],
        icon: 'oauth2',
        steps: ['api-details', 'configure-proxy', 'secure', 'review-deploy'],
        defaults: {
            security: { type: 'oauth2', planName: 'Default OAuth2 plan', resource: '' },
        },
    },
    {
        id: 'apikey-no-secure-step',
        label: 'REST API with API Key',
        headline: 'Most common pattern',
        description:
            'Protect your REST API with simple API key authentication. Consumers receive a key when they subscribe to the plan.',
        tags: ['REST', 'API Key', 'Simple onboarding'],
        icon: 'api-key',
        steps: ['api-details', 'configure-proxy', 'review-deploy'],
        defaults: {
            security: { type: 'api-key', planName: 'Default API key plan' },
        },
    },
    {
        id: 'keyless',
        label: 'REST API with Keyless plan',
        headline: 'Token-free access (not recommended)',
        description: 'Creates a REST proxy with a keyless (open) plan so traffic is accepted without API keys or subscriptions.',
        tags: ['REST', 'Keyless', 'Demo / sandbox'],
        icon: 'keyless',
        caution: {
            label: 'Demo and testing only',
            description:
                'For demos, workshops, and local testing only. The API is publicly reachable without subscriptions or API keys, and you will not get plan-based quotas or access rules. Do not use for production or sensitive data.',
        },
        steps: ['api-details', 'configure-proxy', 'review-deploy'],
        defaults: {
            security: { type: 'keyless' },
        },
    },
] as const;

const stepById = (id: StepId): Omit<StepConfig, 'validate'> => {
    switch (id) {
        case 'api-details':
            return {
                id,
                label: 'API Details',
                fields: [fieldRegistry.apiName, fieldRegistry.apiVersion, fieldRegistry.apiDescription],
            };
        case 'configure-proxy':
            return {
                id,
                label: 'Configure Proxy',
                fields: [fieldRegistry.contextPath, fieldRegistry.enableVirtualHosts, fieldRegistry.targetUrl],
            };
        case 'secure':
            return {
                id,
                label: 'Secure',
                fields: [
                    fieldRegistry.authType,
                    fieldRegistry.planName,
                    fieldRegistry.jwtSignature,
                    fieldRegistry.jwtJwksResolver,
                    fieldRegistry.jwtResolverParam,
                    fieldRegistry.oauth2Resource,
                ],
            };
        case 'review-deploy':
            return {
                id,
                label: 'Review & Deploy',
                fields: [fieldRegistry.deployImmediately],
            };
    }
};

export function getTemplateById(templateId: string | undefined): ApiCreationTemplate | undefined {
    if (!templateId) return undefined;
    return apiCreationTemplates.find((t) => t.id === templateId);
}

export function buildSteps(mode: ApiCreationMode, templateId: string | undefined, state: ApiCreationState): readonly StepConfig[] {
    const template = mode === 'template' ? getTemplateById(templateId) : undefined;
    const stepIds = template?.steps ?? scratchSteps;

    return stepIds
        .map((id) => {
            const base = stepById(id);
            const validate = (s: ApiCreationState) => validateStep(id, s).success;
            return { ...base, validate };
        })
        .filter((step) => (step.isVisible ? step.isVisible(state) : true));
}

export function defaultSecurityConfig(): SecurityConfig {
    return { type: 'keyless' };
}

