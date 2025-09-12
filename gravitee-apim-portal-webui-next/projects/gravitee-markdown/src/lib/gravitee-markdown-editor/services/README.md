# Monaco Editor Suggestions

This service provides auto-completion suggestions for custom components in the Monaco Editor.

## Component-Specific Suggestions

Suggestions are organized by component in separate files:

### Grid Component Suggestions (`grid.suggestions.ts`)

- **`grid`** - Basic grid component with 2 columns
- **`grid-1-columns`** - Grid with 1 column
- **`grid-2-columns`** - Grid with 2 columns  
- **`grid-3-columns`** - Grid with 3 columns
- **`grid-4-columns`** - Grid with 4 columns
- **`grid-5-columns`** - Grid with 5 columns
- **`grid-6-columns`** - Grid with 6 columns

### Cell Component Suggestions (`cell.suggestions.ts`)

- **`cell`** - Basic cell component
- **`cell-with-heading`** - Cell with main heading (h2)
- **`cell-with-subheading`** - Cell with subheading (h3)
- **`cell-with-list`** - Cell with unordered list
- **`cell-with-ordered-list`** - Cell with ordered list
- **`cell-with-code`** - Cell with code block

## Usage

The suggestions are automatically registered when the Monaco Editor is initialized. Users can:

1. Type component names in the editor
2. Press `Ctrl+Space` (or `Cmd+Space` on Mac) to trigger suggestions
3. Select the desired suggestion from the dropdown
4. The snippet will be inserted with placeholder content

## Example

When you type `grid-3-columns` and select the suggestion, you'll get:

```html
<grid columns="3">
    <cell>
        <h3>Column 1</h3>
        <p>Content for column 1</p>
    </cell>
    <cell>
        <h3>Column 2</h3>
        <p>Content for column 2</p>
    </cell>
    <cell>
        <h3>Column 3</h3>
        <p>Content for column 3</p>
    </cell>
</grid>
```

## Adding New Suggestions

To add suggestions for new components:

1. Create a `<component-name>.suggestions.ts` file in the component directory
2. Define suggestions using the `ComponentSuggestion` interface
3. Export the suggestions array
4. Add the export to `suggestions.index.ts`
5. The service will automatically pick up the new suggestions

### Example Component Suggestions File

```typescript
import * as Monaco from 'monaco-editor';
import { ComponentSuggestion } from '../grid/grid.suggestions';

export const myComponentSuggestions: ComponentSuggestion[] = [
  {
    label: 'my-component',
    kind: Monaco.languages.CompletionItemKind.Snippet,
    insertText: '<my-component>Content</my-component>',
    insertTextRules: Monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
    detail: 'My component',
    documentation: {
      value: 'Creates a my component with content.',
    },
  },
];
```
