export type ApiProxyWizardStepId = 'details' | 'configure' | 'secure' | 'review';

export type ApiDetailsModel = {
    readonly name: string;
    readonly version: string;
    readonly description: string;
};

export type VirtualHostModel = {
    readonly host: string;
    readonly path: string;
    readonly overrideAccess: boolean;
};

export type EntrypointsModel =
    | {
          readonly type: 'context_path';
          readonly contextPath: string;
      }
    | {
          readonly type: 'virtual_hosts';
          readonly virtualHosts: readonly VirtualHostModel[];
      };

export type ProxyConfigurationModel = {
    readonly entrypoints: EntrypointsModel;
    readonly upstreamUrl: string;
};

export type SecurityType = 'keyless' | 'api_key' | 'jwt' | 'oauth2' | 'mtls';

export type SecurityModel =
    | {
          readonly type: 'keyless';
      }
    | {
          readonly type: 'api_key';
          readonly planName: string;
      }
    | {
          readonly type: 'jwt';
          readonly planName: string;
          readonly signature: string;
          readonly jwksResolver: 'GIVEN_KEY' | 'GATEWAY_KEYS' | 'JWKS_URL';
          readonly resolverParameter: string;
      }
    | {
          readonly type: 'oauth2';
          readonly planName: string;
          readonly resource: string;
      }
    | {
          readonly type: 'mtls';
          readonly planName: string;
      };

export type DeploymentModel = {
    readonly deployImmediately: boolean;
};

export type ApiProxyWizardModel = {
    readonly details: ApiDetailsModel;
    readonly proxy: ProxyConfigurationModel;
    readonly security: SecurityModel;
    readonly deployment: DeploymentModel;
};

/**
 * Stable shape for future API calls (create proxy + deploy).
 * We keep this "command" format separated from UI models so it can evolve with backend DTOs.
 */
export type CreateAndDeployApiProxyCommand = {
    readonly api: {
        readonly name: string;
        readonly version: string;
        readonly description: string;
        readonly type: 'proxy';
        readonly protocol: 'REST';
    };
    readonly proxy: {
        readonly upstreamUrl: string;
        readonly entrypoints: EntrypointsModel;
    };
    readonly security: SecurityModel;
    readonly deployment: DeploymentModel;
};

export function buildCreateAndDeployCommand(model: ApiProxyWizardModel): CreateAndDeployApiProxyCommand {
    return {
        api: {
            name: model.details.name.trim(),
            version: model.details.version.trim(),
            description: model.details.description.trim(),
            type: 'proxy',
            protocol: 'REST',
        },
        proxy: {
            upstreamUrl: model.proxy.upstreamUrl.trim(),
            entrypoints: model.proxy.entrypoints,
        },
        security: model.security,
        deployment: model.deployment,
    };
}

export function slugify(value: string): string {
    return value
        .toLowerCase()
        .trim()
        .replace(/[^a-z0-9\s-]/g, '')
        .replace(/\s+/g, '-')
        .replace(/-+/g, '-')
        .replace(/^-|-$/g, '');
}

