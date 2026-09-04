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
import { GMD_FORM_STATE_STORE, GmdFormEditorComponent, provideGmdFormStore } from '@gravitee/gravitee-markdown';

import { Component, computed, DestroyRef, effect, HostListener, inject, NgZone, signal, untracked } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { BehaviorSubject, EMPTY, Observable, of } from 'rxjs';
import { catchError, filter, map, startWith, switchMap, tap } from 'rxjs/operators';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';

import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { GioPermissionModule } from '../../shared/components/gio-permission/gio-permission.module';
import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { PortalNavigationSubscriptionForm, UpdateSubscriptionFormPortalNavigationItem } from '../../entities/management-api-v2';
import { PortalNavigationItemService } from '../../services-ngx/portal-navigation-item.service';
import { PortalPageContentService } from '../../services-ngx/portal-page-content.service';
import { GioTableWrapperFilters } from '../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GioTableWrapperModule } from '../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { HasUnsavedChanges } from '../../shared/guards/has-unsaved-changes.guard';
import { confirmDiscardChanges, normalizeContent } from '../../shared/utils/content.util';

interface SubscriptionFormRow {
  item: PortalNavigationSubscriptionForm;
}

@Component({
  selector: 'subscription-form',
  imports: [
    EmptyStateComponent,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatTableModule,
    MatSlideToggleModule,
    GioPermissionModule,
    GioTableWrapperModule,
    GmdFormEditorComponent,
  ],
  templateUrl: './subscription-form.component.html',
  styleUrl: './subscription-form.component.scss',
  providers: [provideGmdFormStore()],
})
export class SubscriptionFormComponent implements HasUnsavedChanges {
  private readonly snackbarService = inject(SnackBarService);
  private readonly portalNavigationItemService = inject(PortalNavigationItemService);
  private readonly portalPageContentService = inject(PortalPageContentService);
  private readonly gioPermissionService = inject(GioPermissionService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly matDialog = inject(MatDialog);
  private readonly ngZone = inject(NgZone);
  private readonly store = inject(GMD_FORM_STATE_STORE);

  private readonly MIN_PANEL_WIDTH = 280;
  private readonly MAX_PANEL_WIDTH = 600;
  panelWidth = signal(500);

  readonly canUpdate = signal(this.gioPermissionService.hasAnyMatching(['environment-metadata-u']));
  readonly displayedColumns = ['title', 'published'];

  readonly filters = signal<GioTableWrapperFilters>({ pagination: { index: 1, size: 10 }, searchTerm: '' });

  private readonly refreshList = new BehaviorSubject<number>(1);
  private readonly forms = toSignal(
    this.refreshList.pipe(
      switchMap(() => this.portalNavigationItemService.getNavigationItems('SUBSCRIPTION_FORM')),
      map(response => response.items as PortalNavigationSubscriptionForm[]),
      catchError(({ error }) => {
        this.snackbarService.error(error?.message ?? 'An error occurred while loading subscription forms.');
        return of([] as PortalNavigationSubscriptionForm[]);
      }),
    ),
    { initialValue: [] as PortalNavigationSubscriptionForm[] },
  );

  private readonly rows = computed<SubscriptionFormRow[]>(() => this.forms().map(item => ({ item })));

  private readonly filteredRows = computed<SubscriptionFormRow[]>(() => {
    const term = this.filters().searchTerm?.trim().toLowerCase() ?? '';
    const rows = this.rows();
    return term ? rows.filter(row => row.item.title.toLowerCase().includes(term)) : rows;
  });

  readonly total = computed(() => this.filteredRows().length);

  readonly pagedRows = computed<SubscriptionFormRow[]>(() => {
    const { index, size } = this.filters().pagination;
    const start = (index - 1) * size;
    return this.filteredRows().slice(start, start + size);
  });

  /** `'new'` while creating an unsaved form; an item id while viewing/editing one; `null` before anything is ever selected. */
  private readonly selectedItemId = signal<string | 'new' | null>(null);
  readonly isCreating = computed(() => this.selectedItemId() === 'new');
  readonly hasSelection = computed(() => this.selectedItemId() !== null);
  readonly selectedItem = computed<PortalNavigationSubscriptionForm | null>(() => {
    const id = this.selectedItemId();
    if (id === null || id === 'new') return null;
    return this.forms().find(form => form.id === id) ?? null;
  });

  readonly titleControl = new FormControl<string>('', { nonNullable: true, validators: [Validators.required] });
  readonly contentControl = new FormControl<string>('', { nonNullable: true });

  private readonly initialTitle = signal('');
  private readonly initialContent = signal('');

  // Keyed purely on the selected id (not on the wider `forms` list) so an unrelated row's publish
  // toggle refreshing the list never clobbers in-progress edits open in this panel.
  private readonly selectedContentResult = toSignal(
    toObservable(this.selectedItemId).pipe(
      switchMap(id => {
        if (id === null || id === 'new') return of(null);
        const item = this.forms().find(form => form.id === id);
        if (!item) return of(null);
        return this.portalPageContentService.getPageContent(item.portalPageContentId).pipe(
          map(content => ({ item, content })),
          catchError(({ error }) => {
            this.snackbarService.error(error?.message ?? 'An error occurred while loading the subscription form.');
            return of(null);
          }),
        );
      }),
    ),
    { initialValue: null },
  );

  private readonly titleValue = toSignal(this.titleControl.valueChanges.pipe(startWith(this.titleControl.value)));
  private readonly contentValue = toSignal(this.contentControl.valueChanges.pipe(startWith(this.contentControl.value)));

  readonly selectedItemIsPublished = computed(() => this.selectedItem()?.published ?? false);

  readonly saveButtonLabel = computed(() => (this.isCreating() ? 'Create' : 'Save'));

  protected readonly hasConfigErrors = computed(() => this.store.criticalConfigErrors().length > 0);

  readonly isSaveDisabled = computed(() => {
    if (this.selectedItemId() === null) return true;
    const title = (this.titleValue() ?? '').trim();
    const content = normalizeContent(this.contentValue());
    if (title.length === 0 || content.length === 0 || this.hasConfigErrors()) return true;
    return !this.hasUnsavedChanges();
  });

  private readonly controlsDisabledStateEffect = effect(() => {
    const canUpdate = this.canUpdate();
    const hasSelection = this.selectedItemId() !== null;
    const options = { emitEvent: false };
    const shouldEnable = canUpdate && hasSelection;
    shouldEnable ? this.titleControl.enable(options) : this.titleControl.disable(options);
    shouldEnable ? this.contentControl.enable(options) : this.contentControl.disable(options);
  });

  private readonly formLoadEffect = effect(() => {
    if (this.isCreating()) {
      untracked(() => {
        this.initialTitle.set('');
        this.initialContent.set('');
        this.titleControl.reset('', { emitEvent: true });
        this.contentControl.reset('', { emitEvent: true });
      });
      return;
    }
    const result = this.selectedContentResult();
    if (!result) return;
    untracked(() => {
      this.initialTitle.set(result.item.title);
      this.initialContent.set(result.content.content || '');
      this.titleControl.reset(result.item.title, { emitEvent: true });
      this.contentControl.reset(result.content.content || '', { emitEvent: true });
    });
  });

  /** Selects the environment default (or the first row) once, the first time the list loads. */
  private readonly autoSelectEffect = effect(() => {
    const forms = this.forms();
    untracked(() => {
      if (this.selectedItemId() !== null || forms.length === 0) return;
      const defaultForm = forms.find(form => form.published) ?? forms[0];
      this.selectedItemId.set(defaultForm.id);
    });
  });

  @HostListener('window:beforeunload', ['$event'])
  beforeUnloadHandler(event: BeforeUnloadEvent) {
    if (this.hasUnsavedChanges()) {
      event.preventDefault();
      event.returnValue = '';
      return '';
    }
  }

  hasUnsavedChanges(): boolean {
    if (this.selectedItemId() === null) return false;
    const currentTitle = (this.titleValue() ?? '').trim();
    const currentContent = normalizeContent(this.contentValue());
    return currentTitle !== this.initialTitle().trim() || currentContent !== normalizeContent(this.initialContent());
  }

  onFiltersChanged(filters: GioTableWrapperFilters): void {
    this.filters.set(filters);
  }

  selectItem(row: SubscriptionFormRow): void {
    this.checkUnsavedChangesAndRun(() => this.selectedItemId.set(row.item.id));
  }

  startCreate(): void {
    this.checkUnsavedChangesAndRun(() => this.selectedItemId.set('new'));
  }

  save(): void {
    if (this.isSaveDisabled()) return;
    const save$ = this.isCreating() ? this.createForm() : this.updateForm(this.selectedItem()!);
    save$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe();
  }

  onPublishToggle(row: SubscriptionFormRow): void {
    const item = row.item;
    const publishing = !item.published;
    const action = publishing ? 'Publish' : 'Unpublish';
    const data: GioConfirmDialogData = {
      title: `${action} subscription form?`,
      content: publishing
        ? `This action will publish "${item.title}". It will become the environment's default subscription form shown to API consumers in the Developer Portal. Any currently published form must be unpublished first.`
        : `This action will unpublish "${item.title}". It will no longer be shown to API consumers in the Developer Portal.`,
      confirmButton: action,
    };

    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        width: GIO_DIALOG_WIDTH.SMALL,
        data,
        role: 'alertdialog',
        id: 'confirmDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirmed => !!confirmed),
        switchMap(() => this.togglePublished(item, publishing)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  onResizeStart(event: MouseEvent): void {
    event.preventDefault();

    const startX = event.clientX;
    const startWidth = this.panelWidth();

    document.body.style.cursor = 'col-resize';
    document.body.style.userSelect = 'none';

    this.ngZone.runOutsideAngular(() => {
      const onMove = (e: MouseEvent) => {
        const deltaX = e.clientX - startX;
        const newWidth = Math.max(this.MIN_PANEL_WIDTH, Math.min(this.MAX_PANEL_WIDTH, startWidth + deltaX));

        this.ngZone.run(() => this.panelWidth.set(newWidth));
      };

      const onUp = () => {
        document.removeEventListener('mousemove', onMove);
        document.removeEventListener('mouseup', onUp);
        document.body.style.cursor = '';
        document.body.style.userSelect = '';
      };

      document.addEventListener('mousemove', onMove);
      document.addEventListener('mouseup', onUp);
    });
  }

  private checkUnsavedChangesAndRun(action: () => void): void {
    if (!this.hasUnsavedChanges()) {
      action();
      return;
    }

    confirmDiscardChanges(this.matDialog)
      .pipe(
        filter(confirmed => !!confirmed),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => action());
  }

  private createForm(): Observable<unknown> {
    const title = this.titleControl.value.trim();
    const content = this.contentControl.value;
    // No portalPageContentId is sent: the backend auto-creates default GRAVITEE_MARKDOWN content
    // (mirroring how PAGE items are created), which is then immediately overwritten below with the
    // form's actually-authored content via the already-existing content-update endpoint.
    return this.portalNavigationItemService
      .createNavigationItem({
        type: 'SUBSCRIPTION_FORM',
        area: 'SUBSCRIPTION_FORM',
        title,
        visibility: 'PUBLIC',
      })
      .pipe(
        switchMap(created =>
          this.portalPageContentService
            .updatePageContent((created as PortalNavigationSubscriptionForm).portalPageContentId, { content })
            .pipe(map(() => created)),
        ),
        tap(created => {
          this.snackbarService.success('Subscription form created successfully.');
          this.initialTitle.set(title);
          this.initialContent.set(content);
          this.selectedItemId.set(created.id);
          this.refreshList.next(1);
        }),
        catchError(({ error }) => {
          this.snackbarService.error(error?.message ?? 'An error occurred while creating the subscription form.');
          return EMPTY;
        }),
      );
  }

  private updateForm(item: PortalNavigationSubscriptionForm): Observable<unknown> {
    const newTitle = this.titleControl.value.trim();
    const newContent = this.contentControl.value;
    const titleChanged = newTitle !== item.title;
    const title$ = titleChanged
      ? this.portalNavigationItemService.updateNavigationItem(item.id, this.buildUpdatePayload(item, item.published, newTitle))
      : of(item);

    return this.portalPageContentService.updatePageContent(item.portalPageContentId, { content: newContent }).pipe(
      switchMap(() => title$),
      tap(() => {
        this.snackbarService.success('Subscription form updated successfully.');
        this.initialTitle.set(newTitle);
        this.initialContent.set(newContent);
        this.refreshList.next(1);
      }),
      catchError(({ error }) => {
        this.snackbarService.error(error?.message ?? 'An error occurred while updating the subscription form.');
        return EMPTY;
      }),
    );
  }

  private togglePublished(item: PortalNavigationSubscriptionForm, published: boolean): Observable<unknown> {
    return this.portalNavigationItemService.updateNavigationItem(item.id, this.buildUpdatePayload(item, published, item.title)).pipe(
      tap(() => {
        this.snackbarService.success(`Subscription form "${item.title}" has been ${published ? 'published' : 'unpublished'} successfully.`);
        this.refreshList.next(1);
      }),
      catchError(({ error }) => {
        this.snackbarService.error(error?.message ?? `Failed to ${published ? 'publish' : 'unpublish'} subscription form.`);
        return EMPTY;
      }),
    );
  }

  private buildUpdatePayload(
    item: PortalNavigationSubscriptionForm,
    published: boolean,
    title: string,
  ): UpdateSubscriptionFormPortalNavigationItem {
    return {
      type: 'SUBSCRIPTION_FORM',
      title,
      order: item.order,
      published,
      visibility: item.visibility,
    };
  }
}
