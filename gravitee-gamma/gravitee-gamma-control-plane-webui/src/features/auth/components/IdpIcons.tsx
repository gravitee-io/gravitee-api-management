/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import type { IdentityProviderType } from '../auth.types';

function GoogleIcon({ className }: { className?: string }) {
    return (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 833.5 844.3" className={className}>
            <rect width="833.5" height="844.3" fill="white" rx="5%" ry="5%" />
            <path
                fill="#4285F4"
                transform="translate(150, 150)"
                d="M533.5,278.4c0-18.5-1.5-37.1-4.7-55.3H272.1v104.8h147c-6.1,33.8-25.7,63.7-54.4,82.7v68h87.7 C503.9,431.2,533.5,361.2,533.5,278.4z"
            />
            <path
                fill="#34A853"
                transform="translate(150, 150)"
                d="M272.1,544.3c73.4,0,135.3-24.1,180.4-65.7l-87.7-68c-24.4,16.6-55.9,26-92.6,26c-71,0-131.2-47.9-152.8-112.3 H28.9v70.1C75.1,486.3,169.2,544.3,272.1,544.3z"
            />
            <path
                fill="#FBBC04"
                transform="translate(150, 150)"
                d="M119.3,324.3c-11.4-33.8-11.4-70.4,0-104.2V150H28.9c-38.6,76.9-38.6,167.5,0,244.4L119.3,324.3z"
            />
            <path
                fill="#EA4335"
                transform="translate(150, 150)"
                d="M272.1,107.7c38.8-0.6,76.3,14,104.4,40.8l0,0l77.7-77.7C405,24.6,339.7-0.8,272.1,0C169.2,0,75.1,58,28.9,150 l90.4,70.1C140.8,155.6,201.1,107.7,272.1,107.7z"
            />
        </svg>
    );
}

function GitHubIcon({ className }: { className?: string }) {
    return (
        <svg viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg" className={className}>
            <path
                fill="currentColor"
                fillRule="evenodd"
                clipRule="evenodd"
                transform="translate(4, 4)"
                d="M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12"
            />
        </svg>
    );
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

const icons: Record<IdentityProviderType, React.FC<{ className?: string }>> = {
    GOOGLE: GoogleIcon,
    GITHUB: GitHubIcon,
    GRAVITEEIO_AM: GraviteeAmIcon,
    OIDC: OidcIcon,
};

export function IdpIcon({ type, className }: { type: IdentityProviderType; className?: string }) {
    const Icon = icons[type] ?? OidcIcon;
    return <Icon className={className} />;
}
