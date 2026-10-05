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
import { validateIntegrationForm } from './integrationForm';

export interface A2aIntegrationFormValues {
    name: string;
    description: string;
    wellKnownUrls: string[];
}

export interface A2aIntegrationFormErrors {
    name?: string;
    description?: string;
    wellKnownUrls: (string | undefined)[];
    missingWellKnownUrls?: string;
}

interface HttpUrlParts {
    scheme: string;
    userinfo: string | undefined;
    host: string;
    port: string | undefined;
    rest: string;
}

const HTTP_URL_PATTERN = /^(https?):\/\/([^/?#]*)(.*)$/i;
const DEFAULT_PORTS: Record<string, string> = { http: '80', https: '443' };
const MAX_PORT = 65535;

function splitHostAndPort(hostPort: string): { host: string; port: string | undefined } {
    const portSeparator = hostPort.startsWith('[') ? hostPort.indexOf(':', hostPort.indexOf(']')) : hostPort.indexOf(':');
    if (portSeparator === -1) {
        return { host: hostPort, port: undefined };
    }
    return { host: hostPort.slice(0, portSeparator), port: hostPort.slice(portSeparator + 1) };
}

function isValidPort(port: string | undefined): boolean {
    return port === undefined || port === '' || (/^\d+$/.test(port) && Number(port) <= MAX_PORT);
}

// Split by hand rather than with `new URL(...)`: the WHATWG parser accepts `http:///foo` and rewrites
// paths, escapes and hosts, which the RFC 3986 section 6.2 comparison must not do.
function parseHttpUrl(value: string): HttpUrlParts | undefined {
    const match = HTTP_URL_PATTERN.exec(value);
    if (!match) {
        return undefined;
    }
    const [, scheme, authority, rest] = match;
    const userinfoSeparator = authority.lastIndexOf('@');
    const userinfo = userinfoSeparator === -1 ? undefined : authority.slice(0, userinfoSeparator);
    const { host, port } = splitHostAndPort(authority.slice(userinfoSeparator + 1));
    return { scheme, userinfo, host, port, rest };
}

export function isValidWellKnownUrl(value: string): boolean {
    if (/\s/.test(value)) {
        return false;
    }
    const parts = parseHttpUrl(value);
    return parts !== undefined && parts.host !== '' && isValidPort(parts.port);
}

export function normalizeWellKnownUrl(value: string): string {
    const parts = parseHttpUrl(value);
    if (!parts) {
        return value;
    }
    const scheme = parts.scheme.toLowerCase();
    const userinfo = parts.userinfo === undefined ? '' : `${parts.userinfo}@`;
    const port = parts.port === undefined || parts.port === '' || parts.port === DEFAULT_PORTS[scheme] ? '' : `:${parts.port}`;
    const rest = parts.rest === '' || /^[?#]/.test(parts.rest) ? `/${parts.rest}` : parts.rest;
    const normalized = `${scheme}://${userinfo}${parts.host.toLowerCase()}${port}${rest}`;
    return normalized.replace(/%[0-9a-f]{2}/gi, escape => escape.toUpperCase());
}

function validateWellKnownUrls(wellKnownUrls: string[]): (string | undefined)[] {
    const seen = new Set<string>();
    return wellKnownUrls.map(entry => {
        if (!isValidWellKnownUrl(entry)) {
            return 'Enter a valid http:// or https:// URL.';
        }
        const normalized = normalizeWellKnownUrl(entry);
        if (seen.has(normalized)) {
            return 'This URL is already in the list.';
        }
        seen.add(normalized);
        return undefined;
    });
}

export function validateA2aIntegrationForm({ name, description, wellKnownUrls }: A2aIntegrationFormValues): A2aIntegrationFormErrors {
    const errors: A2aIntegrationFormErrors = {
        ...validateIntegrationForm({ name, description }),
        wellKnownUrls: validateWellKnownUrls(wellKnownUrls),
    };
    if (name.trim() === '') {
        errors.name = 'Name is required.';
    }
    if (wellKnownUrls.length === 0) {
        errors.missingWellKnownUrls = 'Add at least one well-known URL.';
    }
    return errors;
}

export function isA2aIntegrationFormValid(errors: A2aIntegrationFormErrors): boolean {
    return !errors.name && !errors.description && !errors.missingWellKnownUrls && errors.wellKnownUrls.every(error => !error);
}
