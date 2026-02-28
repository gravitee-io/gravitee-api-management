# 009: Preview Component (JSON Tree + Structured Cards)

> **Common docs**: [frontend-widget.md](./frontend-widget.md)

## Objective

Build the preview rendering component with a toggle between JSON tree view and structured cards. D specifically requested both views.

## File Structure

```
src/app/shared/components/zee/
├── zee-preview/
│   ├── zee-preview.component.ts
│   ├── zee-preview.component.html
│   ├── zee-preview.component.scss
│   └── zee-preview.component.spec.ts
└── zee-structured-view/
    ├── flow-card.component.ts           (Flow-specific structured view)
    ├── flow-card.component.html
    └── flow-card.component.scss
```

## Implementation Detail

### ZeePreviewComponent

Uses `mat-tab-group` for the toggle:

```typescript
@Component({ selector: "zee-preview" })
export class ZeePreviewComponent {
    @Input() data: any;
    @Input() resourceType: ZeeResourceType;

    activeView: "json" | "cards" = "cards";
}
```

```html
<mat-tab-group [(selectedIndex)]="activeViewIndex">
    <mat-tab label="Structured View">
        <ng-container [ngSwitch]="resourceType">
            <zee-flow-card *ngSwitchCase="'FLOW'" [flow]="data"></zee-flow-card>
            <!-- Add more resource-type cards as they're implemented -->
            <pre *ngSwitchDefault>{{ data | json }}</pre>
        </ng-container>
    </mat-tab>
    <mat-tab label="JSON">
        <pre class="json-view">{{ data | json }}</pre>
    </mat-tab>
</mat-tab-group>
```

### FlowCardComponent (Structured View)

Renders a Flow in a human-friendly layout:

```html
<mat-card>
    <mat-card-header>
        <mat-card-title>{{ flow.name }}</mat-card-title>
        <mat-chip [color]="flow.enabled ? 'primary' : 'warn'"> {{ flow.enabled ? 'Enabled' : 'Disabled' }} </mat-chip>
    </mat-card-header>
    <mat-card-content>
        <!-- Request Steps -->
        <h4 *ngIf="flow.request?.length">Request Steps</h4>
        <mat-list *ngIf="flow.request?.length">
            <mat-list-item *ngFor="let step of flow.request">
                <mat-icon matListItemIcon>policy</mat-icon>
                <span matListItemTitle>{{ step.name }}</span>
                <span matListItemLine>Policy: {{ step.policy }}</span>
                <span matListItemLine *ngIf="step.description">{{ step.description }}</span>
            </mat-list-item>
        </mat-list>

        <!-- Selectors -->
        <h4 *ngIf="flow.selectors?.length">Selectors</h4>
        <mat-chip-listbox *ngIf="flow.selectors?.length">
            <mat-chip *ngFor="let sel of flow.selectors">{{ sel.type }}: {{ sel.path || sel.channel }}</mat-chip>
        </mat-chip-listbox>

        <!-- Tags -->
        <h4 *ngIf="flow.tags?.length">Tags</h4>
        <mat-chip-listbox *ngIf="flow.tags?.length">
            <mat-chip *ngFor="let tag of flow.tags">{{ tag }}</mat-chip>
        </mat-chip-listbox>
    </mat-card-content>
</mat-card>
```

## Acceptance Criteria

- [ ] Preview toggles between JSON tree and structured cards
- [ ] FlowCardComponent renders flow name, steps, selectors, tags
- [ ] Default tab is structured view
- [ ] Graceful fallback for unknown resource types (shows JSON)

## Commit Message

```
feat(zee): add preview component with JSON/card toggle

Add ZeePreviewComponent with dual-view toggle: structured cards
(human-friendly) and raw JSON tree. Includes FlowCardComponent
for Flow-specific structured rendering.
```
