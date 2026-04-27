import type { ApiCreationState } from './models';
import type React from 'react';

export type FieldType = 'input' | 'select' | 'switch';

export type SelectOption = {
    readonly value: string;
    readonly label: string;
};

export type FieldConfig = {
    readonly id: string;
    readonly type: FieldType;
    readonly label: string;
    readonly description?: string;
    readonly bind: string;
    readonly required?: boolean;
    readonly visible?: (state: ApiCreationState) => boolean;
    readonly options?: readonly SelectOption[];
    readonly inputType?: React.HTMLInputTypeAttribute;
    readonly inputKind?: 'text' | 'textarea';
    /** HTML constraint validation `pattern` (full-string match). */
    readonly pattern?: string;
    /** `title` on the control for native validation tooltips. */
    readonly validationTitle?: string;
};

/** Leading `/` only; further rules are left to the browser / server where needed. */
export const proxyContextPathInputPattern = '^/.*$';
export const proxyContextPathInputTitle = 'Path must start with /.';

export const fieldRegistry = {
    apiName: {
        id: 'apiName',
        type: 'input',
        label: 'API Name',
        description: 'Public name shown to consumers in the Developer Portal.',
        bind: 'details.name',
        required: true,
    },
    apiVersion: {
        id: 'apiVersion',
        type: 'input',
        label: 'Version',
        description: 'For example 1.0.0.',
        bind: 'details.version',
        required: true,
    },
    apiDescription: {
        id: 'apiDescription',
        type: 'input',
        label: 'Description',
        description: 'Helps consumers understand what your API offers.',
        bind: 'details.description',
        required: false,
        inputKind: 'textarea',
    },
    contextPath: {
        id: 'contextPath',
        type: 'input',
        label: 'Context Path',
        description: 'Must start with / and can contain letters, numbers, dash, or underscore.',
        bind: 'proxy.contextPath',
        required: true,
        pattern: proxyContextPathInputPattern,
        validationTitle: proxyContextPathInputTitle,
    },
    enableVirtualHosts: {
        id: 'enableVirtualHosts',
        type: 'switch',
        label: 'Enable virtual hosts',
        bind: 'proxy.enableVirtualHosts',
    },
    targetUrl: {
        id: 'targetUrl',
        type: 'input',
        label: 'Target URL',
        description: 'Where the gateway forwards traffic to.',
        bind: 'proxy.targetUrl',
        required: true,
        inputType: 'url',
    },
    authType: {
        id: 'authType',
        type: 'select',
        label: 'Consumer Authentication',
        description: "Pick a security type to create the API's default plan.",
        bind: 'security.type',
        required: true,
        options: [
            { value: 'keyless', label: 'Keyless (Open)' },
            { value: 'api-key', label: 'API Key' },
            { value: 'jwt', label: 'JWT' },
            { value: 'oauth2', label: 'OAuth 2.0' },
            { value: 'mtls', label: 'mTLS' },
        ],
    },
    planName: {
        id: 'planName',
        type: 'input',
        label: 'Plan Name',
        description: 'Name shown to consumers when they subscribe.',
        bind: 'security.planName',
        required: true,
        visible: (state) => state.security.type !== 'keyless',
    },
    jwtSignature: {
        id: 'jwtSignature',
        type: 'input',
        label: 'Signature',
        description: 'Algorithm used to verify token signatures.',
        bind: 'security.signature',
        required: true,
        visible: (state) => state.security.type === 'jwt',
    },
    jwtJwksResolver: {
        id: 'jwtJwksResolver',
        type: 'input',
        label: 'JWKS Resolver',
        description: 'Where the gateway loads the verification key from.',
        bind: 'security.jwksResolver',
        required: true,
        visible: (state) => state.security.type === 'jwt',
    },
    jwtResolverParam: {
        id: 'jwtResolverParam',
        type: 'input',
        label: 'Resolver Parameter',
        description: 'Value used by the resolver (URL, key identifier).',
        bind: 'security.resolverParam',
        required: true,
        visible: (state) => state.security.type === 'jwt',
    },
    oauth2Resource: {
        id: 'oauth2Resource',
        type: 'input',
        label: 'Resource',
        bind: 'security.resource',
        required: true,
        visible: (state) => state.security.type === 'oauth2',
    },
    deployImmediately: {
        id: 'deployImmediately',
        type: 'switch',
        label: 'Deploy and start API immediately',
        description: 'When enabled, the API proxy will be deployed to the gateway and start accepting traffic right away.',
        bind: 'deployImmediately',
    },
} as const satisfies Record<string, FieldConfig>;

