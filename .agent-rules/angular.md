# Shared Angular Coding Standards

## 1. General Philosophy

- **Clarity over indirection:** Prefer code that is clear and direct. Avoid introducing intermediate variables when a single expression or direct template binding is readable (e.g. one Observable with `| async` in the template, or a single `toSignal(...)` expression, rather than a private observable in one field and a separate signal in another).
- **Signals First:** Use Signals for all local state, computed values, and inputs/outputs.
- **Standalone Only:** All components, directives, and pipes must be `standalone: true`.
- **Type Safety:** No `any`. Use `unknown` only if absolutely necessary. Strict typing for Dialogs and Forms.
- **Design System:** Do not invent new UI patterns. Reuse existing components. If a new component is needed, suggest collaborating with UX.

## 2. Component Implementation Rules

### Structure & Order

Follow this strict class member ordering:
1.  `private readonly` Injections (using `inject()`)
2.  Inputs (Signal inputs: `input()` or `input.required()`)
3.  Outputs (Signal outputs: `output()`)
4.  State (Signals, public properties)
5.  Computed (Computed signals; `toSignal(...)`, `rxResource(...)`)
6.  Methods (Public then Private)

### Dependency Injection (DI)

- **NEVER** use the `constructor` for DI or for subscription setup.
- **ALWAYS** use `private readonly service = inject(ServiceName);` for DI.
- Use `toSignal()`, `effect()`, or async pipe for reactive data so that no constructor or `ngOnInit` is required for subscriptions.

### Logic

- **Smart Components:** Handle data fetching, business logic, and service calls.
- **Dumb Components:** Purely presentational. Receive data via Inputs, communicate via Outputs.

## 3. State Management & RxJS

### Signals

- Use `signal()` for **mutable local state**.
- Use `computed()` for **derived / read-only state**.

### Consuming Observables — decision order

| Priority | Pattern | When to use |
|----------|---------|-------------|
| 1 | `rxResource` | Async data keyed by inputs/signals; need loading/error state |
| 2 | `observable$ \| async` | Value used only in template; no class-level signal needed |
| 3 | `toSignal(...)` | Signal needed in class but `rxResource` doesn't fit (custom operators like debounce, combineLatest, retry) |
| 4 | `.subscribe()` | Side effects only (navigation, etc); **never** for component state |

**Key rules:**

- Prefer a **single reactive expression**. Don't store a private observable in one field then pass it to `toSignal()` in another.
- `.subscribe()` requires `takeUntilDestroyed(this.destroyRef)`.
- No nested subscriptions — use `switchMap`, `mergeMap`, etc.
- Suffix Observable variables with `$`.

### `rxResource` — async data from inputs or signals

For classic data fetching with potential loading and error states.

Prefer `rxResource` from `@angular/core/rxjs-interop` instead of wiring `toObservable` + `toSignal` around a `switchMap` pipeline.

#### Don't

- Bridge `input()` / signal into RxJS with `toObservable(...).pipe(switchMap(...))` then `toSignal` — use `rxResource` as the single integration point.
- Invent custom discriminated unions (`'loading' | 'loaded' | 'error'`) — that duplicates what the resource already models.

#### Do

- Use OOTB capabilities: `isLoading()` for spinners, `value()` for data, `error()` for failure.
- Skip requests by returning `null` from `params` and `of(undefined)` from `stream`.
- Align with existing usage: `subscriptions.component.ts`, `subscription-details.component.ts`, `documentation-folder.component.ts`.

```typescript
protected readonly data = rxResource<Response | undefined, string | null>({
  params: () => (this.featureOn() ? this.entityId() : null),
  stream: ({ params }) => (params ? this.service.load(params) : of(undefined)),
});
// Template: data.isLoading(), data.value(), data.error()
```

### Single observable + `| async`

Use when the value is only consumed in the template — clearest pattern, no extra signal or variable.

```typescript
items$ = toObservable(this.id).pipe(
  switchMap((id) => this.service.getItems(id)),
  map((list) => list.map(/* transform */)),
);
// Template: @if (items$ | async; as items) { ... }
```

### `toSignal(...)` — when `rxResource` doesn't fit

For use cases like handling a data stream that require multiple custom RxJS operators (debounce, combineLatest, retry logic).

```typescript
items = toSignal(
  toObservable(this.id).pipe(
    switchMap((id) => this.service.getItems(id)),
    map((list) => list.map(/* transform */)),
  ),
  { initialValue: undefined },
);
```

### `.subscribe()` — side effects only

Only for fire-and-forget side effects (navigation, etc). Never for component state. Always include `takeUntilDestroyed`.

```typescript
this.backendService.get().pipe(
  tap((response) => { /* side effects */ }),
  catchError((error) => {
    this.log.error('Failed to fetch backend data', error);
    return EMPTY;
  }),
  takeUntilDestroyed(this.destroyRef),
).subscribe();
```

## 4. HTML Templates & Performance

- **Control Flow:** STRICTLY use the new syntax: `@if`, `@for`, `@switch`. **NEVER** use `*ngIf` or `*ngFor`.
- **No Method Calls:** Never bind to methods in templates (e.g., `{{ getLabel() }}`). Use `computed` signals or Pipes.
- **Styling:** No inline styles. No hard-coded hex values (use CSS variables/Design Tokens).
- **Semantics:** Use semantic HTML (`<button>`) over ARIA (`<div role="button">`).

## 5. Forms & Dialogs

