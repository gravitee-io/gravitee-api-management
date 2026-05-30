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
import { describe, expect, it } from 'vitest';
import { coerce, inferType, isReservedKey, validateKey, policyFnHint } from '../attribute-codec';

describe('attribute-codec coerce', () => {
    it('integer → JS number, rejects non-integers and decimals', () => {
        expect(coerce('integer', '3')).toEqual({ ok: true, value: 3 });
        expect(coerce('integer', '-12')).toEqual({ ok: true, value: -12 });
        expect(coerce('integer', '3.5').ok).toBe(false);
        expect(coerce('integer', 'abc').ok).toBe(false);
    });

    it('decimal → STRING, never a JS number (PDP drops Doubles)', () => {
        const r = coerce('decimal', '3.5');
        expect(r).toEqual({ ok: true, value: '3.5' });
        expect(typeof (r as { value: unknown }).value).toBe('string');
        expect(coerce('decimal', 'x').ok).toBe(false);
    });

    it('boolean → true/false only', () => {
        expect(coerce('boolean', 'true')).toEqual({ ok: true, value: true });
        expect(coerce('boolean', 'FALSE')).toEqual({ ok: true, value: false });
        expect(coerce('boolean', 'yes').ok).toBe(false);
    });

    it('set → deduped string array, warns on duplicates', () => {
        const r = coerce('set', ['us', 'eu', 'us']);
        expect(r).toMatchObject({ ok: true, value: ['us', 'eu'] });
        expect((r as { warning?: string }).warning).toMatch(/collapsed/i);
    });

    it('string/enum pass through verbatim', () => {
        expect(coerce('string', 'engineering')).toEqual({ ok: true, value: 'engineering' });
        expect(coerce('enum', 'gold')).toEqual({ ok: true, value: 'gold' });
    });

    it('timestamp validates ISO-8601 and stores a string', () => {
        expect(coerce('timestamp', '2026-05-30T12:00:00Z')).toEqual({ ok: true, value: '2026-05-30T12:00:00Z' });
        expect(coerce('timestamp', '30/05/2026').ok).toBe(false);
    });

    it('ip validates IPv4/IPv6 and stores a string', () => {
        expect(coerce('ip', '10.0.0.1')).toEqual({ ok: true, value: '10.0.0.1' });
        expect(coerce('ip', '::1')).toEqual({ ok: true, value: '::1' });
        expect(coerce('ip', '2001:db8::1')).toEqual({ ok: true, value: '2001:db8::1' });
        expect(coerce('ip', 'fe80::1')).toEqual({ ok: true, value: 'fe80::1' });
        expect(coerce('ip', '999.1.1.1').ok).toBe(false);
    });

    it('cidr validates a network/mask and stores a string', () => {
        expect(coerce('cidr', '10.0.0.0/24')).toEqual({ ok: true, value: '10.0.0.0/24' });
        expect(coerce('cidr', '2001:db8::/32')).toEqual({ ok: true, value: '2001:db8::/32' });
        expect(coerce('cidr', '10.0.0.0').ok).toBe(false);
    });

    it('duration validates a unit form and stores a string', () => {
        expect(coerce('duration', '30s')).toEqual({ ok: true, value: '30s' });
        expect(coerce('duration', '5m')).toEqual({ ok: true, value: '5m' });
        expect(coerce('duration', 'soon').ok).toBe(false);
    });
});

describe('attribute-codec keys', () => {
    it('isReservedKey blocks underscore-prefix and id/type', () => {
        expect(isReservedKey('_kind')).toBe(true);
        expect(isReservedKey('id')).toBe(true);
        expect(isReservedKey('type')).toBe(true);
        expect(isReservedKey('department')).toBe(false);
    });

    it('validateKey enforces pattern, reserved, and duplicates', () => {
        expect(validateKey('dept', [])).toBeNull();
        expect(validateKey('', [])).toMatch(/required/i);
        expect(validateKey('1bad', [])).toMatch(/match/i);
        expect(validateKey('_x', [])).toMatch(/reserved/i);
        expect(validateKey('dept', ['dept'])).toMatch(/duplicate/i);
    });
});

describe('attribute-codec inferType + hints', () => {
    it('infers type from a stored value', () => {
        expect(inferType(true)).toBe('boolean');
        expect(inferType(7)).toBe('integer');
        expect(inferType(['a', 'b'])).toBe('set');
        expect(inferType('hello')).toBe('string');
    });

    it('exposes a policy function hint for string-backed types', () => {
        expect(policyFnHint('ip')).toBe('ip(...)');
        expect(policyFnHint('timestamp')).toBe('datetime(...)');
        expect(policyFnHint('string')).toBeUndefined();
    });
});
