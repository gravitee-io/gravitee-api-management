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

import {
    buildCorsFormStateFromConsoleSettings,
    buildCorsFormStateFromPortalSettings,
    buildCorsPatch,
    buildManagementCorsFieldReadonly,
    buildPortalCorsFieldReadonly,
    isCorsFormValid,
    parseCorsMaxAge,
} from './corsFormState';
import type { PortalSettings } from '../../security-plan-types/services/portalSettings';
import type { ConsoleSettings } from '../types/consoleSettings';

const CONSOLE_SETTINGS: ConsoleSettings = {
    cors: {
        allowOrigin: ['https://console.example.com'],
        allowMethods: ['GET'],
        allowHeaders: ['Authorization'],
        exposedHeaders: ['ETag'],
        maxAge: 1728000,
    },
};

const PORTAL_SETTINGS: PortalSettings = {
    cors: {
        allowOrigin: ['https://portal.example.com'],
        allowMethods: ['GET', 'POST'],
        maxAge: 60,
    },
};

describe('corsFormState', () => {
    it('builds form state from console and portal settings', () => {
        expect(buildCorsFormStateFromConsoleSettings(CONSOLE_SETTINGS)).toEqual({
            allowOrigin: ['https://console.example.com'],
            allowMethods: ['GET'],
            allowHeaders: ['Authorization'],
            exposedHeaders: ['ETag'],
            maxAge: '1728000',
        });
        expect(buildCorsFormStateFromPortalSettings(PORTAL_SETTINGS)).toEqual({
            allowOrigin: ['https://portal.example.com'],
            allowMethods: ['GET', 'POST'],
            allowHeaders: [],
            exposedHeaders: [],
            maxAge: '60',
        });
    });

    it('parses and validates max age', () => {
        expect(parseCorsMaxAge('60')).toBe(60);
        expect(parseCorsMaxAge('abc')).toBeNull();
        expect(isCorsFormValid({ ...buildCorsFormStateFromConsoleSettings(CONSOLE_SETTINGS), maxAge: 'abc' })).toBe(false);
        expect(isCorsFormValid(buildCorsFormStateFromConsoleSettings(CONSOLE_SETTINGS))).toBe(true);
    });

    it('maps management and portal readonly keys', () => {
        expect(
            buildManagementCorsFieldReadonly({
                ...CONSOLE_SETTINGS,
                metadata: { readonly: ['http.api.management.cors.allow-origin'] },
            }),
        ).toEqual({
            allowOrigin: true,
            allowMethods: false,
            allowHeaders: false,
            exposedHeaders: false,
            maxAge: false,
        });
        expect(
            buildPortalCorsFieldReadonly({
                ...PORTAL_SETTINGS,
                metadata: { readonly: ['http.api.portal.cors.max-age'] },
            }),
        ).toEqual({
            allowOrigin: false,
            allowMethods: false,
            allowHeaders: false,
            exposedHeaders: false,
            maxAge: true,
        });
    });

    it('builds a cors patch from local state', () => {
        expect(buildCorsPatch(buildCorsFormStateFromPortalSettings(PORTAL_SETTINGS))).toEqual({
            allowOrigin: ['https://portal.example.com'],
            allowMethods: ['GET', 'POST'],
            allowHeaders: [],
            exposedHeaders: [],
            maxAge: 60,
        });
    });
});
