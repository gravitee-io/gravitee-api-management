export type ApiDetails = {
    name: string;
    version: string;
    description: string;
};

export type VirtualHost = {
    host: string;
    path: string;
    overrideAccess: boolean;
};

export type ProxyConfig = {
    contextPath: string;
    targetUrl: string;
    enableVirtualHosts: boolean;
    virtualHosts: VirtualHost[];
};

export type SecurityConfig =
    | { type: 'keyless' }
    | { type: 'api-key'; planName: string }
    | { type: 'jwt'; planName: string; signature: string; jwksResolver: string; resolverParam: string }
    | { type: 'oauth2'; planName: string; resource: string }
    | { type: 'mtls'; planName: string };

export type ApiCreationState = {
    details: ApiDetails;
    proxy: ProxyConfig;
    security: SecurityConfig;
    deployImmediately: boolean;
};

export type ApiCreationMode = 'scratch' | 'template';

