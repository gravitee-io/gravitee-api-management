import { ModuleFederationConfig } from '@nx/module-federation';

const REACT_PACKAGES = ['react', 'react-dom'];

const EAGER_ANGULAR_PACKAGES = [
    '@angular/core',
    '@angular/common',
    '@angular/common/http',
    '@angular/platform-browser',
    '@angular/router',
];

const config: ModuleFederationConfig = {
    name: 'app_beta',
    exposes: {
        './Module': 'app_beta/src/app/remote-entry/mount.ts',
    },
    shared: (libraryName, sharedConfig) => {
        if (
            REACT_PACKAGES.some(
                (pkg) =>
                    libraryName === pkg || libraryName.startsWith(`${pkg}/`)
            )
        ) {
            return false;
        }
        if (!libraryName || !sharedConfig) {
            return sharedConfig;
        }
        const eager = EAGER_ANGULAR_PACKAGES.some(
            (pkg) => libraryName === pkg || libraryName.startsWith(`${pkg}/`)
        );
        return {
            ...sharedConfig,
            ...(eager ? { eager: true } : {}),
        };
    },
};

/**
 * Nx requires a default export of the config to allow correct resolution of the module federation graph.
 **/
export default config;
