# Angular async patterns

Depth behind the "Components" and "Utilities and testing" sections of the APIM Angular conventions. Read this when consuming observables in a component or testing a component that uses `rxResource`.

## Consuming observables — decision order

| Priority | Pattern | When to use |
| --- | --- | --- |
| 1 | `rxResource` | Async data keyed by inputs/signals; need loading/error state |
| 2 | `observable$ \| async` | Value used only in the template; no class-level signal needed |
| 3 | `toSignal(...)` | Signal needed in the class but `rxResource` doesn't fit (custom operators like debounce, combineLatest, retry) |
| 4 | `.subscribe()` | Side effects only (navigation, etc.); never for component state |

## `rxResource` — async data from inputs or signals

Prefer `rxResource` from `@angular/core/rxjs-interop` instead of wiring `toObservable` + `toSignal` around a `switchMap` pipeline.

Don't:

- Bridge `input()` / signal into RxJS with `toObservable(...).pipe(switchMap(...))` then `toSignal` — use `rxResource` as the single integration point.
- Invent custom discriminated unions (`'loading' | 'loaded' | 'error'`) — that duplicates what the resource already models.

Do:

- Use the built-in capabilities: `isLoading()` for spinners, `value()` for data, `error()` for failure.
- Skip requests by returning `null` from `params` and `of(undefined)` from `stream`.
- Align with existing usage: `subscriptions.component.ts`, `subscription-details.component.ts`, `documentation-folder.component.ts`.

```typescript
protected readonly data = rxResource<Response | undefined, string | null>({
  params: () => (this.featureOn() ? this.entityId() : null),
  stream: ({ params }) => (params ? this.service.load(params) : of(undefined)),
});
// Template: data.isLoading(), data.value(), data.error()
```

## Single observable + `| async`

Use when the value is only consumed in the template — clearest pattern, no extra signal or variable.

```typescript
items$ = toObservable(this.id).pipe(
  switchMap((id) => this.service.getItems(id)),
  map((list) => list.map(/* transform */)),
);
// Template: @if (items$ | async; as items) { ... }
```

## `toSignal(...)` — when `rxResource` doesn't fit

For streams that need multiple custom RxJS operators (debounce, combineLatest, retry logic).

```typescript
items = toSignal(
  toObservable(this.id).pipe(
    switchMap((id) => this.service.getItems(id)),
    map((list) => list.map(/* transform */)),
  ),
  { initialValue: undefined },
);
```

## `.subscribe()` — side effects only

Fire-and-forget side effects only (navigation, etc.), never component state, always with `takeUntilDestroyed`.

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

## Testing components that use `rxResource`

The constraint itself lives in the shared Angular testing conventions; this section is the setup that satisfies it. The hang mechanism: `fixture.whenStable()` waits forever when the component's `rxResource` sends a real request into the HTTP testing controller's queue and nothing flushes it — which is what module-style setups (`HttpClientTestingModule`, and portal's `AppTestingModule` which wraps it) produce by default. Specs that mock the data services themselves (`useValue` spies) never issue an HTTP request, so they work even with `AppTestingModule` — `documentation-folder.component.spec.ts` does exactly that. For new specs, default to the provider style below; console's `GioTestingModule` and `CONSTANTS_TESTING` (from `src/shared/testing`) register `provideHttpClient()` + `provideHttpClientTesting()` internally and work with `rxResource`.

The provider-style setup, shown in its portal-webui-next shape (`ConfigService` is portal's; `TESTING_BASE_URL` comes from portal's `src/testing/app-testing.module.ts` — substitute your app's own config providers):

```typescript
// BAD — hangs with rxResource
TestBed.configureTestingModule({
  imports: [MyComponent, HttpClientTestingModule, AppTestingModule],
});

// GOOD — works with rxResource
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

Setup pattern — flush HTTP immediately after `detectChanges()`, then re-render:

```typescript
async function setup(response = fakeResponse()) {
  fixture.detectChanges();                    // triggers rxResource stream
  http.expectOne(req => req.url.includes('/myEndpoint')).flush(response);
  await fixture.whenStable();                 // wait for signal propagation
  fixture.detectChanges();                    // re-render with new signal values
}
```

If a mock stream never completes (for example `new Subject()`), `fixture.whenStable()` may hang after the resource subscribes. Prefer `detectChanges()` where needed, or skip `whenStable()` once the async work you care about is scheduled.
