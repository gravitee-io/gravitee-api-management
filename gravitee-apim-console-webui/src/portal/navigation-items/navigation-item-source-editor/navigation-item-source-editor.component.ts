/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { Component, computed, DestroyRef, effect, inject, input, output, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { GioBannerModule, GioFormCronModule, GioFormJsonSchemaModule, GioJsonSchema } from '@gravitee/ui-particles-angular';
import { catchError, debounceTime, map, tap } from 'rxjs/operators';
import { of, Subject, Subscription } from 'rxjs';
import { DatePipe } from '@angular/common';
import { isEqual } from 'lodash';

import { PortalNavigationItemSource } from '../../../entities/management-api-v2';
import { FetcherService } from '../../../services-ngx/fetcher.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

interface FetcherVM {
  id: string;
  name: string;
  schema: GioJsonSchema;
}

export function stripLegacyAutoFetchFromSchema(schema: GioJsonSchema): GioJsonSchema {
  const { if: ifClause, then: thenClause, else: elseClause, ...rest } = schema as Record<string, unknown>;
  const properties = { ...((rest['properties'] as Record<string, unknown>) ?? {}) };
  delete properties['autoFetch'];
  delete properties['fetchCron'];

  const required = Array.isArray(rest['required'])
    ? (rest['required'] as string[]).filter(key => key !== 'autoFetch' && key !== 'fetchCron')
    : undefined;

  const ifClauseProperties = ((ifClause as Record<string, unknown>)?.['properties'] ?? {}) as Record<string, unknown>;
  const referencesAutoFetch = !!ifClause && 'autoFetch' in ifClauseProperties;

  return {
    ...rest,
    properties,
    ...(required && required.length > 0 ? { required } : {}),
    ...(referencesAutoFetch
      ? {}
      : {
          ...(ifClause ? { if: ifClause } : {}),
          ...(thenClause ? { then: thenClause } : {}),
          ...(elseClause ? { else: elseClause } : {}),
        }),
  } as GioJsonSchema;
}

@Component({
  selector: 'navigation-item-source-editor',
  templateUrl: './navigation-item-source-editor.component.html',
  styleUrls: ['./navigation-item-source-editor.component.scss'],
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    GioBannerModule,
    GioFormCronModule,
    GioFormJsonSchemaModule,
    DatePipe,
  ],
})
export class NavigationItemSourceEditorComponent {
  private readonly fetcherService = inject(FetcherService);
  private readonly snackBarService = inject(SnackBarService);
  private readonly destroyRef = inject(DestroyRef);

  source = input<PortalNavigationItemSource | null>(null);
  disabled = input(false);
  embedded = input(false);

  saveSource = output<PortalNavigationItemSource>();
  removeSource = output<void>();

  readonly typeControl = new FormControl<string | null>(null, Validators.required);
  readonly useAutoFetchControl = new FormControl<boolean>(false, { nonNullable: true });
  readonly fetchCronControl = new FormControl<string | null>(null);
  configurationControl?: FormControl<unknown>;

  readonly selectedSchema = signal<GioJsonSchema | undefined>(undefined);
  readonly selectedType = toSignal(this.typeControl.valueChanges, { initialValue: this.typeControl.value });
  readonly useAutoFetch = toSignal(this.useAutoFetchControl.valueChanges, { initialValue: this.useAutoFetchControl.value });
  readonly fetchCron = toSignal(this.fetchCronControl.valueChanges, { initialValue: this.fetchCronControl.value });
  private readonly fetchCronStatus = toSignal(this.fetchCronControl.statusChanges, { initialValue: this.fetchCronControl.status });
  private readonly configurationInvalid = signal(false);
  private readonly rebuildConfiguration$ = new Subject<string | null>();
  private configurationSubscription?: Subscription;
  private lastResetSource: PortalNavigationItemSource | null | undefined = undefined;

  readonly fetchers = toSignal(
    this.fetcherService.getList().pipe(
      map(fetchers =>
        fetchers.map(
          (fetcher): FetcherVM => ({
            id: fetcher.id,
            name: fetcher.name ?? fetcher.id,
            schema: stripLegacyAutoFetchFromSchema(JSON.parse(fetcher.schema)),
          }),
        ),
      ),
      catchError(() => {
        this.snackBarService.error('Failed to load the fetcher plugins');
        return of([] as FetcherVM[]);
      }),
    ),
    { initialValue: [] as FetcherVM[] },
  );

