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
export function stripGaplComments(text: string): string {
    let out = '';
    let i = 0;
    while (i < text.length) {
        const c = text[i];

        if (c === '"') {
            out += c;
            i++;
            while (i < text.length) {
                const s = text[i];
                if (s === '\\' && i + 1 < text.length) {
                    out += s + text[i + 1];
                    i += 2;
                    continue;
                }
                out += s;
                i++;
                if (s === '"') break;
            }
            continue;
        }

        if (c === '/' && text[i + 1] === '/') {
            while (i < text.length && text[i] !== '\n') i++;
            continue;
        }

        if (c === '/' && text[i + 1] === '*') {
            i += 2;
            while (i < text.length) {
                if (text[i] === '\n') {
                    out += '\n';
                    i++;
                    continue;
                }
                if (text[i] === '*' && text[i + 1] === '/') {
                    i += 2;
                    break;
                }
                i++;
            }
            continue;
        }

        out += c;
        i++;
    }
    return out;
}
