/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { parsePropertiesStringFormat } from './propertiesParser';

describe('PropertiesValidator', () => {
  describe('without error', () => {
    it('simple var', () => {
      const rawInput = 'NAME_A=AAA';
      expect(parsePropertiesStringFormat(rawInput)).toEqual({
        properties: [{ key: 'NAME_A', value: `AAA` }],
        errors: [],
      });
    });

    it('multiple vars', () => {
      const rawInput = ['NAME_A=AAA', '   NAME_B=BBBB', 'NAME_C=CCCCC'].join('\n');
      expect(parsePropertiesStringFormat(rawInput)).toEqual({
        properties: [
          { key: 'NAME_A', value: 'AAA' },
          { key: 'NAME_B', value: 'BBBB' },
          { key: 'NAME_C', value: 'CCCCC' },
        ],
        errors: [],
      });
    });

    it('accept empty values', () => {
      const rawInput = ['NAME_A=AAA', 'NAME_B=', 'NAME_C='].join('\n');
      expect(parsePropertiesStringFormat(rawInput)).toEqual({
        properties: [
          { key: 'NAME_A', value: 'AAA' },
          { key: 'NAME_B', value: '' },
          { key: 'NAME_C', value: '' },
        ],
        errors: [],
      });
    });

    // We don't consider these as 2 values
    it('accept space in values', () => {
      const rawInput = ['NAME_A=AAA', 'NAME_B=BBBB BBBB', 'NAME_C=CCCCC'].join('\n');
      expect(parsePropertiesStringFormat(rawInput)).toEqual({
        properties: [
          { key: 'NAME_A', value: 'AAA' },
          { key: 'NAME_B', value: 'BBBB BBBB' },
          { key: 'NAME_C', value: 'CCCCC' },
        ],
        errors: [],
      });
    });

    it('accept simple quotes in values', () => {
      const rawInput = ['NAME_A=AAA', `NAME_B=BBBB'BBBB`, 'NAME_C=CCCCC'].join('\n');
      expect(parsePropertiesStringFormat(rawInput)).toEqual({
        properties: [
          { key: 'NAME_A', value: 'AAA' },
          { key: 'NAME_B', value: `BBBB'BBBB` },
          { key: 'NAME_C', value: 'CCCCC' },
        ],
        errors: [],
      });
    });

    it('accept double quotes in values', () => {
      const rawInput = ['NAME_A=AAA', `NAME_B=BBBB"BBBB`, 'NAME_C=CCCCC'].join('\n');
      expect(parsePropertiesStringFormat(rawInput)).toEqual({
        properties: [
          { key: 'NAME_A', value: 'AAA' },
          { key: 'NAME_B', value: `BBBB"BBBB` },
          { key: 'NAME_C', value: 'CCCCC' },
        ],
        errors: [],
      });
    });

    // We consider = as part of the value
    it('accept = in values', () => {
      const rawInput = ['NAME_A=AAA', 'NAME_B=BBBB OTHER=BBBB', 'NAME_C=CCCCC'].join('\n');
      expect(parsePropertiesStringFormat(rawInput)).toEqual({
        properties: [
          { key: 'NAME_A', value: 'AAA' },
          { key: 'NAME_B', value: 'BBBB OTHER=BBBB' },
          { key: 'NAME_C', value: 'CCCCC' },
        ],
        errors: [],
      });
    });

    it('ignore lines starting with comments', () => {
      const rawInput = ['NAME_A=AAA', '#NAME_B=BBBB', 'NAME_C=CCCCC'].join('\n');
      expect(parsePropertiesStringFormat(rawInput)).toEqual({
        properties: [
          { key: 'NAME_A', value: 'AAA' },
          { key: 'NAME_C', value: 'CCCCC' },
        ],
        errors: [],
      });
    });

    it('ignore empty lines', () => {
      const rawInput = ['', 'NAME_A=AAA', '', 'NAME_B=BBBB', 'NAME_C=CCCCC', ''].join('\n');
      expect(parsePropertiesStringFormat(rawInput)).toEqual({
        properties: [
          { key: 'NAME_A', value: 'AAA' },
          { key: 'NAME_B', value: 'BBBB' },
          { key: 'NAME_C', value: 'CCCCC' },
        ],
        errors: [],
      });
    });

    it('line breaks must be quoted (simple)', () => {
      const rawInput = [`NAME_A='A\na\nA'`, `NAME_B=BBBB`].join('\n');
      expect(parsePropertiesStringFormat(rawInput)).toEqual({
        properties: [
          { key: 'NAME_A', value: `A\na\nA` },
          { key: 'NAME_B', value: 'BBBB' },
        ],
        errors: [],
      });
    });

    it('line breaks must be quoted (double)', () => {
      const rawInput = [`NAME_A="A\na\nA"`, `NAME_B=BBBB`].join('\n');
      expect(parsePropertiesStringFormat(rawInput)).toEqual({
        properties: [
          { key: 'NAME_A', value: `A\na\nA` },
          { key: 'NAME_B', value: 'BBBB' },
        ],
        errors: [],
      });
    });

    it('simple quotes must be escaped in simple quotes', () => {
      const rawInput = [`NAME_A='AAA'`, `NAME_B='BBBB \\' BBBB'`, `NAME_C='CCCCC \\'CCCCC\\' CCCCC'`].join('\n');
      expect(parsePropertiesStringFormat(rawInput)).toEqual({
        properties: [
          { key: 'NAME_A', value: `AAA` },
          { key: 'NAME_B', value: `BBBB ' BBBB` },
          { key: 'NAME_C', value: `CCCCC 'CCCCC' CCCCC` },
        ],
        errors: [],
      });
    });

    it('double quotes must be escaped in double quotes', () => {
      const rawInput = [`NAME_A="AAA"`, `NAME_B="BBBB \\" BBBB"`, `NAME_C="CCCCC \\"CCCCC\\" CCCCC"`].join('\n');
      expect(parsePropertiesStringFormat(rawInput)).toEqual({
        properties: [
          { key: 'NAME_A', value: `AAA` },
          { key: 'NAME_B', value: `BBBB " BBBB` },
          { key: 'NAME_C', value: `CCCCC "CCCCC" CCCCC` },
        ],
        errors: [],
      });
    });

    it('understand multiple escaping', () => {
      // \" => "
      // \\\" => \"
      // \\\\\\" => \\"
      // \\\\\\\\" => \\\"
      // ...
      const rawInput = [
        // AAA \\\" AAA
        `NAME_A="AAA \\\\\\" AAA"`,
        // BBBB \\\\\"BBBB\\\\\" BBBB
        `NAME_B="BBBB \\\\\\\\\\"BBBB\\\\\\\\\\" BBBB"`,
        // CCCCC \\\\\\\"CCCCC\\\\\\\" CCCCC
        `NAME_C="CCCCC \\\\\\\\\\\\\\"CCCCC\\\\\\\\\\\\\\" CCCCC"`,
      ].join('\n');
      expect(parsePropertiesStringFormat(rawInput)).toEqual({
        properties: [
          // AAA \" AAA
          { key: 'NAME_A', value: `AAA \\\" AAA` }, // eslint-disable-line no-useless-escape
          // BBBB \\"BBBB\\" BBBB
          { key: 'NAME_B', value: `BBBB \\\\"BBBB\\\\" BBBB` },
          // CCCCC \\\"CCCCC\\\" CCCCC
          { key: 'NAME_C', value: `CCCCC \\\\\\"CCCCC\\\\\\" CCCCC` },
        ],
        errors: [],
      });
    });

    it('understand quoted slash+n as 2 different characters', () => {
      const rawInput = [
        'NAME_A="AAA\nAAA"',
        // BBBB\nBBBB
        `NAME_B="BBBB\\nBBBB"`,
      ].join('\n');
      expect(parsePropertiesStringFormat(rawInput)).toEqual({
        properties: [
          // AAA
          // AAA
          { key: 'NAME_A', value: `AAA\nAAA` },
          // BBBB\nBBBB
          { key: 'NAME_B', value: `BBBB\\nBBBB` },
        ],
        errors: [],
      });
    });
  });

  describe('with errors', () => {
    it('duplicated variable names', () => {
      const rawInput = ['NAME_A=AAA', 'NAME_A=aaa'].join('\n');
      expect(parsePropertiesStringFormat(rawInput)).toEqual({
        properties: [{ key: 'NAME_A', value: 'AAA' }],
        errors: ["Line 2 is not valid. Key 'NAME_A' is duplicated"],
      });
    });

    it('line without =', () => {
      const rawInput = ['NAME_A=A', 'AA', 'NAME_B=BBBB'].join('\n');
      expect(parsePropertiesStringFormat(rawInput)).toEqual({
        properties: [
          { key: 'NAME_A', value: 'A' },
          { key: 'NAME_B', value: 'BBBB' },
        ],
        errors: ["Line 2 is not valid. It must contain '='"],
      });
    });

    it('line without key', () => {
      const rawInput = ['NAME_A=A', '=AA', 'NAME_B=BBBB'].join('\n');
      expect(parsePropertiesStringFormat(rawInput)).toEqual({
        properties: [
          { key: 'NAME_A', value: 'A' },
          { key: 'NAME_B', value: 'BBBB' },
        ],
        errors: ["Line 2 is not valid. Key can't be empty"],
      });
    });
  });
});
