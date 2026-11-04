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
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  GIO_DIALOG_WIDTH,
  GioBannerModule,
  GioConfirmDialogComponent,
  GioConfirmDialogData,
  GioConfirmDialogModule,
  GioIconsModule,
} from '@gravitee/ui-particles-angular';
import { isEqual } from 'lodash';
import { EMPTY, of } from 'rxjs';
import { catchError, distinctUntilChanged, filter, map, switchMap, tap } from 'rxjs/operators';

import {
  ApiV4PathMappingsAddDialogComponent,
  ApiV4PathMappingsAddDialogData,
  ApiV4PathMappingsAddDialogResult,
} from './api-v4-path-mappings-add-dialog.component';
import { ApiV4PathMappingsEditDialogComponent, ApiV4PathMappingsEditDialogData } from './api-v4-path-mappings-edit-dialog.component';

import { ApiV4, HttpListener } from '../../../entities/management-api-v2';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { onlyApiV4Filter } from '../../../util/apiFilter.operator';

@Component({
  selector: 'api-v4-path-mappings',
  imports: [GioBannerModule, GioConfirmDialogModule, GioIconsModule, MatButtonModule, MatIconModule, MatTableModule, MatTooltipModule],
  templateUrl: './api-v4-path-mappings.component.html',
  styleUrl: './api-v4-path-mappings.component.scss',
})
export class ApiV4PathMappingsComponent {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly apiV2Service = inject(ApiV2Service);
  private readonly matDialog = inject(MatDialog);
  private readonly snackBarService = inject(SnackBarService);
  private readonly permissionService = inject(GioPermissionService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly apiId = this.activatedRoute.snapshot.params.apiId as string;

  protected readonly displayedColumns = ['path', 'actions'];

  private readonly api = toSignal(
    this.apiV2Service.getLastApiFetch(this.apiId).pipe(
      distinctUntilChanged(isEqual),
      map(api => {
        if (api.definitionVersion !== 'V4') {
          this.snackBarService.error('Only V4 APIs are supported.');
          return null;
        }
        return api;
      }),
      catchError(error => {
        this.snackBarService.error(readErrorMessage(error));
        return of(null);
      }),
    ),
  );

  protected readonly isLoading = computed(() => this.api() === undefined);

  protected readonly loadUnavailable = computed(() => this.api() === null);

  protected readonly isReadOnly = computed(() => {
    const api = this.api();
    if (api?.definitionVersion !== 'V4') {
      return true;
    }
    return api.definitionContext?.origin === 'KUBERNETES' || !this.permissionService.hasAnyMatching(['api-definition-u']);
  });

  protected readonly pathMappings = computed(() =>
    storedPathMappings(this.v4Api())
      .slice()
      .sort((a, b) => a.localeCompare(b)),
  );

  protected addPathMapping(): void {
    const api = this.v4Api();
    if (!api) {
      return;
    }
    this.matDialog
      .open<ApiV4PathMappingsAddDialogComponent, ApiV4PathMappingsAddDialogData, ApiV4PathMappingsAddDialogResult>(
        ApiV4PathMappingsAddDialogComponent,
        {
          width: GIO_DIALOG_WIDTH.MEDIUM,
          data: { apiId: api.id, existing: storedPathMappings(api) },
          id: 'addPathMappingDialog',
        },
      )
      .afterClosed()
      .pipe(
        filter((result): result is ApiV4PathMappingsAddDialogResult => result != null),
        switchMap(result => this.saveAddedMapping(result)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  protected editPathMapping(path: string): void {
    const api = this.v4Api();
    if (!api) {
      return;
    }
    this.matDialog
      .open<ApiV4PathMappingsEditDialogComponent, ApiV4PathMappingsEditDialogData, string>(ApiV4PathMappingsEditDialogComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
        data: { path, existing: storedPathMappings(api) },
        id: 'editPathMappingDialog',
      })
      .afterClosed()
      .pipe(
        filter((updated): updated is string => typeof updated === 'string' && updated.length > 0),
        switchMap(updated => {
          const change = (mappings: string[]) => replaceMapping(mappings, path, updated);
          return this.saveMappings(change, 'The path mapping has been successfully updated!', path);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  protected deletePathMapping(path: string): void {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        width: GIO_DIALOG_WIDTH.MEDIUM,
        data: {
          title: 'Delete path mapping',
          content: `Are you sure you want to delete the path mapping <strong>${escapeHtml(path)}</strong>?`,
          confirmButton: 'Delete',
        },
        role: 'alertdialog',
        id: 'deletePathMappingConfirmDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirm => confirm === true),
        switchMap(() =>
          this.saveMappings(
            mappings => mappings.filter(mapping => mapping !== path),
            `The path mapping ${path} has been successfully deleted!`,
            path,
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private saveAddedMapping(result: ApiV4PathMappingsAddDialogResult) {
    if (result.action === 'import') {
      return this.apiV2Service.importPathMappings(this.apiId, { pageId: result.pageId, overwrite: result.overwrite }).pipe(
        tap(() => {
          this.snackBarService.success('The path mappings have been successfully imported!');
        }),
        catchError(error => {
          this.snackBarService.error(readErrorMessage(error));
          return EMPTY;
        }),
      );
    }
    return this.saveMappings(mappings => [...mappings, result.path], 'The path mapping has been successfully added!');
  }

  private saveMappings(change: (mappings: string[]) => string[], successMessage: string, targetPath?: string) {
    return this.apiV2Service.get(this.apiId).pipe(
      onlyApiV4Filter(this.snackBarService),
      switchMap(api => {
        if (api.definitionVersion !== 'V4') {
          return EMPTY;
        }
        const stored = storedPathMappings(api);
        if (!firstHttpListener(api)) {
          this.snackBarService.error('This API has no HTTP listener to store path mappings.');
          return EMPTY;
        }
        if (targetPath !== undefined && !stored.includes(targetPath)) {
          this.snackBarService.error('The path mapping no longer exists.');
          return EMPTY;
        }
        const next = change(stored);
        if (sameMappings(stored, next)) {
          return EMPTY;
        }
        return this.apiV2Service.update(api.id, replaceFirstHttpListenerPathMappings(api, next));
      }),
      tap(() => {
        this.snackBarService.success(successMessage);
      }),
      catchError(error => {
        this.snackBarService.error(readErrorMessage(error));
        return EMPTY;
      }),
    );
  }

  private v4Api(): ApiV4 | undefined {
    const api = this.api();
    return api?.definitionVersion === 'V4' ? api : undefined;
  }
}

function storedPathMappings(api: ApiV4 | undefined): string[] {
  const listener = firstHttpListener(api);
  return listener?.pathMappings ? [...listener.pathMappings] : [];
}

function firstHttpListener(api: ApiV4 | undefined): HttpListener | undefined {
  return api?.listeners.find((listener): listener is HttpListener => listener.type === 'HTTP');
}

function sameMappings(left: string[], right: string[]): boolean {
  return left.length === right.length && left.every((value, index) => value === right[index]);
}

function replaceMapping(mappings: string[], current: string, updated: string): string[] {
  return mappings.map(mapping => (mapping === current ? updated : mapping));
}

function replaceFirstHttpListenerPathMappings(api: ApiV4, pathMappings: string[]): ApiV4 {
  let replaced = false;
  return {
    ...api,
    listeners: api.listeners.map(listener => {
      if (!replaced && listener.type === 'HTTP') {
        replaced = true;
        return { ...listener, pathMappings };
      }
      return listener;
    }),
  };
}

function escapeHtml(value: string): string {
  return value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;').replaceAll("'", '&#x27;');
}

function readErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const message = (error.error as { message?: unknown } | null)?.message;
    if (typeof message === 'string') {
      return message;
    }
    return error.message;
  }
  return 'An error occurred while updating path mappings.';
}