- **Forms:** Always use **Typed Forms**. Use Signals/Models for ReactiveForm values where applicable.
- **Dialogs:** Strictly type the Open call.
  - Pattern: `this.matDialog.open<ComponentType, InputType, OutputType>(...)`

## 6. Naming

Names should describe what something does or represents. Prefer names that make the operation and data flow explicit.

- **Functions**: Use verb + noun that reflects the operation (extract, collect, derive, compute, find).
- **Variables**: Name after the concept they hold, not the type. Avoid contractions (`comp`, `el`, `attr`, `c`) unless they are an established pattern in the file.
- **Booleans**: Use `is`, `has`, `can`, `should` prefixes.
- **Observables**: Suffix with `$`.

### Example: Function names

```typescript
// ❌ BAD - "get" + "already added" is vague; source of data is unclear
function getAlreadyAddedApiIds(items: PortalNavigationItem[]): string[] {
  return items.filter((i): i is PortalNavigationApi => i.type === 'API').map(i => i.apiId);
}

// ✅ GOOD - clearly describes the operation: extract API IDs from navigation items
function extractApiIdsFromNavigationItems(items: PortalNavigationItem[]): string[] {
  return items.filter((i): i is PortalNavigationApi => i.type === 'API').map(i => i.apiId);
}
```

### Example: Variable names

```typescript
// ❌ BAD - unnecessary contraction; inconsistent with file convention
const comp = fixture.componentInstance;
comp.onNodeMenuAction(...);

// ✅ GOOD - use full, descriptive names; follow established patterns in the file
const component = fixture.componentInstance;
component.onNodeMenuAction(...);
```

## 7. Utilities & Dependencies

- **Prefer existing utilities:** Before implementing a common transform or utility (string casing, object shape, etc.), check if it already exists in the project. Use **lodash** for typical operations like `kebabCase`, `camelCase`, `snakeCase`, `isEmpty`, `isEqual`, `merge`, etc.

### Example: string casing

```typescript
// ❌ BAD — custom implementation when lodash has it
private static camelToKebab(str: string): string {
  return str.replace(/([a-z])([A-Z])/g, '$1-$2').toLowerCase();
}
...
map(([k, v]) => [`--preview-dark-${this.camelToKebab(k)}`, v])

// ✅ GOOD — use lodash
import { kebabCase } from 'lodash';
...
map(([k, v]) => [`--preview-dark-${kebabCase(k)}`, v])
```

## 8. Styling (CSS & Material)

- **Layout:** Use Flexbox for layout. Avoid hard-set margins/padding.
- **Encapsulation:** **NEVER** use `:ng-deep`.
- **Material:** Use CSS Tokens to modify components. Do not override global classes.

## 9. Testing

- **Philosophy:** Test business rules, not implementation details.
- **Harnesses:**
  - Use **Component Harnesses** (e.g., `MatTableHarness`, `SidenavLayoutComponentHarness`) for all interactions.
  - Compose harnesses following the component hierarchy (call child component harnesses inside parent component harnesses)
- **Querying:** Don't use DOM methods (querySelector, getElementById, etc) or fixture.debugElement.query(), prefer composing harnesses.
- **Selectors:** Prefer `data-testid` or accessibility-oriented selectors over brittle DOM structure
- **Async:** Await all Promises. Use `fixture.destroy()` instead of `discardPeriodicTasks`.
- **HTTP Testing:** When a component uses services that call the backend, use `HttpTestingController` rather than mocking them.

### Testing components that use `rxResource`

**NEVER** use `HttpClientTestingModule` or `AppTestingModule` for components with `rxResource` — tests will hang on `await fixture.whenStable()`. Use the standalone provider style instead:

```typescript
// ❌ BAD — hangs with rxResource
TestBed.configureTestingModule({
  imports: [MyComponent, HttpClientTestingModule, AppTestingModule],
});

// ✅ GOOD — works with rxResource
TestBed.configureTestingModule({
  imports: [MyComponent],
  providers: [
    provideHttpClient(),
    provideHttpClientTesting(),
    provideNoopAnimations(),
    provideRouter([]),
    { provide: ConfigService, useValue: { baseURL: TESTING_BASE_URL } },
  ],
});
```

Setup pattern — flush HTTP **immediately** after `detectChanges()`, then re-render:

```typescript
async function setup(response = fakeResponse()) {
  fixture.detectChanges();                    // triggers rxResource stream
  http.expectOne(req => req.url.includes('/myEndpoint')).flush(response);
  await fixture.whenStable();                 // wait for signal propagation
  fixture.detectChanges();                    // re-render with new signal values
}
```

If a mock stream **never completes** (e.g. `new Subject()`), `fixture.whenStable()` may hang after the resource subscribes. Prefer **`detectChanges()`** where needed or skip **`whenStable()`** in that scenario after the async work you care about is scheduled.

## Code example (Gold Standard)

```typescript
@Component({
  selector: 'app-user-card',
  standalone: true,
  imports: [MatButtonModule, DatePipe],
  template: `
    <div class="user-card">
      @if (user(); as u) {
        <h3>{{ u.name }}</h3>
        <p>Joined: {{ u.joinedDate | date }}</p>
        <button mat-button (click)="promote.emit(u.id)">Promote</button>
      }
    </div>
  `
})
export class UserCardComponent {
  // 1. Injections
  private readonly logging = inject(LoggerService);

  // 2. Inputs
  user = input.required<User>();

  // 3. Outputs
  promote = output<string>();

  // 4. Computed
  isValid = computed(() => !!this.user().id);
}
```
