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
import { catchError, filter, startWith, switchMap, tap } from 'rxjs/operators';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';

import { SubscriptionFormListComponent } from './subscription-form-list/subscription-form-list.component';

import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { GioPermissionModule } from '../../shared/components/gio-permission/gio-permission.module';
import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { SubscriptionForm } from '../../entities/management-api-v2';
import { SubscriptionFormService } from '../../services-ngx/subscription-form.service';
import { HasUnsavedChanges } from '../../shared/guards/has-unsaved-changes.guard';
import { confirmDiscardChanges, normalizeContent } from '../../shared/utils/content.util';

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
    MatTooltipModule,
    GioPermissionModule,
    GmdFormEditorComponent,
    SubscriptionFormListComponent,
  ],
  templateUrl: './subscription-form.component.html',
  styleUrl: './subscription-form.component.scss',
  providers: [provideGmdFormStore()],
})
export class SubscriptionFormComponent implements HasUnsavedChanges {
  private readonly snackbarService = inject(SnackBarService);
  private readonly subscriptionFormService = inject(SubscriptionFormService);
  private readonly gioPermissionService = inject(GioPermissionService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly matDialog = inject(MatDialog);
  private readonly ngZone = inject(NgZone);
  private readonly store = inject(GMD_FORM_STATE_STORE);

  private readonly MIN_PANEL_WIDTH = 280;
  private readonly MAX_PANEL_WIDTH = 600;
  panelWidth = signal(500);

  readonly canUpdate = signal(this.gioPermissionService.hasAnyMatching(['environment-metadata-u']));
  private readonly refreshList = new BehaviorSubject<void>(undefined);
  readonly forms = toSignal(
    this.refreshList.pipe(
      switchMap(() =>
        this.subscriptionFormService.list().pipe(
          catchError(({ error }) => {
            this.snackbarService.error(error?.message ?? 'An error occurred while loading subscription forms.');
            return of([] as SubscriptionForm[]);
          }),
        ),
      ),
    ),
    { initialValue: [] as SubscriptionForm[] },
  );

  /** `'new'` while creating an unsaved form; a form id while viewing/editing one; `null` before anything is ever selected. */
  private readonly selectedFormId = signal<string | 'new' | null>(null);
  readonly isCreating = computed(() => this.selectedFormId() === 'new');
  readonly hasSelection = computed(() => this.selectedFormId() !== null);
  readonly selectedForm = computed<SubscriptionForm | null>(() => {
    const id = this.selectedFormId();
    if (id === null || id === 'new') return null;
    return this.forms().find(form => form.id === id) ?? null;
  });

  readonly nameControl = new FormControl<string>('', { nonNullable: true, validators: [Validators.required] });
  readonly contentControl = new FormControl<string>('', { nonNullable: true });

  private readonly initialName = signal('');
  private readonly initialContent = signal('');

  // Keyed purely on the selected id (not on the wider `forms` list) so an unrelated row's toggle
  // refreshing the list never clobbers in-progress edits open in this panel.
  private readonly selectedFormDetail = toSignal(
    toObservable(this.selectedFormId).pipe(
      switchMap(id => {
        if (id === null || id === 'new') return of(null);
        return this.subscriptionFormService.get(id).pipe(
          catchError(({ error }) => {
            this.snackbarService.error(error?.message ?? 'An error occurred while loading the subscription form.');
            return of(null);
          }),
        );
      }),
    ),
    { initialValue: null },
  );

  private readonly nameValue = toSignal(this.nameControl.valueChanges.pipe(startWith(this.nameControl.value)));
  private readonly contentValue = toSignal(this.contentControl.valueChanges.pipe(startWith(this.contentControl.value)));

  readonly selectedFormEnabled = computed(() => this.selectedForm()?.enabled ?? false);
  readonly selectedFormIsDefault = computed(() => this.selectedForm()?.defaultForm ?? false);

  readonly saveButtonLabel = computed(() => (this.isCreating() ? 'Create' : 'Save'));

  protected readonly hasConfigErrors = computed(() => this.store.criticalConfigErrors().length > 0);

  readonly isSaveDisabled = computed(() => {
    if (this.selectedFormId() === null) return true;
    const name = (this.nameValue() ?? '').trim();
    const content = normalizeContent(this.contentValue());
    if (name.length === 0 || content.length === 0 || this.hasConfigErrors()) return true;
    return !this.hasUnsavedChanges();
  });

  private readonly controlsDisabledStateEffect = effect(() => {
    const canUpdate = this.canUpdate();
    const hasSelection = this.selectedFormId() !== null;
    const options = { emitEvent: false };
    const shouldEnable = canUpdate && hasSelection;
    shouldEnable ? this.nameControl.enable(options) : this.nameControl.disable(options);
    shouldEnable ? this.contentControl.enable(options) : this.contentControl.disable(options);
  });

  private readonly formLoadEffect = effect(() => {
    if (this.isCreating()) {
      untracked(() => {
        this.initialName.set('');
        this.initialContent.set('');
        this.nameControl.reset('', { emitEvent: true });
        this.contentControl.reset('', { emitEvent: true });
      });
      return;
    }
    const form = this.selectedFormDetail();
    if (!form) return;
    untracked(() => {
      this.initialName.set(form.name);
      this.initialContent.set(form.gmdContent || '');
      this.nameControl.reset(form.name, { emitEvent: true });
      this.contentControl.reset(form.gmdContent || '', { emitEvent: true });
    });
  });

  /** Selects the environment default (or the first row) once, the first time the list loads. */
  private readonly autoSelectEffect = effect(() => {
    const forms = this.forms();
    untracked(() => {
      if (this.selectedFormId() !== null || forms.length === 0) return;
      const defaultForm = forms.find(form => form.defaultForm) ?? forms[0];
      this.selectedFormId.set(defaultForm.id);
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
    if (this.selectedFormId() === null) return false;
    const currentName = (this.nameValue() ?? '').trim();
    const currentContent = normalizeContent(this.contentValue());
    return currentName !== this.initialName().trim() || currentContent !== normalizeContent(this.initialContent());
  }

  selectForm(form: SubscriptionForm): void {
    this.checkUnsavedChangesAndRun(() => this.selectedFormId.set(form.id));
  }

  startCreate(): void {
    this.checkUnsavedChangesAndRun(() => this.selectedFormId.set('new'));
  }

  save(): void {
    if (this.isSaveDisabled()) return;
    const save$ = this.isCreating() ? this.createForm() : this.updateForm(this.selectedForm()!);
    save$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe();
  }

  onEnabledToggle(form: SubscriptionForm): void {
    const enabling = !form.enabled;
    const action = enabling ? 'Enable' : 'Disable';
    const data: GioConfirmDialogData = {
      title: `${action} subscription form?`,
      content: enabling
        ? `This action will enable "${form.name}". API consumers will see it in the Developer Portal when subscribing to the APIs it applies to.`
        : `This action will disable "${form.name}". It will no longer be shown to API consumers in the Developer Portal, but you can enable it again at any time.`,
      confirmButton: action,
    };

    this.confirm(data)
      .pipe(
        switchMap(() => this.toggleEnabled(form, enabling)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  setAsDefault(): void {
    const form = this.selectedForm();
    if (!form || form.defaultForm) return;
    const data: GioConfirmDialogData = {
      title: 'Set as default subscription form?',
      content: `"${form.name}" will be used for every API without a dedicated subscription form. The current default form will no longer be.`,
      confirmButton: 'Set as default',
    };

    this.confirm(data)
      .pipe(
        switchMap(() =>
          this.subscriptionFormService.setDefault(form.id).pipe(
            tap(() => {
              this.snackbarService.success(`Subscription form "${form.name}" is now the default.`);
              this.refreshList.next();
            }),
            catchError(({ error }) => {
              this.snackbarService.error(error?.message ?? 'Failed to set the subscription form as default.');
              return EMPTY;
            }),
          ),
        ),
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

  private confirm(data: GioConfirmDialogData): Observable<boolean> {
    return this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        width: GIO_DIALOG_WIDTH.SMALL,
        data,
        role: 'alertdialog',
        id: 'confirmDialog',
      })
      .afterClosed()
      .pipe(filter(confirmed => !!confirmed));
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
    const name = this.nameControl.value.trim();
    const gmdContent = this.contentControl.value;
    return this.subscriptionFormService.create({ name, gmdContent }).pipe(
      tap(created => {
        this.snackbarService.success('Subscription form created successfully.');
        this.initialName.set(created.name);
        this.initialContent.set(created.gmdContent);
        this.selectedFormId.set(created.id);
        this.refreshList.next();
      }),
      catchError(({ error }) => {
        this.snackbarService.error(error?.message ?? 'An error occurred while creating the subscription form.');
        return EMPTY;
      }),
    );
  }

  private updateForm(form: SubscriptionForm): Observable<unknown> {
    const name = this.nameControl.value.trim();
    const gmdContent = this.contentControl.value;
    return this.subscriptionFormService.update(form.id, { name, gmdContent }).pipe(
      tap(updated => {
        this.snackbarService.success('Subscription form updated successfully.');
        this.initialName.set(updated.name);
        this.initialContent.set(updated.gmdContent);
        this.refreshList.next();
      }),
      catchError(({ error }) => {
        this.snackbarService.error(error?.message ?? 'An error occurred while updating the subscription form.');
        return EMPTY;
      }),
    );
  }

  private toggleEnabled(form: SubscriptionForm, enabled: boolean): Observable<unknown> {
    const request$ = enabled ? this.subscriptionFormService.enable(form.id) : this.subscriptionFormService.disable(form.id);
    return request$.pipe(
      tap(() => {
        this.snackbarService.success(`Subscription form "${form.name}" has been ${enabled ? 'enabled' : 'disabled'} successfully.`);
        this.refreshList.next();
      }),
      catchError(({ error }) => {
        this.snackbarService.error(error?.message ?? `Failed to ${enabled ? 'enable' : 'disable'} subscription form.`);
        return EMPTY;
      }),
    );
  }
}
