# Zee Widget — Frontend Component Design

## Overview

A single, generic Angular NgModule component (`<zee-widget>`) that can be dropped into any Console UI page where resources are created or updated. Minimal configuration required per integration point.

## Module Structure

```
gravitee-apim-console-webui/src/app/shared/components/zee/
├── zee.module.ts
├── zee-widget/
│   ├── zee-widget.component.ts
│   ├── zee-widget.component.html
│   ├── zee-widget.component.scss
│   └── zee-widget.component.spec.ts
├── zee-preview/
│   ├── zee-preview.component.ts      (JSON tree + structured cards toggle)
│   ├── zee-preview.component.html
│   └── zee-preview.component.scss
├── zee.service.ts                     (HTTP calls to /v2/ai/generate)
├── zee.model.ts                       (TypeScript interfaces)
└── adapters/
    ├── zee-resource-adapter.ts        (interface)
    └── flow-adapter.ts                (first implementation)
```

## Component API

```typescript
// zee-widget.component.ts
@Component({ selector: "zee-widget" })
export class ZeeWidgetComponent {
    @Input() resourceType: ZeeResourceType;
    @Input() adapter: ZeeResourceAdapter;
    @Input() contextData?: Record<string, any>;
    @Output() accepted = new EventEmitter<any>();
    @Output() rejected = new EventEmitter<void>();
}

// zee.model.ts
export enum ZeeResourceType {
    FLOW = "FLOW",
    PLAN = "PLAN",
    API = "API",
    ENDPOINT = "ENDPOINT",
    ENTRYPOINT = "ENTRYPOINT",
}

export interface ZeeResourceAdapter<TGenerated = any, TSavePayload = any> {
    transform(generated: TGenerated, context?: Record<string, any>): TSavePayload;
    previewLabel: string;
}

export interface ZeeGenerateRequest {
    resourceType: ZeeResourceType;
    prompt: string;
    contextData?: Record<string, any>;
}

export interface ZeeGenerateResponse {
    resourceType: string;
    generated: any;
    metadata: { model: string; tokensUsed: number };
}
```

## UI States

| State        | What's Shown                                                                                    |
| ------------ | ----------------------------------------------------------------------------------------------- |
| **Idle**     | Text input field, file upload dropzone ("Drag files or click to upload")                        |
| **Loading**  | Spinner + "Zee is thinking..." message                                                          |
| **Preview**  | Generated resource preview with Accept/Reject buttons, toggle for JSON tree vs structured cards |
| **Accepted** | Brief success snackbar, widget resets to Idle                                                   |
| **Error**    | Error message + "Try Again" button                                                              |

## Preview Rendering

D requested **both** JSON tree and structured cards with a toggle:

- **JSON Tree**: Raw JSON rendered in a collapsible tree viewer (Angular Material tree or similar)
- **Structured Cards**: Human-friendly card layout (e.g., for a Flow: flow name, list of request steps with policy names and descriptions)

## File Upload

- Supported formats: `.json`, `.yaml`, `.yml`, `.md`, `.txt`
- Sent as `multipart/form-data` parts
- Backend extracts text content and appends to prompt
- Max file size: TBD (reasonable limit, e.g., 1MB per file, 5 files max)

## Integration Example

```html
<!-- In the flow creation page template -->
<zee-widget
    [resourceType]="'FLOW'"
    [adapter]="flowAdapter"
    [contextData]="{ apiId: api.id }"
    (accepted)="onFlowGenerated($event)"
    (rejected)="onGenerationRejected()"
>
</zee-widget>
```

```typescript
// In the flow creation page component
flowAdapter: ZeeResourceAdapter = {
  previewLabel: 'Generated Flow',
  transform: (generated: any, context?: Record<string, any>) => {
    // Map rehydrated Gravitee Flow shape → Console UI save payload
    return {
      name: generated.name,
      enabled: generated.enabled,
      selectors: generated.selectors,
      request: generated.request,
      response: generated.response,
      // ... field mapping as needed
    };
  }
};

onFlowGenerated(savePayload: any) {
  // Fire existing save/update REST call
  this.flowService.create(this.apiId, savePayload).subscribe(/* ... */);
}
```

## ZeeService (HTTP Layer)

```typescript
@Injectable({ providedIn: "root" })
export class ZeeService {
    constructor(private http: HttpClient) {}

    generate(request: ZeeGenerateRequest, files?: File[]): Observable<ZeeGenerateResponse> {
        const formData = new FormData();
        formData.append("request", new Blob([JSON.stringify(request)], { type: "application/json" }));
        if (files) {
            files.forEach((f) => formData.append("files", f));
        }
        return this.http.post<ZeeGenerateResponse>(`${Constants.env.v2BaseURL}/ai/generate`, formData);
    }
}
```
