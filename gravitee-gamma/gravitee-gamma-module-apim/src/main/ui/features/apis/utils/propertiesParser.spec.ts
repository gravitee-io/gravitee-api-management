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
import { parsePropertiesStringFormat } from './propertiesParser';

describe('parsePropertiesStringFormat', () => {
    describe('valid input', () => {
        it('parses a single property', () => {
            expect(parsePropertiesStringFormat('NAME_A=AAA')).toEqual({
                properties: [{ key: 'NAME_A', value: 'AAA' }],
                errors: [],
            });
        });

        it('parses multiple properties', () => {
            const input = ['NAME_A=AAA', '   NAME_B=BBBB', 'NAME_C=CCCCC'].join('\n');
            expect(parsePropertiesStringFormat(input)).toEqual({
                properties: [
                    { key: 'NAME_A', value: 'AAA' },
                    { key: 'NAME_B', value: 'BBBB' },
                    { key: 'NAME_C', value: 'CCCCC' },
                ],
                errors: [],
            });
        });

        it('accepts empty values', () => {
            const input = ['NAME_A=AAA', 'NAME_B=', 'NAME_C='].join('\n');
            expect(parsePropertiesStringFormat(input)).toEqual({
                properties: [
                    { key: 'NAME_A', value: 'AAA' },
                    { key: 'NAME_B', value: '' },
                    { key: 'NAME_C', value: '' },
                ],
                errors: [],
            });
        });

        it('accepts spaces in values', () => {
            const input = ['NAME_A=AAA', 'NAME_B=BBBB BBBB', 'NAME_C=CCCCC'].join('\n');
            expect(parsePropertiesStringFormat(input)).toEqual({
                properties: [
                    { key: 'NAME_A', value: 'AAA' },
                    { key: 'NAME_B', value: 'BBBB BBBB' },
                    { key: 'NAME_C', value: 'CCCCC' },
                ],
                errors: [],
            });
        });

        it('accepts = in values (only first = is the separator)', () => {
            const input = ['NAME_A=AAA', 'NAME_B=BBBB OTHER=BBBB', 'NAME_C=CCCCC'].join('\n');
            expect(parsePropertiesStringFormat(input)).toEqual({
                properties: [
                    { key: 'NAME_A', value: 'AAA' },
                    { key: 'NAME_B', value: 'BBBB OTHER=BBBB' },
                    { key: 'NAME_C', value: 'CCCCC' },
                ],
                errors: [],
            });
        });

        it('ignores comment lines', () => {
            const input = ['NAME_A=AAA', '#NAME_B=BBBB', 'NAME_C=CCCCC'].join('\n');
            expect(parsePropertiesStringFormat(input)).toEqual({
                properties: [
                    { key: 'NAME_A', value: 'AAA' },
                    { key: 'NAME_C', value: 'CCCCC' },
                ],
                errors: [],
            });
        });

        it('ignores empty lines', () => {
            const input = ['', 'NAME_A=AAA', '', 'NAME_B=BBBB', 'NAME_C=CCCCC', ''].join('\n');
            expect(parsePropertiesStringFormat(input)).toEqual({
                properties: [
                    { key: 'NAME_A', value: 'AAA' },
                    { key: 'NAME_B', value: 'BBBB' },
                    { key: 'NAME_C', value: 'CCCCC' },
                ],
                errors: [],
            });
        });

        it('returns empty result for empty input', () => {
            expect(parsePropertiesStringFormat('')).toEqual({ properties: [], errors: [] });
        });

        it('parses multiline value wrapped in single quotes', () => {
            const input = [`NAME_A='A\na\nA'`, `NAME_B=BBBB`].join('\n');
            expect(parsePropertiesStringFormat(input)).toEqual({
                properties: [
                    { key: 'NAME_A', value: `A\na\nA` },
                    { key: 'NAME_B', value: 'BBBB' },
                ],
                errors: [],
            });
        });

        it('parses multiline value wrapped in double quotes', () => {
            const input = [`NAME_A="A\na\nA"`, `NAME_B=BBBB`].join('\n');
            expect(parsePropertiesStringFormat(input)).toEqual({
                properties: [
                    { key: 'NAME_A', value: `A\na\nA` },
                    { key: 'NAME_B', value: 'BBBB' },
                ],
                errors: [],
            });
        });

        it('unescapes single quotes inside single-quoted value', () => {
            const input = [`NAME_A='AAA'`, `NAME_B='BBBB \\' BBBB'`, `NAME_C='CCCCC \\'CCCCC\\' CCCCC'`].join('\n');
            expect(parsePropertiesStringFormat(input)).toEqual({
                properties: [
                    { key: 'NAME_A', value: `AAA` },
                    { key: 'NAME_B', value: `BBBB ' BBBB` },
                    { key: 'NAME_C', value: `CCCCC 'CCCCC' CCCCC` },
                ],
                errors: [],
            });
        });

        it('unescapes double quotes inside double-quoted value', () => {
            const input = [`NAME_A="AAA"`, `NAME_B="BBBB \\" BBBB"`, `NAME_C="CCCCC \\"CCCCC\\" CCCCC"`].join('\n');
            expect(parsePropertiesStringFormat(input)).toEqual({
                properties: [
                    { key: 'NAME_A', value: `AAA` },
                    { key: 'NAME_B', value: `BBBB " BBBB` },
                    { key: 'NAME_C', value: `CCCCC "CCCCC" CCCCC` },
                ],
                errors: [],
            });
        });

        it('preserves escaped \\n as a literal backslash-n (not a newline)', () => {
            const input = ['NAME_A="AAA\nAAA"', `NAME_B="BBBB\\nBBBB"`].join('\n');
            expect(parsePropertiesStringFormat(input)).toEqual({
                properties: [
                    { key: 'NAME_A', value: `AAA\nAAA` },
                    { key: 'NAME_B', value: `BBBB\\nBBBB` },
                ],
                errors: [],
            });
        });
    });

    describe('CRLF line endings', () => {
        it('handles CRLF line endings the same as LF', () => {
            // trim() on key/value strips the \r left by CRLF splitting on \n
            const input = 'NAME_A=AAA\r\nNAME_B=BBBB\r\n';
            expect(parsePropertiesStringFormat(input)).toEqual({
                properties: [
                    { key: 'NAME_A', value: 'AAA' },
                    { key: 'NAME_B', value: 'BBBB' },
                ],
                errors: [],
            });
        });
    });

    describe('error cases', () => {
        it('reports duplicate keys', () => {
            const input = ['NAME_A=AAA', 'NAME_A=aaa'].join('\n');
            expect(parsePropertiesStringFormat(input)).toEqual({
                properties: [{ key: 'NAME_A', value: 'AAA' }],
                errors: ["Line 2 is not valid. Key 'NAME_A' is duplicated"],
            });
        });

        it('reports lines without =', () => {
            const input = ['NAME_A=A', 'AA', 'NAME_B=BBBB'].join('\n');
            expect(parsePropertiesStringFormat(input)).toEqual({
                properties: [
                    { key: 'NAME_A', value: 'A' },
                    { key: 'NAME_B', value: 'BBBB' },
                ],
                errors: ["Line 2 is not valid. It must contain '='"],
            });
        });

        it('reports lines with empty key', () => {
            const input = ['NAME_A=A', '=AA', 'NAME_B=BBBB'].join('\n');
            expect(parsePropertiesStringFormat(input)).toEqual({
                properties: [
                    { key: 'NAME_A', value: 'A' },
                    { key: 'NAME_B', value: 'BBBB' },
                ],
                errors: ["Line 2 is not valid. Key can't be empty"],
            });
        });
    });
});
