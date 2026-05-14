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

const LINE_BREAK = '\n';
const SINGLE_QUOTE = "'";
const DOUBLE_QUOTE = '"';
const EQUAL = '=';
const COMMENT = '#';

/**
 * Parse properties string format.
 * Format: KEY=value per line. Lines starting with '#' are comments.
 * Values may be single- or double-quoted to allow embedded newlines.
 */
export function parsePropertiesStringFormat(input: string): { properties: { key: string; value: string }[]; errors: string[] } {
    const defaultResult = { properties: [] as { key: string; value: string }[], errors: [] as string[] };
    if (!input) return defaultResult;

    const lines = splitToLine(input.trim()).filter(line => line.trim());
    const properties: { key: string; value: string }[] = [];
    const seenKeys = new Set<string>();
    const errors: string[] = [];

    for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        if (line.startsWith(COMMENT)) continue;

        const index = line.indexOf(EQUAL);
        if (index === -1) {
            errors.push(`Line ${i + 1} is not valid. It must contain '='`);
            continue;
        }
        const key = line.substring(0, index).trim();
        if (!key) {
            errors.push(`Line ${i + 1} is not valid. Key can't be empty`);
            continue;
        }
        if (seenKeys.has(key)) {
            errors.push(`Line ${i + 1} is not valid. Key '${key}' is duplicated`);
            continue;
        }
        seenKeys.add(key);
        const value = line.substring(index + 1).trim();
        properties.push({ key, value: removeUnnecessaryEscape(unquote(value)) });
    }

    return { properties, errors };
}

function splitToLine(input: string): string[] {
    const lines: string[] = [];
    let partialLine = '';
    let insideSingleQuote = false;
    let insideDoubleQuote = false;

    for (let i = 0; i < input.length; i++) {
        const char = input[i];
        const prev = i > 0 ? input[i - 1] : null;
        const next = i < input.length - 1 ? input[i + 1] : null;

        if ((prev && `${prev}${char}` === `${EQUAL}${SINGLE_QUOTE}`) || (next && `${char}${next}` === `${SINGLE_QUOTE}${LINE_BREAK}`)) {
            insideSingleQuote = !insideSingleQuote;
        }
        if ((prev && `${prev}${char}` === `${EQUAL}${DOUBLE_QUOTE}`) || (next && `${char}${next}` === `${DOUBLE_QUOTE}${LINE_BREAK}`)) {
            insideDoubleQuote = !insideDoubleQuote;
        }
        if (char === LINE_BREAK && !insideSingleQuote && !insideDoubleQuote) {
            lines.push(partialLine);
            partialLine = '';
            continue;
        }
        partialLine += char;
    }
    if (partialLine) lines.push(partialLine);

    return lines;
}

function unquote(value: string): string {
    const v = value.trim();
    if ((v.startsWith(SINGLE_QUOTE) && v.endsWith(SINGLE_QUOTE)) || (v.startsWith(DOUBLE_QUOTE) && v.endsWith(DOUBLE_QUOTE))) {
        return v.substring(1, v.length - 1);
    }
    return v;
}

function removeUnnecessaryEscape(value: string): string {
    const KEEP = 'GIO_KEEP_ESCAPE_LINE_BREAK';
    return value.replace(/\\n/g, KEEP).replace(/\\(.)/g, '$1').replace(new RegExp(KEEP, 'g'), '\\n');
}
