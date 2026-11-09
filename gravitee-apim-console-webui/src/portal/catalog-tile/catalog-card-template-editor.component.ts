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
import {
  GraviteeMarkdownEditorModule,
  GraviteeMarkdownViewerModule,
  resolveElExpressions,
} from '@gravitee/gravitee-markdown';

import { Component, computed, DestroyRef, effect, HostListener, inject, input, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { GioBannerModule, GioLoaderModule } from '@gravitee/ui-particles-angular';
import { EMPTY } from 'rxjs';
import { catchError, startWith, tap } from 'rxjs/operators';

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

  private readonly currentSettings = signal<PortalSettings | null>(null);
  private readonly initialContent = signal('');
  readonly editorReady = signal(false);

  readonly contentControl = new FormControl({ value: '', disabled: true }, { nonNullable: true });
  private readonly contentValue = toSignal(this.contentControl.valueChanges.pipe(startWith(this.contentControl.value)));

  readonly previewContent = computed(() =>
    resolveElExpressions(this.contentValue() || DEFAULT_CATALOG_CARD_TEMPLATE, SAMPLE_CATALOG_API_CONTEXT),
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
      const template = settings.portalNext?.catalog?.cardTemplate?.trim() || DEFAULT_CATALOG_CARD_TEMPLATE;
      this.initialContent.set(template);
      this.contentControl.reset(template, { emitEvent: true });
      if (this.canUpdate) {
        this.contentControl.enable({ emitEvent: false });
      } else {
        this.contentControl.disable({ emitEvent: false });
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
    return this.editorReady() && normalizeContent(this.contentControl.value) !== normalizeContent(this.initialContent());
  }

  retry(): void {
    this.editorReady.set(false);
    this.settingsResource.reload();
  }

  reset(): void {
    this.contentControl.reset(this.initialContent(), { emitEvent: true });
    this.contentControl.markAsPristine();
  }

  save(): void {
    const settings = this.currentSettings();
    if (!settings || !this.hasUnsavedChanges()) {
      return;
    }

    const cardTemplate = normalizeContent(this.contentControl.value) || DEFAULT_CATALOG_CARD_TEMPLATE;
    const payload = this.toSettingsPayload(settings, cardTemplate);
    this.portalSettingsService
      .save(payload)
      .pipe(
        tap(savedSettings => {
          const next = savedSettings ?? payload;
          this.currentSettings.set(next);
          this.initialContent.set(cardTemplate);
          this.contentControl.reset(cardTemplate, { emitEvent: true });
          this.contentControl.markAsPristine();
          this.snackBarService.success('Catalog card template saved.');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error?.message ?? 'Unable to save catalog card template.');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private toSettingsPayload(settings: PortalSettings, cardTemplate: string): PortalSettings {
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
          cardTemplate,
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