  readonly saveDisabled = computed(() => {
    if (this.disabled() || !this.selectedType() || !this.selectedSchema() || this.configurationInvalid()) {
      return true;
    }
    if (!this.useAutoFetch()) {
      return false;
    }
    return !this.fetchCron()?.trim() || this.fetchCronStatus() === 'INVALID';
  });

  private readonly rebuildConfigurationSubscription = this.rebuildConfiguration$
    .pipe(
      tap(() => {
        this.selectedSchema.set(undefined);
        this.configurationControl = undefined;
      }),
      debounceTime(10),
      takeUntilDestroyed(this.destroyRef),
    )
    .subscribe(type => {
      const initialConfiguration = type && type === this.source()?.type ? this.source()?.configuration : {};
      this.createConfigurationControl(initialConfiguration);
      this.selectedSchema.set(type ? this.fetchers().find(fetcher => fetcher.id === type)?.schema : undefined);
    });

  private readonly typeChangeSubscription = this.typeControl.valueChanges
    .pipe(takeUntilDestroyed(this.destroyRef))
    .subscribe(type => this.rebuildConfiguration$.next(type));

  private readonly resetOnSourceChange = effect(() => {
    const source = this.source();
    const fetchers = this.fetchers();
    if (!isEqual(source ?? null, this.lastResetSource ?? null) || this.lastResetSource === undefined) {
      this.lastResetSource = source ?? null;
      this.resetFormFromSource(source);
    } else if (fetchers.length > 0 && this.selectedType() && !this.selectedSchema()) {
      this.rebuildConfiguration$.next(this.selectedType());
    }
  });

  // The cron field renders only while auto-fetch is on, and its validator attaches with
  // emitEvent: false — statusChanges misses that recalculation, so re-run it once rendered
  private readonly observeCronValidityOnRender = effect(() => {
    if (this.useAutoFetch()) {
      queueMicrotask(() => this.fetchCronControl.updateValueAndValidity());
    }
  });

  private readonly applyDisabledState = effect(() => {
    const disabled = this.disabled();
    const options = { emitEvent: false };
    if (disabled) {
      this.typeControl.disable(options);
      this.useAutoFetchControl.disable(options);
      this.fetchCronControl.disable(options);
      this.configurationControl?.disable(options);
    } else {
      this.typeControl.enable(options);
      this.useAutoFetchControl.enable(options);
      this.fetchCronControl.enable(options);
      this.configurationControl?.enable(options);
    }
  });

  onSchemaFormReady(ready: boolean) {
    if (ready) {
      this.configurationControl?.updateValueAndValidity();
    }
  }

  buildSource(): PortalNavigationItemSource | null {
    const type = this.typeControl.value;
    if (!type || !this.configurationControl || this.configurationControl.invalid) {
      return null;
    }
    const useAutoFetch = this.useAutoFetchControl.value;
    const fetchCron = this.fetchCronControl.value;
    return {
      type,
      configuration: this.configurationControl.value,
      useAutoFetch,
      ...(useAutoFetch && fetchCron ? { fetchCron } : {}),
    };
  }

  onSave() {
    const source = this.buildSource();
    if (source) {
      this.saveSource.emit(source);
    }
  }

  onRemove() {
    this.removeSource.emit();
  }

  private resetFormFromSource(source: PortalNavigationItemSource | null) {
    this.typeControl.setValue(source?.type ?? null);
    this.useAutoFetchControl.setValue(source?.useAutoFetch ?? false);
    this.fetchCronControl.setValue(source?.fetchCron ?? null);
  }

  private createConfigurationControl(value: unknown) {
    this.configurationSubscription?.unsubscribe();
    const control = new FormControl<unknown>({ value, disabled: this.disabled() });
    this.configurationControl = control;
    this.configurationInvalid.set(false);
    this.configurationSubscription = control.statusChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(status => this.configurationInvalid.set(status === 'INVALID'));
  }
}
