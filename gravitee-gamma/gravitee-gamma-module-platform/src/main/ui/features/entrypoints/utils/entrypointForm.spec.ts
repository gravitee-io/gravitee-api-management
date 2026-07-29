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
    composeEntrypointValue,
    decomposeEntrypointValue,
    findDuplicateMapping,
    isEntrypointFormValid,
    isValidEntrypointHttpUrl,
    isValidKafkaDomain,
    isValidPort,
    KAFKA_DOMAIN_PLACEHOLDER,
} from './entrypointForm';
import type { EntrypointMappingRow } from '../types/entrypoint';

const ROW: EntrypointMappingRow = {
    id: 'ep-1',
    value: 'https://api.example.com',
    target: 'HTTP',
    targetLabel: 'HTTP',
    tags: ['prod'],
    tagsName: ['Production'],
    environmentIds: [],
    environmentNames: [],
};

describe('entrypointForm utils', () => {
    it('validates ports in range 1-65535', () => {
        expect(isValidPort('1')).toBe(true);
        expect(isValidPort('65535')).toBe(true);
        expect(isValidPort('0')).toBe(false);
        expect(isValidPort('65536')).toBe(false);
        expect(isValidPort('abc')).toBe(false);
    });

    it('requires kafka domain to include the apiHost placeholder', () => {
        expect(isValidKafkaDomain(`${KAFKA_DOMAIN_PLACEHOLDER}.kafka.local`)).toBe(true);
        expect(isValidKafkaDomain('kafka.local')).toBe(false);
    });

    it('composes and decomposes values per target', () => {
        expect(composeEntrypointValue('HTTP', { httpValue: ' https://x ', tcpPort: '', kafkaDomain: '', kafkaPort: '' })).toBe('https://x');
        expect(composeEntrypointValue('TCP', { httpValue: '', tcpPort: '4082', kafkaDomain: '', kafkaPort: '' })).toBe('4082');
        expect(
            composeEntrypointValue('KAFKA', {
                httpValue: '',
                tcpPort: '',
                kafkaDomain: `${KAFKA_DOMAIN_PLACEHOLDER}.example.com`,
                kafkaPort: '9092',
            }),
        ).toBe(`${KAFKA_DOMAIN_PLACEHOLDER}.example.com:9092`);

        expect(decomposeEntrypointValue('HTTP', 'https://x').httpValue).toBe('https://x');
        expect(decomposeEntrypointValue('TCP', '4082').tcpPort).toBe('4082');
        const kafka = decomposeEntrypointValue('KAFKA', `${KAFKA_DOMAIN_PLACEHOLDER}.example.com:9092`);
        expect(kafka.kafkaDomain).toBe(`${KAFKA_DOMAIN_PLACEHOLDER}.example.com`);
        expect(kafka.kafkaPort).toBe('9092');
    });

    it('validates HTTP entrypoint URLs like Classic type="url"', () => {
        expect(isValidEntrypointHttpUrl('https://gateway.example.com')).toBe(true);
        expect(isValidEntrypointHttpUrl('http://localhost:8082')).toBe(true);
        expect(isValidEntrypointHttpUrl('ioioo')).toBe(false);
        expect(isValidEntrypointHttpUrl('gateway.example.com')).toBe(false);
        expect(isValidEntrypointHttpUrl('ftp://example.com')).toBe(false);
    });

    it('marks forms valid only when required fields are present', () => {
        expect(isEntrypointFormValid('HTTP', { httpValue: 'https://x', tcpPort: '', kafkaDomain: '', kafkaPort: '' })).toBe(true);
        expect(isEntrypointFormValid('TCP', { httpValue: '', tcpPort: '99', kafkaDomain: '', kafkaPort: '' })).toBe(true);
        expect(
            isEntrypointFormValid('KAFKA', {
                httpValue: '',
                tcpPort: '',
                kafkaDomain: KAFKA_DOMAIN_PLACEHOLDER,
                kafkaPort: '9092',
            }),
        ).toBe(true);
        expect(isEntrypointFormValid('HTTP', { httpValue: '  ', tcpPort: '', kafkaDomain: '', kafkaPort: '' })).toBe(false);
        expect(isEntrypointFormValid('HTTP', { httpValue: 'ioioo', tcpPort: '', kafkaDomain: '', kafkaPort: '' })).toBe(false);
    });

    it('detects duplicate mappings with overlapping environments', () => {
        expect(findDuplicateMapping('HTTP', 'https://api.example.com', [], [ROW])).toEqual(ROW);
        expect(findDuplicateMapping('HTTP', 'https://api.example.com', [], [ROW], 'ep-1')).toBeUndefined();
        expect(findDuplicateMapping('TCP', '4082', [], [ROW])).toBeUndefined();
    });
});
