# 008: Frontend Zee Module & Widget Component

> **Common docs**: [frontend-widget.md](./frontend-widget.md) · [architecture-overview.md](./architecture-overview.md)

## Objective

Create the `ZeeModule` with the `ZeeWidgetComponent` — the generic AI chat widget for Console UI. Text input + file upload, state machine (idle → loading → preview → accepted/error).

## Prerequisites

```bash
cd gravitee-apim-console-webui
nvm use
yarn
```

## File Structure

```
src/app/shared/components/zee/
├── zee.module.ts
├── zee-widget/
│   ├── zee-widget.component.ts
│   ├── zee-widget.component.html
│   ├── zee-widget.component.scss
│   └── zee-widget.component.spec.ts
├── zee.service.ts
└── zee.model.ts
```

## Implementation Detail

### ZeeModule

```typescript
@NgModule({
    declarations: [ZeeWidgetComponent],
    exports: [ZeeWidgetComponent],
    imports: [
        CommonModule,
        ReactiveFormsModule,
        MatButtonModule,
        MatProgressSpinnerModule,
        MatInputModule,
        MatIconModule,
        MatSnackBarModule,
        MatTabsModule, // for JSON/cards toggle
    ],
})
export class ZeeModule {}
```

### Component State Machine

```typescript
type ZeeState = "idle" | "loading" | "preview" | "accepted" | "error";

@Component({ selector: "zee-widget" })
export class ZeeWidgetComponent implements OnInit {
    @Input() resourceType!: ZeeResourceType;
    @Input() adapter!: ZeeResourceAdapter;
    @Input() contextData?: Record<string, any>;
    @Output() accepted = new EventEmitter<any>();
    @Output() rejected = new EventEmitter<void>();

    state: ZeeState = "idle";
    prompt = "";
    files: File[] = [];
    generatedResource: any = null;
    errorMessage = "";

    constructor(private zeeService: ZeeService) {}

    onSubmit(): void {
        this.state = "loading";
        this.zeeService
            .generate(
                {
                    resourceType: this.resourceType,
                    prompt: this.prompt,
                    contextData: this.contextData,
                },
                this.files,
            )
            .subscribe({
                next: (res) => {
                    this.generatedResource = res.generated;
                    this.state = "preview";
                },
                error: (err) => {
                    this.errorMessage = err.message || "Generation failed";
                    this.state = "error";
                },
            });
    }

    onAccept(): void {
        const payload = this.adapter.transform(this.generatedResource, this.contextData);
        this.accepted.emit(payload);
        this.reset();
    }

    onReject(): void {
        this.rejected.emit();
        this.reset();
    }

    private reset(): void {
        this.state = "idle";
        this.prompt = "";
        this.files = [];
        this.generatedResource = null;
    }
}
```

### Template (simplified)

```html
<div class="zee-widget" [ngSwitch]="state">
    <!-- IDLE -->
    <div *ngSwitchCase="'idle'" class="zee-input">
        <mat-form-field appearance="outline" class="zee-prompt">
            <mat-label>Describe what you want Zee to create...</mat-label>
            <textarea matInput [(ngModel)]="prompt" rows="3"></textarea>
        </mat-form-field>
        <div class="zee-file-upload" (drop)="onFileDrop($event)" (dragover)="$event.preventDefault()">
            <input type="file" #fileInput (change)="onFileSelect($event)" multiple accept=".json,.yaml,.yml,.md,.txt" />
            <span>Drag files or click to upload</span>
        </div>
        <button mat-raised-button color="primary" (click)="onSubmit()" [disabled]="!prompt.trim()">Generate with Zee</button>
    </div>

    <!-- LOADING -->
    <div *ngSwitchCase="'loading'" class="zee-loading">
        <mat-spinner diameter="40"></mat-spinner>
        <span>Zee is thinking...</span>
    </div>

    <!-- PREVIEW (see step 009 for detailed preview component) -->
    <div *ngSwitchCase="'preview'" class="zee-preview">
        <h3>{{ adapter.previewLabel }}</h3>
        <pre>{{ generatedResource | json }}</pre>
        <div class="zee-actions">
            <button mat-raised-button color="primary" (click)="onAccept()">Accept</button>
            <button mat-button (click)="onReject()">Reject</button>
        </div>
    </div>

    <!-- ERROR -->
    <div *ngSwitchCase="'error'" class="zee-error">
        <span>{{ errorMessage }}</span>
        <button mat-button (click)="reset()">Try Again</button>
    </div>
</div>
```

### ZeeService

```typescript
@Injectable({ providedIn: "root" })
export class ZeeService {
    constructor(
        private http: HttpClient,
        @Inject("Constants") private constants: any,
    ) {}

    generate(request: ZeeGenerateRequest, files?: File[]): Observable<ZeeGenerateResponse> {
        const formData = new FormData();
        formData.append("request", new Blob([JSON.stringify(request)], { type: "application/json" }));
        files?.forEach((f) => formData.append("files", f));
        return this.http.post<ZeeGenerateResponse>(`${this.constants.env.v2BaseURL}/ai/generate`, formData);
    }
}
```

## Testing

```bash
cd gravitee-apim-console-webui
yarn test --testPathPattern="zee"
```

## Acceptance Criteria

- [ ] `ZeeModule` compiles and exports `ZeeWidgetComponent`
- [ ] State machine transitions correctly: idle → loading → preview → accepted/error
- [ ] File drag-and-drop works
- [ ] Service sends multipart request to `/v2/ai/generate`
- [ ] Component tests pass

## Commit Message

```
feat(zee): add Zee Widget Angular component

Add ZeeModule and ZeeWidgetComponent with text input, file upload,
and state machine (idle → loading → preview → accepted/error).
Includes ZeeService for backend API calls.
```
