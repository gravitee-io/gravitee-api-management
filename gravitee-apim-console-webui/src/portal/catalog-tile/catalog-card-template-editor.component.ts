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
import { GraviteeMarkdownEditorModule, GraviteeMarkdownViewerModule, resolveElExpressions } from '@gravitee/gravitee-markdown';

import { Component, computed, DestroyRef, effect, HostListener, inject, input, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { GioBannerModule, GioLoaderModule } from '@gravitee/ui-particles-angular';
import { EMPTY } from 'rxjs';
import { catchError, startWith, tap } from 'rxjs/operators';

import {
  CATALOG_LIST_COLUMN_OPTIONS,
  catalogConfigFromSettings,
  CatalogListColumnId,
  CatalogPageConfig,
  orderListColumns,
  parseListColumns,
} from './catalog-page-config.constants';
import { DEFAULT_CATALOG_CARD_TEMPLATE, SAMPLE_CATALOG_API_CONTEXT } from './default-catalog-card-template';

import { PortalSettings } from '../../entities/portal/portalSettings';
import { PortalSettingsService } from '../../services-ngx/portal-settings.service';
import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { HasUnsavedChanges } from '../../shared/guards/has-unsaved-changes.guard';
import { normalizeContent } from '../../shared/utils/content.util';

@Component({
  selector: 'portal-catalog-card-template-editor',
  imports: [
    GioBannerModule,
    GioLoaderModule,
    GraviteeMarkdownEditorModule,
    GraviteeMarkdownViewerModule,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTabsModule,
    ReactiveFormsModule,
  ],
  templateUrl: './catalog-card-template-editor.component.html',
  styleUrl: './catalog-card-template-editor.component.scss',
})
export class CatalogCardTemplateEditorComponent implements HasUnsavedChanges {
  private readonly destroyRef = inject(DestroyRef);
  private readonly permissionService = inject(GioPermissionService);
  private readonly portalSettingsService = inject(PortalSettingsService);
  private readonly snackBarService = inject(SnackBarService);
  private readonly canUpdate = this.permissionService.hasAnyMatching(['environment-settings-u']);

  /** Mirrors Navigation page Preview toggle. */
  readonly showPreview = input(true);

  readonly listColumnOptions = CATALOG_LIST_COLUMN_OPTIONS;

  private readonly currentSettings = signal<PortalSettings | null>(null);
  private readonly initialConfig = signal<CatalogPageConfig | null>(null);
  readonly editorReady = signal(false);

  readonly optionsForm = new FormGroup({
    categoryFilterEnabled: new FormControl(true, { nonNullable: true }),
    categoryFilterLabel: new FormControl('Category', { nonNullable: true }),
    cardsViewEnabled: new FormControl(true, { nonNullable: true }),
    cardsViewLabel: new FormControl('Cards', { nonNullable: true }),
    listViewEnabled: new FormControl(true, { nonNullable: true }),
    listViewLabel: new FormControl('List', { nonNullable: true }),
    defaultDisplayMode: new FormControl<'grid' | 'list'>('grid', { nonNullable: true }),
    listColumns: new FormControl<CatalogListColumnId[]>([...CATALOG_LIST_COLUMN_OPTIONS], { nonNullable: true }),
    listColumnsOrder: new FormControl(CATALOG_LIST_COLUMN_OPTIONS.join(','), { nonNullable: true }),
  });

  readonly topContentControl = new FormControl({ value: '', disabled: true }, { nonNullable: true });
  readonly bottomContentControl = new FormControl({ value: '', disabled: true }, { nonNullable: true });
  readonly cardTemplateControl = new FormControl({ value: '', disabled: true }, { nonNullable: true });

  private readonly optionsValue = toSignal(this.optionsForm.valueChanges.pipe(startWith(this.optionsForm.getRawValue())), {
    initialValue: this.optionsForm.getRawValue(),
  });
  private readonly cardTemplateValue = toSignal(
    this.cardTemplateControl.valueChanges.pipe(startWith(this.cardTemplateControl.value)),
  );
  private readonly topContentValue = toSignal(this.topContentControl.valueChanges.pipe(startWith(this.topContentControl.value)));
  private readonly bottomContentValue = toSignal(
    this.bottomContentControl.valueChanges.pipe(startWith(this.bottomContentControl.value)),
  );

  readonly showCardsLabel = computed(() => !!this.optionsValue()?.cardsViewEnabled);
  readonly showListLabel = computed(() => !!this.optionsValue()?.listViewEnabled);
  readonly showListColumns = computed(() => !!this.optionsValue()?.listViewEnabled);
  readonly showCategoryLabel = computed(() => !!this.optionsValue()?.categoryFilterEnabled);
  readonly orderedSelectedColumns = computed(() =>
    orderListColumns(this.optionsValue()?.listColumns ?? [], this.optionsValue()?.listColumnsOrder ?? ''),
  );

  readonly previewContent = computed(() =>
    resolveElExpressions(this.cardTemplateValue() || DEFAULT_CATALOG_CARD_TEMPLATE, SAMPLE_CATALOG_API_CONTEXT),
  );

  readonly settingsResource = rxResource({
    stream: () => this.portalSettingsService.get(),
  });

  constructor() {
    effect(() => {
      if (this.settingsResource.hasValue()) {
        this.currentSettings.set(this.settingsResource.value());
      }
    });

    effect(() => {
      const settings = this.currentSettings();
      if (!settings || this.editorReady()) {
        return;
      }
      const config = catalogConfigFromSettings(settings.portalNext?.catalog);
      config.cardTemplate = settings.portalNext?.catalog?.cardTemplate?.trim() || DEFAULT_CATALOG_CARD_TEMPLATE;
      this.initialConfig.set(config);
      this.patchOptionsForm(config);
      this.topContentControl.reset(config.topContent, { emitEvent: true });
      this.bottomContentControl.reset(config.bottomContent, { emitEvent: true });
      this.cardTemplateControl.reset(config.cardTemplate, { emitEvent: true });
      if (this.canUpdate) {
        this.optionsForm.enable({ emitEvent: false });
        this.topContentControl.enable({ emitEvent: false });
        this.bottomContentControl.enable({ emitEvent: false });
        this.cardTemplateControl.enable({ emitEvent: false });
      } else {
        this.optionsForm.disable({ emitEvent: false });
        this.topContentControl.disable({ emitEvent: false });
        this.bottomContentControl.disable({ emitEvent: false });
        this.cardTemplateControl.disable({ emitEvent: false });
      }
      this.editorReady.set(true);
    });
  }

  @HostListener('window:beforeunload', ['$event'])
  beforeUnloadHandler(event: BeforeUnloadEvent): string | void {
    if (this.hasUnsavedChanges()) {
      event.preventDefault();
      event.returnValue = '';
      return '';
    }
  }

  hasUnsavedChanges(): boolean {
    if (!this.editorReady() || !this.initialConfig()) {
      return false;
    }
    const current = this.readCurrentConfig();
    const initial = this.initialConfig()!;
    return (
      normalizeContent(current.topContent) !== normalizeContent(initial.topContent) ||
      normalizeContent(current.bottomContent) !== normalizeContent(initial.bottomContent) ||
      normalizeContent(current.cardTemplate) !== normalizeContent(initial.cardTemplate) ||
      current.categoryFilterEnabled !== initial.categoryFilterEnabled ||
      current.categoryFilterLabel !== initial.categoryFilterLabel ||
      current.cardsViewEnabled !== initial.cardsViewEnabled ||
      current.cardsViewLabel !== initial.cardsViewLabel ||
      current.listViewEnabled !== initial.listViewEnabled ||
      current.listViewLabel !== initial.listViewLabel ||
      current.defaultDisplayMode !== initial.defaultDisplayMode ||
      current.listColumns.join(',') !== initial.listColumns.join(',')
    );
  }

  retry(): void {
    this.editorReady.set(false);
    this.settingsResource.reload();
  }

  reset(): void {
    const initial = this.initialConfig();
    if (!initial) {
      return;
    }
    this.patchOptionsForm(initial);
    this.optionsForm.markAsPristine();
    this.topContentControl.reset(initial.topContent, { emitEvent: true });
    this.bottomContentControl.reset(initial.bottomContent, { emitEvent: true });
    this.cardTemplateControl.reset(initial.cardTemplate, { emitEvent: true });
    this.topContentControl.markAsPristine();
    this.bottomContentControl.markAsPristine();
    this.cardTemplateControl.markAsPristine();
  }

  save(): void {
    const settings = this.currentSettings();
    if (!settings || !this.hasUnsavedChanges()) {
      return;
    }

    const config = this.readCurrentConfig();
    const cardTemplate = normalizeContent(config.cardTemplate) || DEFAULT_CATALOG_CARD_TEMPLATE;
    const payload = this.toSettingsPayload(settings, { ...config, cardTemplate });
    this.portalSettingsService
      .save(payload)
      .pipe(
        tap(savedSettings => {
          const next = savedSettings ?? payload;
          this.currentSettings.set(next);
          const savedConfig = catalogConfigFromSettings(next.portalNext?.catalog);
          savedConfig.cardTemplate = next.portalNext?.catalog?.cardTemplate?.trim() || cardTemplate;
          this.initialConfig.set(savedConfig);
          this.patchOptionsForm(savedConfig);
          this.optionsForm.markAsPristine();
          this.topContentControl.reset(savedConfig.topContent, { emitEvent: true });
          this.bottomContentControl.reset(savedConfig.bottomContent, { emitEvent: true });
          this.cardTemplateControl.reset(savedConfig.cardTemplate, { emitEvent: true });
          this.topContentControl.markAsPristine();
          this.bottomContentControl.markAsPristine();
          this.cardTemplateControl.markAsPristine();
          this.snackBarService.success('Catalog page configuration saved.');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error?.message ?? 'Unable to save catalog page configuration.');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  isColumnSelected(column: CatalogListColumnId): boolean {
    return this.optionsForm.controls.listColumns.value.includes(column);
  }

  toggleColumn(column: CatalogListColumnId, checked: boolean): void {
    const current = this.optionsForm.controls.listColumns.value;
    const next = checked ? [...current.filter(item => item !== column), column] : current.filter(item => item !== column);
    this.optionsForm.controls.listColumns.setValue(next);
    this.optionsForm.controls.listColumns.markAsDirty();
    const order = orderListColumns(next, this.optionsForm.controls.listColumnsOrder.value);
    this.optionsForm.controls.listColumnsOrder.setValue(order.join(','));
  }

  moveColumn(column: CatalogListColumnId, direction: -1 | 1): void {
    const order = parseListColumns(this.optionsForm.controls.listColumnsOrder.value);
    const selected = new Set(this.optionsForm.controls.listColumns.value);
    const visible = order.filter(item => selected.has(item));
    const index = visible.indexOf(column);
    const swapWith = index + direction;
    if (index < 0 || swapWith < 0 || swapWith >= visible.length) {
      return;
    }
    const next = [...visible];
    [next[index], next[swapWith]] = [next[swapWith], next[index]];
    const remaining = order.filter(item => !selected.has(item));
    this.optionsForm.controls.listColumnsOrder.setValue([...next, ...remaining].join(','));
    this.optionsForm.controls.listColumnsOrder.markAsDirty();
  }

  private patchOptionsForm(config: CatalogPageConfig): void {
    this.optionsForm.reset(
      {
        categoryFilterEnabled: config.categoryFilterEnabled,
        categoryFilterLabel: config.categoryFilterLabel,
        cardsViewEnabled: config.cardsViewEnabled,
        cardsViewLabel: config.cardsViewLabel,
        listViewEnabled: config.listViewEnabled,
        listViewLabel: config.listViewLabel,
        defaultDisplayMode: config.defaultDisplayMode,
        listColumns: [...config.listColumns],
        listColumnsOrder: config.listColumns.join(','),
      },
      { emitEvent: true },
    );
  }

  private readCurrentConfig(): CatalogPageConfig {
    const raw = this.optionsForm.getRawValue();
    return {
      topContent: this.topContentValue() ?? '',
      bottomContent: this.bottomContentValue() ?? '',
      cardTemplate: this.cardTemplateValue() ?? '',
      categoryFilterEnabled: raw.categoryFilterEnabled,
      categoryFilterLabel: raw.categoryFilterLabel.trim() || 'Category',
      cardsViewEnabled: raw.cardsViewEnabled,
      cardsViewLabel: raw.cardsViewLabel.trim() || 'Cards',
      listViewEnabled: raw.listViewEnabled,
      listViewLabel: raw.listViewLabel.trim() || 'List',
      defaultDisplayMode: raw.defaultDisplayMode === 'list' ? 'list' : 'grid',
      listColumns: orderListColumns(raw.listColumns, raw.listColumnsOrder),
    };
  }

  private toSettingsPayload(settings: PortalSettings, config: CatalogPageConfig): PortalSettings {
    return {
      ...settings,
      email: settings.email
        ? {
            ...settings.email,
            from: unwrapQuotedSender(settings.email.from),
            brandedSenders: settings.email.brandedSenders?.map(sender => ({
              ...sender,
              from: unwrapQuotedSender(sender.from),
            })),
          }
        : settings.email,
      portalNext: {
        ...settings.portalNext,
        access: settings.portalNext?.access ?? { enabled: false },
        catalog: {
          ...settings.portalNext?.catalog,
          topContent: normalizeContent(config.topContent),
          bottomContent: normalizeContent(config.bottomContent),
          categoryFilter: {
            enabled: config.categoryFilterEnabled,
            label: config.categoryFilterLabel,
          },
          cardsView: {
            enabled: config.cardsViewEnabled,
            label: config.cardsViewLabel,
          },
          listView: {
            enabled: config.listViewEnabled,
            label: config.listViewLabel,
          },
          defaultDisplayMode: config.defaultDisplayMode,
          listColumns: config.listColumns.join(','),
          cardTemplate: config.cardTemplate,
        },
      },
    };
  }
}

function unwrapQuotedSender(value: string): string {
  const trimmed = value?.trim();
  if (trimmed && trimmed.length > 2 && trimmed.startsWith('"') && trimmed.endsWith('"')) {
    const inner = trimmed.slice(1, -1).trim();
    if (inner && !inner.includes('"')) {
      return inner;
    }
  }
  return value;
}
