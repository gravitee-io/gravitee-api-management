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
import { stripGaplComments } from '../strip-gapl-comments';

describe('stripGaplComments — line comments', () => {
    it('strips `// ...` to end of line', () => {
        expect(stripGaplComments('permit (); // trailing')).toBe('permit (); ');
    });

    it('keeps `//` inside a string literal', () => {
        expect(stripGaplComments('permit (principal == User::"a//b");')).toBe('permit (principal == User::"a//b");');
    });

    it('preserves blank lines so line numbers stay aligned', () => {
        const input = '// header\npermit ();\n// tail';
        const output = stripGaplComments(input);
        expect(output.split('\n')).toHaveLength(3);
    });
});

describe('stripGaplComments — block comments', () => {
    it('strips a single-line `/* ... */` block', () => {
        expect(stripGaplComments('permit /* note */ ();')).toBe('permit  ();');
    });

    it('strips a multi-line `/* ... */` block and keeps line breaks', () => {
        const input = 'permit /* a\nb\nc */ ();';
        const output = stripGaplComments(input);
        expect(output).toContain('permit');
        expect(output).toContain('();');
        expect(output.split('\n')).toHaveLength(3);
    });

    it('keeps `/*` inside a string literal', () => {
        const input = 'permit (resource == Doc::"/* not a comment */");';
        expect(stripGaplComments(input)).toBe(input);
    });

    it('treats unterminated `/*` as comment to end of input', () => {
        const input = 'permit (); /* never closed';
        expect(stripGaplComments(input)).toBe('permit (); ');
    });

    it('does not support nested block comments', () => {
        const input = 'a /* outer /* inner */ tail */ b';
        expect(stripGaplComments(input)).toBe('a  tail */ b');
    });

    it('handles a line comment followed by a block comment', () => {
        const input = '// header\n/* block */ permit ();';
        const output = stripGaplComments(input);
        expect(output).toBe('\n permit ();');
    });
});

describe('stripGaplComments — edge cases', () => {
    it('returns the empty string unchanged', () => {
        expect(stripGaplComments('')).toBe('');
    });

    it('handles escaped quotes inside strings', () => {
        const input = 'permit (resource == Doc::"he said \\"hi\\" // ok");';
        expect(stripGaplComments(input)).toBe(input);
    });
});
