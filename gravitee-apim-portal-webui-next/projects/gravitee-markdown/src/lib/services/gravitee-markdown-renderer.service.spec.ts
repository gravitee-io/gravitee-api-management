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

    it('should preserve style and class attributes in gmd-md elements', () => {
      const htmlInput = `
        <div>
          <gmd-md style="color: red; font-size: 16px;">
            # Styled Title
            This is **styled** content.
          </gmd-md>
          <gmd-md class="custom-class">
            # Classed Title
            This is **classed** content.
          </gmd-md>
          <gmd-md style="background: blue;" class="styled-class">
            # Both Attributes
            This has both **style** and class.
          </gmd-md>
          <gmd-md>
            # Regular Title
            This is regular content.
          </gmd-md>
        </div>
      `;

      const result = service.preprocessGmdBlocks(htmlInput);

      expect(result).toContain('<gmd-md style="color: red; font-size: 16px;">');
      expect(result).toContain('<h1 id="styled-title">Styled Title</h1>');
      expect(result).toContain('<p>This is <strong>styled</strong> content.</p>');

      expect(result).toContain('<gmd-md class="custom-class">');
      expect(result).toContain('<h1 id="classed-title">Classed Title</h1>');
      expect(result).toContain('<p>This is <strong>classed</strong> content.</p>');

      expect(result).toContain('<gmd-md style="background: blue;" class="styled-class">');
      expect(result).toContain('<h1 id="both-attributes">Both Attributes</h1>');
      expect(result).toContain('<p>This has both <strong>style</strong> and class.</p>');

      expect(result).toContain('<gmd-md><h1 id="regular-title">Regular Title</h1>');
      expect(result).toContain('<p>This is regular content.</p>');
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
      {
        description: 'should handle nested GMD components correctly',
        input: `# Button Component Integration

This markdown editor demonstrates the **button component** integration.

<gmd-grid columns="3">
  <gmd-cell style="text-align: center;">
    <gmd-button appearance="filled" link="/save">Save</gmd-button>
  </gmd-cell>
  <gmd-cell style="text-align: center;">
    <gmd-button appearance="outlined" link="/preview">Preview</gmd-button>
  </gmd-cell>
  <gmd-cell style="text-align: center;">
    <gmd-button appearance="text" link="/cancel">Cancel</gmd-button>
  </gmd-cell>
</gmd-grid>

> All buttons use the token-based theming system.`,
        expected:
          '<h1 id="button-component-integration">Button Component Integration</h1>\n<p>This markdown editor demonstrates the <strong>button component</strong> integration.</p>\n<gmd-grid columns="3">\n  <gmd-cell style="text-align: center;">\n    <gmd-button appearance="filled" link="/save">Save</gmd-button>\n  </gmd-cell>\n  <gmd-cell style="text-align: center;">\n    <gmd-button appearance="outlined" link="/preview">Preview</gmd-button>\n  </gmd-cell>\n  <gmd-cell style="text-align: center;">\n    <gmd-button appearance="text" link="/cancel">Cancel</gmd-button>\n  </gmd-cell>\n</gmd-grid><blockquote>\n<p>All buttons use the token-based theming system.</p>\n</blockquote>\n',
      },
      {
        description: 'should handle deeply nested GMD components',
        input: `<gmd-card>
  <gmd-card-title>Card Title</gmd-card-title>
  <gmd-card-subtitle>Card Subtitle</gmd-card-subtitle>
  <gmd-grid columns="2">
    <gmd-cell>
      <gmd-button appearance="filled">Action 1</gmd-button>
    </gmd-cell>
    <gmd-cell>
      <gmd-button appearance="outlined">Action 2</gmd-button>
    </gmd-cell>
  </gmd-grid>
</gmd-card>`,
        expected:
          '<gmd-card>\n  <gmd-card-title>Card Title</gmd-card-title>\n  <gmd-card-subtitle>Card Subtitle</gmd-card-subtitle>\n  <gmd-grid columns="2">\n    <gmd-cell>\n      <gmd-button appearance="filled">Action 1</gmd-button>\n    </gmd-cell>\n    <gmd-cell>\n      <gmd-button appearance="outlined">Action 2</gmd-button>\n    </gmd-cell>\n  </gmd-grid>\n</gmd-card>',
      },
      {
        description: 'should handle nested components of the same type',
        input: `<gmd-card>
  <gmd-card-title>Parent Card</gmd-card-title>
  <gmd-card-subtitle>This card contains another card</gmd-card-subtitle>
  <gmd-card>
    <gmd-card-title>Child Card</gmd-card-title>
    <gmd-card-subtitle>Nested inside parent</gmd-card-subtitle>
    <gmd-button appearance="filled">Child Action</gmd-button>
  </gmd-card>
  <gmd-button appearance="outlined">Parent Action</gmd-button>
</gmd-card>`,
        expected:
          '<gmd-card>\n  <gmd-card-title>Parent Card</gmd-card-title>\n  <gmd-card-subtitle>This card contains another card</gmd-card-subtitle>\n  <gmd-card>\n    <gmd-card-title>Child Card</gmd-card-title>\n    <gmd-card-subtitle>Nested inside parent</gmd-card-subtitle>\n    <gmd-button appearance="filled">Child Action</gmd-button>\n  </gmd-card>\n  <gmd-button appearance="outlined">Parent Action</gmd-button>\n</gmd-card>',
      },
      {
        description: 'should normalize self-closing GMD form components to opening/closing format',
        input: `<gmd-input name="test" label="Test Input" type="text" required="true" />
<gmd-select name="test-select" label="Test Select" options='["Option 1", "Option 2"]' />
<gmd-textarea name="test-textarea" label="Test Textarea" placeholder="Enter text" />
<gmd-checkbox name="test-checkbox" label="Test Checkbox" required="true" />
<gmd-radio name="test-radio" label="Test Radio" options="option1,option2" />`,
        expected: `<gmd-input name="test" label="Test Input" type="text" required="true"></gmd-input>
<gmd-select name="test-select" label="Test Select" options="[&quot;Option 1&quot;, &quot;Option 2&quot;]"></gmd-select>
<gmd-textarea name="test-textarea" label="Test Textarea" placeholder="Enter text"></gmd-textarea>
<gmd-checkbox name="test-checkbox" label="Test Checkbox" required="true"></gmd-checkbox>
<gmd-radio name="test-radio" label="Test Radio" options="option1,option2"></gmd-radio>`,
      },
      {
        description: 'should handle images inside GMD components without parsing errors',
        input: `## Outside

<img src="https://supertails.com/cdn/shop/articles/360_f_681163919_71bp2aiyziip3l4j5mbphdxtipdtm2zh_e2c1dbbd-e3b0-4c7d-bc09-1ebff39513ef.jpg?v=1747293323"
                 alt="Sample image"
                 style="width: 100%; max-width: 500px; height: auto; border-radius: 8px;" />

<gmd-grid columns="1">
<gmd-cell>
        Inside
</gmd-cell>
  <gmd-cell>
    <img src="https://supertails.com/cdn/shop/articles/360_f_681163919_71bp2aiyziip3l4j5mbphdxtipdtm2zh_e2c1dbbd-e3b0-4c7d-bc09-1ebff39513ef.jpg?v=1747293323"
                 alt="Sample image"
                 >
  </gmd-cell>
</gmd-grid>

## Sample Content
- *Italic text* and **bold text**
- \`Inline code\`
- [Links](https://gravitee.io)`,
        expected:
          '<h2 id="outside">Outside</h2>\n<img src="https://supertails.com/cdn/shop/articles/360_f_681163919_71bp2aiyziip3l4j5mbphdxtipdtm2zh_e2c1dbbd-e3b0-4c7d-bc09-1ebff39513ef.jpg?v=1747293323" alt="Sample image" style="width: 100%; max-width: 500px; height: auto; border-radius: 8px;">\n\n<gmd-grid columns="1">\n<gmd-cell>\n        Inside\n</gmd-cell>\n  <gmd-cell>\n    <img src="https://supertails.com/cdn/shop/articles/360_f_681163919_71bp2aiyziip3l4j5mbphdxtipdtm2zh_e2c1dbbd-e3b0-4c7d-bc09-1ebff39513ef.jpg?v=1747293323" alt="Sample image">\n  </gmd-cell>\n</gmd-grid><h2 id="sample-content">Sample Content</h2>\n<ul>\n<li><em>Italic text</em> and <strong>bold text</strong></li>\n<li><code>Inline code</code></li>\n<li><a href="https://gravitee.io">Links</a></li>\n</ul>\n',
      },
      {
        description: 'should handle markdown with special characters at root level',
        input: `# Special Characters Test

> This is a blockquote at root level
> It spans multiple lines
> And should render properly

- List item with special characters: > < * #
- Another item with symbols: @ $ % ^ &

## Code with special characters
\`\`\`javascript
// This code contains special characters
const special = "> < * # @ $ % ^ &";
console.log(special);
\`\`\`

> Final blockquote to test parsing`,
        expected:
          '<h1 id="special-characters-test">Special Characters Test</h1>\n<blockquote>\n<p>This is a blockquote at root level<br>It spans multiple lines<br>And should render properly</p>\n</blockquote>\n<ul>\n<li>List item with special characters: &gt; &lt; * #</li>\n<li>Another item with symbols: @ $ % ^ &amp;</li>\n</ul>\n<h2 id="code-with-special-characters">Code with special characters</h2>\n<pre><code class="language-javascript">// This code contains special characters\nconst special = &quot;&amp;gt; &amp;lt; * # @ $ % ^ &amp;amp;&quot;;\nconsole.log(special);\n</code></pre>\n<blockquote>\n<p>Final blockquote to test parsing</p>\n</blockquote>\n',
      },
    ];

    complexTestCases.forEach(({ description, input, expected }) => {
      it(description, () => {
        const result = service.render(input);
        expect(result).toBe(expected);
      });
    });
  });

  describe('Form Components Normalization', () => {
    it('should normalize all self-closing form components', () => {
      const input = `<gmd-input name="test" />
<gmd-textarea name="test2" />
<gmd-select name="test3" />
<gmd-checkbox name="test4" />
<gmd-radio name="test5" />`;

      const result = service.render(input);

      // All self-closing components should be converted to opening/closing format
      expect(result).toContain('<gmd-input name="test"></gmd-input>');
      expect(result).toContain('<gmd-textarea name="test2"></gmd-textarea>');
      expect(result).toContain('<gmd-select name="test3"></gmd-select>');
      expect(result).toContain('<gmd-checkbox name="test4"></gmd-checkbox>');
      expect(result).toContain('<gmd-radio name="test5"></gmd-radio>');

      // Should not contain self-closing format
      expect(result).not.toContain('<gmd-input name="test" />');
      expect(result).not.toContain('<gmd-textarea name="test2" />');
      expect(result).not.toContain('<gmd-select name="test3" />');
      expect(result).not.toContain('<gmd-checkbox name="test4" />');
      expect(result).not.toContain('<gmd-radio name="test5" />');
    });

    it('should preserve attributes when normalizing self-closing components', () => {
      const input = `<gmd-input name="email" label="Email" placeholder="Enter email" required="true" fieldKey="consumer_email" />
<gmd-select name="env" label="Environment" options='["dev", "prod"]' fieldKey="environment" />`;

      const result = service.render(input);

      // Attributes should be preserved
      expect(result).toContain('name="email"');
      expect(result).toContain('label="Email"');
      expect(result).toContain('placeholder="Enter email"');
      expect(result).toContain('required="true"');
      // DOMParser converts attribute names to lowercase
      expect(result).toContain('fieldkey="consumer_email"');
      expect(result).toContain('fieldkey="environment"');

      // Components should be normalized
      expect(result).toContain('</gmd-input>');
      expect(result).toContain('</gmd-select>');
    });

    it('should handle self-closing components with various spacing', () => {
      const input = `<gmd-input name="test1"/>
<gmd-input name="test2" />
<gmd-input name="test3"  />
<gmd-input name="test4"   />`;

      const result = service.render(input);

      // All variations should be normalized
      expect(result).toContain('<gmd-input name="test1"></gmd-input>');
      expect(result).toContain('<gmd-input name="test2"></gmd-input>');
      expect(result).toContain('<gmd-input name="test3"></gmd-input>');
      expect(result).toContain('<gmd-input name="test4"></gmd-input>');
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
