# Test Suggestions

This is a test file to verify that Monaco editor suggestions are working.

Try typing:
- `<` - should trigger suggestions
- `<copy-code` - should show copy-code suggestions
- `<copy-code text="test"` - should complete the tag

## Expected Behavior

1. When you type `<`, you should see suggestions for copy-code
2. When you type `<copy-code`, you should see the component suggestions
3. The suggestions should include both regular and self-closing tags

## Test Cases

```markdown
# Test 1: Basic suggestion
<copy-code text="console.log('Hello World')"></copy-code>

# Test 2: Self-closing tag
<copy-code text="npm install package" />

# Test 3: With attributes
<copy-code text="curl -X GET https://api.example.com/v1/users"></copy-code>
```

## Manual Trigger

You can also manually trigger suggestions by:
1. Pressing `Ctrl+Space` (or `Cmd+Space` on Mac)
2. Right-clicking and selecting "Trigger Suggest"
3. Using the command palette (`Ctrl+Shift+P`) and typing "Trigger Suggest" 