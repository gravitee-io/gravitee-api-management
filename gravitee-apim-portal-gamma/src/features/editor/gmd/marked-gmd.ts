/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { marked } from 'marked';

let configured = false;

function configureMarked(): void {
    if (configured) {
        return;
    }

    marked.setOptions({
        breaks: true,
        gfm: true,
    });

    marked.use({
        extensions: [
            {
                name: 'gmd-any',
                level: 'block',
                start(src: string) {
                    return src.match(/<gmd-[a-z0-9-]*\b/i)?.index;
                },
                tokenizer(src: string) {
                    const openingGmdTagMatch = src.match(/^<gmd-([a-z0-9-]*)\b[^>]*>/i);
                    if (!openingGmdTagMatch) {
                        return undefined;
                    }

                    const tagName = openingGmdTagMatch[0].match(/^<(gmd-[a-z0-9-]*)/i)?.[1];
                    if (!tagName) {
                        return undefined;
                    }

                    const doc = new DOMParser().parseFromString(src, 'text/html');
                    const foundComponent = doc.body.querySelector(tagName);
                    if (!foundComponent) {
                        return undefined;
                    }

                    return {
                        type: 'gmd-any',
                        raw: foundComponent.outerHTML,
                    };
                },
                renderer(token) {
                    return token.raw;
                },
            },
        ],
    });

    configured = true;
}

export function gmdToHtml(gmd: string): string {
    configureMarked();
    return marked.parse(gmd, { async: false }) as string;
}
