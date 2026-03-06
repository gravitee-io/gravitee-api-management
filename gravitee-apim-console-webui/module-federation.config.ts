import { ModuleFederationConfig } from '@nx/module-federation';

const EAGER_PACKAGES = [
  '@angular/core',
  '@angular/common',
  '@angular/common/http',
  '@angular/router',
  '@angular/platform-browser',
  '@angular/platform-browser-dynamic',
  'rxjs',
];

const config: ModuleFederationConfig = {
  name: 'console',
  exposes: {
    './PortalHomepage': 'src/mount-gamma.ts',
  },
  shared: (libraryName: string, sharedConfig: Record<string, unknown> | undefined) => {
    if (!libraryName || !sharedConfig) {
      return sharedConfig;
    }
    const eager = EAGER_PACKAGES.some(pkg => libraryName === pkg || libraryName.startsWith(`${pkg}/`));
    return {
      ...sharedConfig,
      singleton: true,
      strictVersion: true,
      requiredVersion: sharedConfig.requiredVersion ?? 'auto',
      ...(eager ? { eager: true } : {}),
    };
  },
};

export default config;
