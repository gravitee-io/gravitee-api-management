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
import { ModuleFederationConfig } from '@nx/module-federation';

const config: ModuleFederationConfig = {
    name: 'gravitee-gamma-module-apim',
    exposes: {
        './App': './src/main/ui/federation.tsx',
    },
    // Must match the shared config in the host app (gamma-control-plane-webui)
    shared: (libraryName, sharedConfig) => {
        if (['react', 'react-dom', 'react-router-dom', '@gravitee/graphene'].includes(libraryName)) {
            return {
                singleton: true,
                strictVersion: false,
                requiredVersion: sharedConfig.requiredVersion,
            };
        }
        return false;
    },
};

export default config;
