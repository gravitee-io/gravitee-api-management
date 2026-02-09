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
import { normalizeIndentation } from './indentation-normalizer';

describe('normalizeIndentation', () => {
  it.each([
    // [description, input, expected]
    ['should handle empty string', '', ''],
    ['should handle string with only whitespace', '   \n  \n  ', ''],
    ['should handle single line without indentation', 'Hello World', 'Hello World'],
    ['should handle single line with indentation', '    Hello World', 'Hello World'],
    [
      'should remove common indentation from multiple lines',
      `    Line 1
    Line 2
    Line 3`,
      `Line 1
Line 2
Line 3`,
    ],
    [
      'should handle mixed indentation levels',
      `    Line 1
        Line 2 with more indentation
    Line 3`,
      `Line 1
    Line 2 with more indentation
Line 3`,
    ],
    [
      'should remove leading and trailing empty lines',
      `

    Line 1
    Line 2

`,
      `Line 1
Line 2`,
    ],
    [
      'should preserve blockquote syntax with simple indentation',
      `    > This is a blockquote
    > with multiple lines`,
      `> This is a blockquote
> with multiple lines`,
    ],
    [
      'should preserve blockquote syntax with complex indentation',
      `    > Lorem Ipsum is simply dummy text
    > of the printing and typesetting industry.

    Regular paragraph text.

    1. List item 1
    2. List item 2`,
      `> Lorem Ipsum is simply dummy text
> of the printing and typesetting industry.

Regular paragraph text.

1. List item 1
2. List item 2`,
    ],
    [
      'should handle blockquotes with different indentation levels',
      `        > First blockquote line
        > Second blockquote line

        Regular paragraph with 4 spaces indentation.

            > Nested blockquote with 8 spaces
            > Another nested line

        Another regular paragraph.`,
      `> First blockquote line
> Second blockquote line

Regular paragraph with 4 spaces indentation.

    > Nested blockquote with 8 spaces
    > Another nested line

Another regular paragraph.`,
    ],
    [
      'should handle mixed content with blockquotes and regular text',
      `    # Title

    > This is a blockquote
    > with multiple lines

    Regular paragraph text.

    - List item 1
    - List item 2`,
      `# Title

> This is a blockquote
> with multiple lines

Regular paragraph text.

- List item 1
- List item 2`,
    ],
    [
      'should handle blockquotes with empty lines',
      `    > First line
    >
    > Second line after empty line`,
      `> First line
>
> Second line after empty line`,
    ],
    [
      'should handle blockquotes with only > character',
      `    > First line
    >
    > Third line`,
      `> First line
>
> Third line`,
    ],
    [
      'should handle tabs and spaces mixed',
      `\t    Line 1
\t    Line 2`,
      `Line 1
Line 2`,
    ],
    [
      'should handle blockquotes with tabs and spaces',
      `\t    > Blockquote with mixed indentation
\t    > Second line`,
      `> Blockquote with mixed indentation
> Second line`,
    ],
    ['should handle Windows line endings', `    Line 1\r\n    Line 2\r\n    Line 3`, `Line 1\nLine 2\nLine 3`],
    [
      'should handle complex real-world example',
      `    > Lorem Ipsum is simply dummy text of the printing and typesetting industry.

    Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book.
    It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged.
    It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages,
    and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.

    1. Lorem ipsum dolor sit amet
    2. Consectetur adipiscing elit
    3. Integer molestie lorem at massa`,
      `> Lorem Ipsum is simply dummy text of the printing and typesetting industry.

Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book.
It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged.
It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages,
and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.

1. Lorem ipsum dolor sit amet
2. Consectetur adipiscing elit
3. Integer molestie lorem at massa`,
    ],
    [
      'should handle edge case with only blockquotes',
      `    > Only blockquote
    > Another blockquote`,
      `> Only blockquote
> Another blockquote`,
    ],
    [
      'should handle edge case with no non-blockquote lines',
      `    > Blockquote 1
    > Blockquote 2
    > Blockquote 3`,
      `> Blockquote 1
> Blockquote 2
> Blockquote 3`,
    ],
    [
      'should handle blockquotes with leading spaces after >',
      `    >   Blockquote with spaces after >
    >   Another line with spaces`,
      `>   Blockquote with spaces after >
>   Another line with spaces`,
    ],
    [
      'should work',
      `
          # h1
          ## h2
          ### h3
          #### h4

          Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book.
          It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged.
          It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages,
          and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.
`,
      `# h1
## h2
### h3
#### h4

Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book.
It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged.
It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages,
and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.`,
    ],
  ])('%s', (_desc, input, expected) => {
    expect(normalizeIndentation(input)).toBe(expected);
  });
});
