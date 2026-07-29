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
    buildEntrypointPortalSettingsPayload,
    CONFIG_PORT_MAX,
    CONFIG_PORT_MIN,
    isEntrypointConfigFieldReadonly,
    isEntrypointConfigFormDirty,
    isEntrypointConfigFormValid,
    isValidConfigPort,
    toEntrypointConfigFormValues,
    type EntrypointConfigFormValues,
} from './entrypointConfigForm';
import { KAFKA_DOMAIN_PLACEHOLDER } from './entrypointForm';
import type { EntrypointPortalSettings } from '../types/entrypoint';

const VALID_FORM: EntrypointConfigFormValues = {
    entrypoint: 'https://api.example.com',
    tcpPort: '4082',
    kafkaDomain: `${KAFKA_DOMAIN_PLACEHOLDER}.example.com`,
    kafkaPort: '9092',
};

const SETTINGS: EntrypointPortalSettings = {
    portal: {
        entrypoint: 'https://api.example.com',
        tcpPort: 4082,
        kafkaDomain: `${KAFKA_DOMAIN_PLACEHOLDER}.example.com`,
        kafkaPort: 9092,
        other: 'keep-me',
    },
    metadata: { readonly: ['portal.tcpPort'] },
    cors: { allowOrigin: ['*'] },
};

describe('entrypointConfigForm utils', () => {
    it('validates config ports in Classic range 1025-65535', () => {
        expect(isValidConfigPort(String(CONFIG_PORT_MIN))).toBe(true);
        expect(isValidConfigPort(String(CONFIG_PORT_MAX))).toBe(true);
        expect(isValidConfigPort('1024')).toBe(false);
        expect(isValidConfigPort('65536')).toBe(false);
        expect(isValidConfigPort('abc')).toBe(false);
        expect(isValidConfigPort('')).toBe(false);
    });

    it('maps portal settings into form values', () => {
        expect(toEntrypointConfigFormValues(SETTINGS)).toEqual(VALID_FORM);
        expect(toEntrypointConfigFormValues({})).toEqual({
            entrypoint: '',
            tcpPort: '',
            kafkaDomain: '',
            kafkaPort: '',
        });
    });

    it('detects system-readonly portal fields from metadata', () => {
        expect(isEntrypointConfigFieldReadonly(SETTINGS, 'tcpPort')).toBe(true);
        expect(isEntrypointConfigFieldReadonly(SETTINGS, 'entrypoint')).toBe(false);
        expect(isEntrypointConfigFieldReadonly(undefined, 'entrypoint')).toBe(false);
    });

    it('marks form valid only when ports and kafka domain are valid', () => {
        expect(isEntrypointConfigFormValid(VALID_FORM)).toBe(true);
        expect(isEntrypointConfigFormValid({ ...VALID_FORM, tcpPort: '80' })).toBe(false);
        expect(isEntrypointConfigFormValid({ ...VALID_FORM, kafkaDomain: 'missing-placeholder' })).toBe(false);
        expect(isEntrypointConfigFormValid({ ...VALID_FORM, kafkaPort: 'abc' })).toBe(false);
    });

    it('detects dirty form values', () => {
        expect(isEntrypointConfigFormDirty(VALID_FORM, VALID_FORM)).toBe(false);
        expect(isEntrypointConfigFormDirty({ ...VALID_FORM, tcpPort: '8888' }, VALID_FORM)).toBe(true);
        expect(isEntrypointConfigFormDirty({ ...VALID_FORM, entrypoint: 'https://x' }, VALID_FORM)).toBe(true);
    });

    it('merges edited portal defaults into the full settings document', () => {
        const payload = buildEntrypointPortalSettingsPayload(SETTINGS, {
            ...VALID_FORM,
            entrypoint: ' https://gateway.example.com ',
            tcpPort: '8888',
            kafkaDomain: ` ${KAFKA_DOMAIN_PLACEHOLDER}.new.org `,
            kafkaPort: '9093',
        });
        expect(payload).toEqual({
            portal: {
                entrypoint: 'https://gateway.example.com',
                tcpPort: 8888,
                kafkaDomain: `${KAFKA_DOMAIN_PLACEHOLDER}.new.org`,
                kafkaPort: 9093,
                other: 'keep-me',
            },
            metadata: { readonly: ['portal.tcpPort'] },
            cors: { allowOrigin: ['*'] },
        });
    });
});
