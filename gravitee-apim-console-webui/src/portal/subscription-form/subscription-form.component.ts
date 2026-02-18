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

import { Component, computed, DestroyRef, effect, HostListener, inject, signal, untracked, WritableSignal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { catchError, filter, startWith, switchMap, tap } from 'rxjs/operators';
import { EMPTY } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDialog } from '@angular/material/dialog';
import {
  GIO_DIALOG_WIDTH,
  GioConfirmDialogComponent,
  GioConfirmDialogData,
  GioFormSlideToggleModule,
} from '@gravitee/ui-particles-angular';

import { PortalHeaderComponent } from '../components/header/portal-header.component';
import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { SubscriptionForm } from '../../entities/management-api-v2';
import { SubscriptionFormService } from '../../services-ngx/subscription-form.service';
import { HasUnsavedChanges } from '../../shared/guards/has-unsaved-changes.guard';
import { normalizeContent } from '../../shared/utils/content.util';

@Component({
  selector: 'subscription-form',
  imports: [
    PortalHeaderComponent,
    ReactiveFormsModule,
    GmdFormEditorComponent,
    MatButtonModule,
    MatTooltipModule,
    MatSlideToggleModule,
    GioFormSlideToggleModule,
  ],
  templateUrl: './subscription-form.component.html',
  styleUrl: './subscription-form.component.scss',
  providers: [...provideGmdFormStore()],
})
export class SubscriptionFormComponent implements HasUnsavedChanges {
  private readonly snackbarService = inject(SnackBarService);
  private readonly subscriptionFormService = inject(SubscriptionFormService);
  private readonly gioPermissionService = inject(GioPermissionService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly matDialog = inject(MatDialog);
  private readonly store = inject(GMD_FORM_STATE_STORE);

  private readonly subscriptionForm: WritableSignal<SubscriptionForm | null> = signal(null);
  private readonly canUpdate = signal(this.gioPermissionService.hasAnyMatching(['environment-settings-u']));
  contentControl = new FormControl<string>('', { nonNullable: true });
  enabledControl = new FormControl<boolean>(false, { nonNullable: true });

  private readonly contentValue = toSignal(this.contentControl.valueChanges.pipe(startWith(this.contentControl.value)));
  private readonly subscriptionFormResult = toSignal(
    this.subscriptionFormService.getSubscriptionForm().pipe(
      catchError(({ error }) => {
        this.snackbarService.error(error?.message ?? 'An error occurred while loading the subscription form');
        return EMPTY;
      }),
    ),
    { initialValue: null },
  );
  private readonly enabledControlChanges = toSignal(this.enabledControl.valueChanges.pipe(filter(() => this.canUpdate())), {
    initialValue: undefined,
  });

  protected readonly hasConfigErrors = computed(() => this.store.allConfigErrors().length > 0);
  readonly configErrorsTooltip = 'Fix configuration errors before continuing.';
  readonly subscriptionFormEnabled = computed(() => this.subscriptionForm()?.enabled ?? false);

  readonly enabledControlTooltip = computed(() => {
    if (!this.canUpdate()) return 'You do not have permission to change this.';
    if (this.hasConfigErrors()) return this.configErrorsTooltip;
    return '';
  });

  readonly saveButtonTooltip = computed(() => {
    if (!this.canUpdate()) return 'You do not have permission to update the subscription form.';
    if (this.hasConfigErrors()) return this.configErrorsTooltip;
    return '';
  });

  isSaveDisabled = computed(() => {
    const currentContent = this.contentValue() ?? '';
    const normalized = normalizeContent(currentContent);
    const isEmpty = normalized.length === 0;
    const hasConfigErrors = this.hasConfigErrors();
    const isUnchanged = !this.hasUnsavedChanges();
    return !this.canUpdate() || isEmpty || hasConfigErrors || isUnchanged;
  });

  private readonly controlsDisabledStateEffect = effect(() => {
    const canUpdate = this.canUpdate();
    const hasConfigErrors = this.hasConfigErrors();
    const options = { emitEvent: false };
    canUpdate ? this.contentControl.enable(options) : this.contentControl.disable(options);
    const enableToggle = canUpdate && !hasConfigErrors;
    enableToggle ? this.enabledControl.enable(options) : this.enabledControl.disable(options);
  });

  private readonly subscriptionFormLoadEffect = effect(() => {
    const result = this.subscriptionFormResult();
    if (!result) return;
    this.subscriptionForm.set(result);
    this.contentControl.reset(result.gmdContent || '', { emitEvent: true });
    this.enabledControl.setValue(result.enabled, { emitEvent: false });
  });

  private readonly enabledControlEffect = effect(() => {
    const enabled = this.enabledControlChanges();
    if (enabled === undefined) return;
    untracked(() => this.handleToggleChange(enabled));
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
    const current = normalizeContent(this.contentValue() ?? '');
    const initial = normalizeContent(this.subscriptionForm()?.gmdContent ?? '');
    return current !== initial;
  }

  updateSubscriptionForm(): void {
    const form = this.subscriptionForm();
    if (!form) return;

    this.subscriptionFormService
      .updateSubscriptionForm(form.id, {
        gmdContent: this.contentControl.value,
      })
      .pipe(
        tap(updatedForm => {
          this.snackbarService.success(`The subscription form has been updated successfully`);
          this.subscriptionForm.set(updatedForm);
        }),
        catchError(({ error }) => {
          this.snackbarService.error(error?.message ?? 'An error occurred while updating the subscription form');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private resetEnabledToInitial(): void {
    this.enabledControl.setValue(this.subscriptionFormEnabled(), { emitEvent: false });
  }

  private handleToggleChange(enabled: boolean): void {
    if (this.hasConfigErrors()) {
      this.resetEnabledToInitial();
      return;
    }
    if (this.hasUnsavedChanges()) {
      const action = enabled ? 'Enable' : 'Disable';
      const data: GioConfirmDialogData = {
        title: `${action} subscription form?`,
        content: `You have unsaved changes. Save the form before changing visibility.`,
        confirmButton: `Save and ${action.toLowerCase()}`,
        cancelButton: 'Cancel',
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
          filter(confirmed => {
            if (!confirmed) this.resetEnabledToInitial();
            return confirmed;
          }),
          switchMap(() => this.saveAndToggleEnabled(enabled)),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe();
    } else {
      this.executeToggleEnabled(enabled);
    }
  }

  private saveAndToggleEnabled(enabled: boolean) {
    const form = this.subscriptionForm();
    if (!form) {
      this.resetEnabledToInitial();
      return EMPTY;
    }

    const action = enabled ? 'enable' : 'disable';

    return this.subscriptionFormService
      .updateSubscriptionForm(form.id, {
        gmdContent: this.contentControl.value,
      })
      .pipe(
        tap(updatedForm => this.subscriptionForm.set(updatedForm)),
        switchMap(() =>
          enabled
            ? this.subscriptionFormService.enableSubscriptionForm(form.id)
            : this.subscriptionFormService.disableSubscriptionForm(form.id),
        ),
        tap(updatedForm => {
          this.subscriptionForm.set(updatedForm);
          this.enabledControl.setValue(updatedForm.enabled, { emitEvent: false });
          this.snackbarService.success(`Subscription form has been ${updatedForm.enabled ? 'enabled' : 'disabled'} successfully.`);
        }),
        catchError(({ error }) => {
          this.resetEnabledToInitial();
          this.snackbarService.error(error?.message ?? `Failed to ${action} subscription form.`);
          return EMPTY;
        }),
      );
  }

  private executeToggleEnabled(enabled: boolean): void {
    const form = this.subscriptionForm();
    if (!form) return;

    const action = enabled ? 'enable' : 'disable';
    const data: GioConfirmDialogData = {
      title: `${action.charAt(0).toUpperCase() + action.slice(1)} subscription form?`,
      content: enabled
        ? `This action will enable the subscription form. It will be visible to API consumers in the Developer Portal.`
        : `This action will disable the subscription form. It will no longer be visible in the Developer Portal, but you can enable it again at any time.`,
      confirmButton: action.charAt(0).toUpperCase() + action.slice(1),
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
        filter(confirmed => {
          if (!confirmed) this.resetEnabledToInitial();
          return confirmed;
        }),
        switchMap(() =>
          enabled
            ? this.subscriptionFormService.enableSubscriptionForm(form.id)
            : this.subscriptionFormService.disableSubscriptionForm(form.id),
        ),
        tap(updatedForm => {
          this.subscriptionForm.set(updatedForm);
          this.enabledControl.setValue(updatedForm.enabled, { emitEvent: false });
          this.snackbarService.success(`Subscription form has been ${updatedForm.enabled ? 'enabled' : 'disabled'} successfully.`);
        }),
        catchError(({ error }) => {
          this.resetEnabledToInitial();
          this.snackbarService.error(error?.message ?? `Failed to ${action} subscription form.`);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }
}
