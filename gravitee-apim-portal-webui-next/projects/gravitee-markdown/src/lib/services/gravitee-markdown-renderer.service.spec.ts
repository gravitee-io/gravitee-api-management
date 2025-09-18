/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { TestBed } from '@angular/core/testing';

import { GraviteeMarkdownRendererService } from './gravitee-markdown-renderer.service';

describe('GraviteeMarkdownRendererService', () => {
  let service: GraviteeMarkdownRendererService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [GraviteeMarkdownRendererService],
    });
    service = TestBed.inject(GraviteeMarkdownRendererService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('Basic Markdown Rendering', () => {
    const testCases = [
      {
        description: 'should render simple heading',
        input: '# Hello World',
        expected: '<h1 id="hello-world">Hello World</h1>\n',
      },
      {
        description: 'should render paragraph',
        input: 'This is a simple paragraph.',
        expected: '<p>This is a simple paragraph.</p>\n',
      },
      {
        description: 'should render bold text',
        input: 'This is **bold** text.',
        expected: '<p>This is <strong>bold</strong> text.</p>\n',
      },
      {
        description: 'should render italic text',
        input: 'This is *italic* text.',
        expected: '<p>This is <em>italic</em> text.</p>\n',
      },
      {
        description: 'should render code block',
        input: '```\nconst x = 42;\n```',
        expected: '<pre><code>const x = 42;\n</code></pre>\n',
      },
      {
        description: 'should render inline code',
        input: 'Use `console.log()` to debug.',
        expected: '<p>Use <code>console.log()</code> to debug.</p>\n',
      },
      {
        description: 'should render unordered list',
        input: '- Item 1\n- Item 2\n- Item 3',
        expected: '<ul>\n<li>Item 1</li>\n<li>Item 2</li>\n<li>Item 3</li>\n</ul>\n',
      },
      {
        description: 'should render ordered list',
        input: '1. First item\n2. Second item\n3. Third item',
        expected: '<ol>\n<li>First item</li>\n<li>Second item</li>\n<li>Third item</li>\n</ol>\n',
      },
    ];

    testCases.forEach(({ description, input, expected }) => {
      it(description, () => {
        const result = service.render(input);
        expect(result).toBe(expected);
      });
    });
  });

  describe('Link Rendering', () => {
    const linkTestCases = [
      {
        description: 'should render anchor links with anchor class',
        input: '[Go to section](#section)',
        expected: '<p><a class="anchor" href="#section">Go to section</a></p>\n',
      },
      {
        description: 'should render internal links without hash prefix',
        input: '[Go to page](/#!/catalog/api/123)',
        expected: '<p><a href="/catalog/api/123">Go to page</a></p>\n',
      },
      {
        description: 'should render external links normally',
        input: '[External link](https://example.com)',
        expected: '<p><a href="https://example.com">External link</a></p>\n',
      },
      {
        description: 'should render relative links normally',
        input: '[Relative link](./page.html)',
        expected: '<p><a href="./page.html">Relative link</a></p>\n',
      },
    ];

    linkTestCases.forEach(({ description, input, expected }) => {
      it(description, () => {
        const result = service.render(input);
        expect(result).toBe(expected);
      });
    });
  });

  describe('GMD Blocks Preprocessing', () => {
    it('should process gmd-block elements', () => {
      const htmlInput = `
        <div>
          <h1>Regular HTML</h1>
          <gmd-md>
            # Markdown Title
            This is **markdown** content.
          </gmd-md>
          <p>More HTML content</p>
        </div>
      `;

      const result = service.preprocessGmdBlocks(htmlInput);

      expect(result).toContain(`<div>
          <h1>Regular HTML</h1>
          <gmd-md><h1 id="markdown-title">Markdown Title</h1>
<p>This is <strong>markdown</strong> content.</p>
</gmd-md>
          <p>More HTML content</p>
        </div>`);
    });

    it('should handle multiple gmd-block elements', () => {
      const htmlInput = `
        <div>
          <gmd-md>
            # First Block
            Content 1
          </gmd-md>
          <gmd-md>
            # Second Block
            Content 2
          </gmd-md>
        </div>
      `;

      const result = service.preprocessGmdBlocks(htmlInput);

      expect(result).toContain('<h1 id="first-block">First Block</h1>');
      expect(result).toContain('<p>Content 1</p>');
      expect(result).toContain('<h1 id="second-block">Second Block</h1>');
      expect(result).toContain('<p>Content 2</p>');
    });

    it('should handle empty gmd-block elements', () => {
      const htmlInput = `
        <div>
          <gmd-md></gmd-md>
          <gmd-md>
            # Non-empty block
            Content here
          </gmd-md>
        </div>
      `;

      const result = service.preprocessGmdBlocks(htmlInput);

      expect(result).toContain('<gmd-md></gmd-md>');
      expect(result).toContain('<h1 id="non-empty-block">Non-empty block</h1>');
      expect(result).toContain('<p>Content here</p>');
    });

    it('should normalize indentation within gmd-blocks', () => {
      const htmlInput = `
        <div>
          <gmd-md>
                # Indented Title

                    This is indented content.

                > This is a blockquote
          </gmd-md>
        </div>
      `;

      const result = service.preprocessGmdBlocks(htmlInput);

      // The indentation should be normalized, so the content should be properly formatted
      expect(result).toContain(`<div>
          <gmd-md><h1 id="indented-title">Indented Title</h1>
<pre><code>This is indented content.
</code></pre>
<blockquote>
<p>This is a blockquote</p>
</blockquote>
</gmd-md>
        </div>`);
    });
  });

  describe('Complex Markdown Scenarios', () => {
    const complexTestCases = [
      {
        description: 'should handle nested lists',
        input: `1. First item
   - Nested item 1
   - Nested item 2
2. Second item
   - Another nested item`,
        expected: `<ol>
<li>First item<ul>
<li>Nested item 1</li>
<li>Nested item 2</li>
</ul>
</li>
<li>Second item<ul>
<li>Another nested item</li>
</ul>
</li>
</ol>
`,
      },
      {
        description: 'should handle code blocks with language specification',
        input: '```javascript\nconst x = 42;\nconsole.log(x);\n```',
        expected: '<pre><code class="language-javascript">const x = 42;\nconsole.log(x);\n</code></pre>\n',
      },
      {
        description: 'should handle horizontal rules',
        input: 'Content above\n\n---\n\nContent below',
        expected: '<p>Content above</p>\n<hr>\n<p>Content below</p>\n',
      },
    ];

    complexTestCases.forEach(({ description, input, expected }) => {
      it(description, () => {
        const result = service.render(input);
        expect(result).toBe(expected);
      });
    });
  });

  describe('Edge Cases', () => {
    it('should handle empty string', () => {
      const result = service.render('');
      expect(result).toBe('');
    });

    it('should handle string with only whitespace', () => {
      const result = service.render('   \n  \t  \n  ');
      expect(result).toBe('');
    });

    it('should handle malformed markdown gracefully', () => {
      const input = '**Unclosed bold\n*Unclosed italic';
      const result = service.render(input);
      expect(result).toContain('**Unclosed bold');
      expect(result).toContain('*Unclosed italic');
    });

    it('should handle special characters in headings', () => {
      const input = '# Heading with @#$%^&*() characters';
      const result = service.render(input);
      expect(result).toContain('<h1 id="heading-with--characters">Heading with @#$%^&amp;*() characters</h1>');
    });
  });
});
