# 010: Flow Adapter & Console UI Integration

> **Common docs**: [frontend-widget.md](./frontend-widget.md) · [architecture-overview.md](./architecture-overview.md)

## Objective

Build the `FlowAdapter` (the first `ZeeResourceAdapter` implementation) and wire the Zee Widget into the Console UI's flow creation page.

## Tasks

### 1. FlowAdapter

```typescript
// src/app/shared/components/zee/adapters/flow-adapter.ts
export const FLOW_ADAPTER: ZeeResourceAdapter = {
    previewLabel: "Generated Flow",
    transform: (generated: any, context?: Record<string, any>): any => {
        // Map rehydrated Gravitee API Definition Flow shape
        // to the REST API save payload format
        return {
            name: generated.name,
            enabled: generated.enabled ?? true,
            selectors: generated.selectors?.map(mapSelector) ?? [],
            request: generated.request?.map(mapStep) ?? [],
            response: generated.response?.map(mapStep) ?? [],
            subscribe: generated.subscribe?.map(mapStep) ?? [],
            publish: generated.publish?.map(mapStep) ?? [],
            tags: generated.tags ?? [],
        };
    },
};

function mapStep(step: any) {
    return {
        name: step.name,
        policy: step.policy,
        enabled: step.enabled ?? true,
        description: step.description,
        condition: step.condition,
        configuration: step.configuration ? JSON.parse(step.configuration) : {},
        messageCondition: step.messageCondition,
    };
}

function mapSelector(sel: any) {
    // Map based on discriminator type
    switch (sel.type) {
        case "http":
            return { type: "HTTP", path: sel.path, pathOperator: sel.pathOperator, methods: sel.methods };
        case "channel":
            return { type: "CHANNEL", channel: sel.channel, channelOperator: sel.channelOperator };
        case "condition":
            return { type: "CONDITION", condition: sel.condition };
        default:
            return sel;
    }
}
```

> **Key point**: The `configuration` field in the generated output is a **stringified JSON** (because json-schema-llm's opaque type pass converts `Map<String, Object>` to `String`). The adapter must `JSON.parse()` it back to an object before saving.

### 2. Find the Flow Creation Page

```bash
# Find where flows are created in the Console UI
grep -r "createFlow\|addFlow\|flow.*create\|flow.*new" \
  gravitee-apim-console-webui/src/ --include="*.ts" -l | head -10
```

Look for the component that handles creating a new flow for an API. It will likely be in the API detail pages, under a flows or policy-studio section.

### 3. Wire the Widget

In the flow creation page's module:

```typescript
imports: [ZeeModule, ...]
```

In the template:

```html
<zee-widget [resourceType]="'FLOW'" [adapter]="flowAdapter" [contextData]="{ apiId: api.id }" (accepted)="onFlowGenerated($event)">
</zee-widget>
```

In the component:

```typescript
flowAdapter = FLOW_ADAPTER;

onFlowGenerated(savePayload: any) {
  this.flowService.createFlow(this.apiId, savePayload).subscribe({
    next: () => this.snackBar.open('Flow created by Zee!', 'Close', { duration: 3000 }),
    error: (err) => this.snackBar.open('Failed to save flow', 'Close', { duration: 3000 }),
  });
}
```

## Testing

```bash
cd gravitee-apim-console-webui
yarn test --testPathPattern="flow-adapter"
yarn serve  # then navigate to flow creation page
```

## Acceptance Criteria

- [ ] FlowAdapter correctly transforms generated Flow → save payload
- [ ] `configuration` field is properly parsed from string to object
- [ ] Selector types are correctly mapped
- [ ] Zee Widget appears on the flow creation page
- [ ] End-to-end: type prompt → see preview → accept → flow is saved
- [ ] Reject resets the widget

## Commit Message

```
feat(zee): integrate Zee Widget into flow creation page

Add FlowAdapter that transforms generated Flows to save payloads.
Wire ZeeWidgetComponent into the Console UI flow creation page
for end-to-end AI-assisted flow creation.
```
