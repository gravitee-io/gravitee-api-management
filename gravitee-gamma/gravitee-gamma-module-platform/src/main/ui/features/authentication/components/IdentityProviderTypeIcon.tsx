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

import { GithubIcon } from '@gravitee/graphene-core/icons';
import type { ReactElement } from 'react';

import type { IdentityProviderType } from '../types/identityProvider';

function GoogleIcon({ className }: { className?: string }) {
    return (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" className={className}>
            <path
                fill="#4285F4"
                d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
            />
            <path
                fill="#34A853"
                d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
            />
            <path
                fill="#FBBC05"
                d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
            />
            <path
                fill="#EA4335"
                d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
            />
        </svg>
    );
}

function GithubTypeIcon({ className }: { className?: string }) {
    return <GithubIcon className={className} aria-hidden="true" />;
}

function GraviteeAmIcon({ className }: { className?: string }) {
    return (
        <svg viewBox="0 0 108.26 138.53" xmlns="http://www.w3.org/2000/svg" className={className}>
            <path
                fill="currentColor"
                d="m92.57 16.85h-58.45a17 17 0 0 0-17 17v25.51a16.93 16.93 0 0 0 4.12 11.07 15.85 15.85 0 0 0-1.74 19 16.93 16.93 0 0 0-6.65 13.45v1.83a17 17 0 0 0 17 17h46.54a17 17 0 0 0 17-17v-1.83a17 17 0 0 0-17-17h-43.33a4.83 4.83 0 0 1 0-9.65h.17 38.88a17 17 0 0 0 17-17l.08-29a2.28 2.28 0 0 1 2.25-2.23h1.13a2.8 2.8 0 0 0 2.8-2.8v-5.56a2.79 2.79 0 0 0-2.8-2.79zm-10.37 86v1.83a5.82 5.82 0 0 1-5.81 5.82h-46.54a5.83 5.83 0 0 1-5.85-5.78v-1.83a5.83 5.83 0 0 1 5.82-5.82h46.57a5.82 5.82 0 0 1 5.81 5.82zm-4.26-43.49a5.82 5.82 0 0 1-5.82 5.81h-38a5.82 5.82 0 0 1-5.82-5.81v-25.55a5.83 5.83 0 0 1 5.82-5.81h38a5.83 5.83 0 0 1 5.82 5.82z"
            />
        </svg>
    );
}

function OidcIcon({ className }: { className?: string }) {
    return (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 90 90" className={className}>
            <g transform="translate(-59,-80)">
                <g transform="matrix(0.35277777,0,0,-0.35277777,-15.526842,222.47817)">
                    <path fill="currentColor" d="m 330.10774,359.51207 v -159.9391 -20.0609 l 32,15.0609 v 180.5721 z" />
                    <path fill="currentColor" d="m 440.93004,306.71227 4.417,-45.864 -61.883,13.464" />
                    <path
                        fill="currentColor"
                        d="m 266.10774,248.01987 c 0,22.674 24.707,41.769 58.383,47.598 v 20.325 c -51.51,-6.226 -90.383,-34.267 -90.383,-67.923 0,-34.869 41.725,-63.709 96,-68.508 v 20.061 c -36.516,4.578 -64,24.528 -64,48.447 m 101.617,67.915 v -20.317 c 13.399,-2.319 25.385,-6.727 34.9511,-12.64 l 22.6269,13.984 c -15.42,9.531 -35.322,16.283 -57.578,18.973"
                    />
                </g>
            </g>
        </svg>
    );
}

const ICONS: Record<IdentityProviderType, (props: { className?: string }) => ReactElement> = {
    GOOGLE: GoogleIcon,
    GITHUB: GithubTypeIcon,
    GRAVITEEIO_AM: GraviteeAmIcon,
    OIDC: OidcIcon,
};

export function IdentityProviderTypeIcon({ type, className }: { readonly type: IdentityProviderType; readonly className?: string }) {
    const Icon = ICONS[type] ?? OidcIcon;
    return (
        <span aria-hidden>
            <Icon className={className} />
        </span>
    );
}
