import { ModuleFederationConfig } from '@nx/module-federation';

const REACT_PACKAGES = ['react', 'react-dom'];

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
        return sharedConfig;
    },
};

/**
 * Nx requires a default export of the config to allow correct resolution of the module federation graph.
 **/
export default config;
