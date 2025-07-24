# Monaco Facade Solution

## Problem
The library was directly importing Monaco Editor in multiple files, which caused build conflicts when the parent application tried to manage Monaco assets through Angular's asset configuration:

```json
{
  "glob": "**/*",
  "input": "node_modules/monaco-editor",
  "output": "assets/monaco-editor"
}
```

## Solution
Created a Monaco facade that provides type-safe interfaces without directly importing Monaco, centralizing all Monaco usage in the wrapper service.

### Key Changes

1. **Created Monaco Facade** (`monaco-facade.ts`)
   - Provides type-safe interfaces that mirror Monaco types
   - Includes completion item kinds and insert text rules
   - Avoids direct Monaco imports

2. **Updated Wrapper Service** (`gravitee-monaco-wrapper.service.ts`)
   - Removed direct Monaco import
   - Uses facade types instead
   - Handles Monaco loading through AMD loader

3. **Updated Component** (`gravitee-monaco-wrapper.component.ts`)
   - Removed direct Monaco import
   - Uses facade types for all Monaco interactions
   - Maintains full functionality

4. **Updated All Suggestion Files**
   - Replaced direct Monaco imports with facade imports
   - Updated completion item types and constants
   - Maintains all autocomplete functionality

### Benefits

- **No Build Conflicts**: Library no longer directly imports Monaco
- **Centralized Monaco Usage**: All Monaco interactions go through the wrapper service
- **Type Safety**: Facade provides proper TypeScript types
- **Maintainability**: Single point of control for Monaco integration
- **Parent Application Control**: Parent can manage Monaco assets as needed

### Files Modified

- `monaco-facade.ts` (new)
- `gravitee-monaco-wrapper.service.ts`
- `gravitee-monaco-wrapper.component.ts`
- `custom-markdown.language.ts`
- All component suggestion files in `component-library/`

### Usage

The library now works without direct Monaco imports. The parent application can:

1. Include Monaco assets in their build configuration
2. The library will use the globally loaded Monaco instance
3. No build conflicts occur

The facade ensures type safety while avoiding direct dependencies on Monaco Editor. 