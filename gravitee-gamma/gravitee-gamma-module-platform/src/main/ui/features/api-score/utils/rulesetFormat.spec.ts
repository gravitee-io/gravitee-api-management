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
    functionFileNameErrors,
    isValidFunctionFileName,
    isValidRulesetName,
    resolveImportedRulesetFormat,
    rulesetFormatLabel,
    rulesetNameError,
} from './rulesetFormat';
import { GRAVITEE_API_DEFINITION } from '../types/rulesets';

describe('rulesetFormatLabel', () => {
    it('matches Classic Console badge copy', () => {
        expect(rulesetFormatLabel('OPENAPI')).toBe('OpenAPI');
        expect(rulesetFormatLabel('ASYNCAPI')).toBe('AsyncAPI');
        expect(rulesetFormatLabel('GRAVITEE_PROXY')).toBe('Gravitee Proxy API');
        expect(rulesetFormatLabel('GRAVITEE_MESSAGE')).toBe('Gravitee Message API');
        expect(rulesetFormatLabel('GRAVITEE_NATIVE')).toBe('Gravitee Native API');
        expect(rulesetFormatLabel('GRAVITEE_FEDERATION')).toBe('Gravitee Federated API');
        expect(rulesetFormatLabel('GRAVITEE_V2')).toBe('Gravitee V2 API');
        expect(rulesetFormatLabel(undefined)).toBe('API');
    });
});

describe('resolveImportedRulesetFormat', () => {
    it('uses the OpenAPI or AsyncAPI card as the MAPI format', () => {
        expect(resolveImportedRulesetFormat('OPENAPI', '')).toBe('OPENAPI');
        expect(resolveImportedRulesetFormat('ASYNCAPI', '')).toBe('ASYNCAPI');
    });

    it('uses the nested Gravitee card once Gravitee API is selected', () => {
        expect(resolveImportedRulesetFormat(GRAVITEE_API_DEFINITION, 'GRAVITEE_PROXY')).toBe('GRAVITEE_PROXY');
        expect(resolveImportedRulesetFormat(GRAVITEE_API_DEFINITION, '')).toBeUndefined();
    });
});

describe('isValidRulesetName', () => {
    it('requires 1–50 characters after trim', () => {
        expect(isValidRulesetName('')).toBe(false);
        expect(isValidRulesetName('   ')).toBe(false);
        expect(isValidRulesetName('a')).toBe(true);
        expect(isValidRulesetName('a'.repeat(50))).toBe(true);
        expect(isValidRulesetName('a'.repeat(51))).toBe(false);
    });
});

describe('rulesetNameError', () => {
    it('uses Console import vs edit nouns', () => {
        expect(rulesetNameError('', 'Ruleset name')).toBe('Ruleset name is required.');
        expect(rulesetNameError('   ', 'Name')).toBe('Name is required.');
        expect(rulesetNameError('a'.repeat(51), 'Ruleset name')).toBe('Ruleset name can not exceed 50 characters.');
        expect(rulesetNameError('Style', 'Name')).toBeNull();
    });
});

describe('functionFileNameErrors', () => {
    it('uses Console copy derived from FUNCTION_NAME_PATTERN and FUNCTION_NAME_MAX', () => {
        expect(functionFileNameErrors('checkTag.js')).toEqual([]);
        expect(functionFileNameErrors('path/checkTag.js')).toEqual(['File name should fulfill ^[^/]+\\.js$ pattern']);
        expect(functionFileNameErrors(`${'a'.repeat(48)}.js`)).toEqual(['File name can not exceed 50 characters.']);
    });
});

describe('isValidFunctionFileName', () => {
    it('requires a .js name without slashes, max 50 characters', () => {
        expect(isValidFunctionFileName('checkTag.js')).toBe(true);
        expect(isValidFunctionFileName('path/checkTag.js')).toBe(false);
        expect(isValidFunctionFileName('checkTag.ts')).toBe(false);
        expect(isValidFunctionFileName(`${'a'.repeat(48)}.js`)).toBe(false);
    });
});
