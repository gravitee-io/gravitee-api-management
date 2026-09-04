/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import DOMPurify from 'dompurify';

/**
 * An agent's answer is third-party content, and the markdown viewer renders raw html verbatim,
 * so every tag is stripped before the renderer ever sees it. Markdown punctuation is plain text
 * to an html parser and survives; an injected form, input or button does not. The cost is that
 * html shown as a code sample loses its tags.
 */
export function markdownWithoutHtml(text: string): string {
  let current = text;
  let stripped = stripTags(current);
  // Stripping reads the text back out decoded, so a single pass hands `&lt;gmd-button&gt;` to the
  // viewer as live markup — the very tag it was asked to remove. Each pass either drops a tag or
  // decodes an entity, and both shorten the text, so repeating until it settles terminates.
  while (stripped !== current) {
    current = stripped;
    stripped = stripTags(current);
  }
  return stripped;
}

function stripTags(text: string): string {
  const fragment = DOMPurify.sanitize(text, {
    ALLOWED_TAGS: [],
    KEEP_CONTENT: true,
    RETURN_DOM_FRAGMENT: true,
  });
  return fragment.textContent ?? '';
}
