/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { catchError, finalize, of } from 'rxjs';

import {
  buildPreferenceHookCategories,
  categoryDisplayName,
  APIKEY_CLOSE_TO_EXPIRY_DAYS_PREFIX,
  APIKEY_CLOSE_TO_EXPIRY_HOOK_ID,
  CERTIFICATE_CLOSE_TO_EXPIRY_DAYS_PREFIX,
  CERTIFICATE_CLOSE_TO_EXPIRY_HOOK_ID,
  CLOSE_TO_EXPIRY_HOOK_IDS,
  DEFAULT_CLOSE_TO_EXPIRY_DAYS,
  MAX_CLOSE_TO_EXPIRY_DAYS,
  MIN_CLOSE_TO_EXPIRY_DAYS,
  parseCloseToExpiryDays,
  persistNotificationHooks,
  PortalNotificationHook,
  SUBSCRIPTION_CLOSE_TO_EXPIRY_DAYS_PREFIX,
  SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK_ID,
  visibleNotificationHooks,
} from '../../../../entities/notification/portal-notification-hook';
import { ApplicationsResponse } from '../../../../entities/application/application';
import { SubscriptionMetadata, SubscriptionsResponse } from '../../../../entities/subscription/subscriptions-response';
import { ApplicationService } from '../../../../services/application.service';
import { ApplicationNotificationService } from '../../../../services/application-notification.service';
import { SubscriptionService } from '../../../../services/subscription.service';

type PreferenceScope = 'APPLICATION' | 'API';

interface SelectableOption {
  id: string;
  name: string;
}

@Component({
  selector: 'app-notification-preferences',
  imports: [
    MatButtonModule,
    MatButtonToggleModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
    MatSelectModule,
    ReactiveFormsModule,
  ],
  templateUrl: './notification-preferences.component.html',
  styleUrl: './notification-preferences.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificationPreferencesComponent {
  private readonly applicationService = inject(ApplicationService);
  private readonly applicationNotificationService = inject(ApplicationNotificationService);
  private readonly subscriptionService = inject(SubscriptionService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly scope = signal<PreferenceScope>('APPLICATION');
  protected readonly selectedApiId = signal<string | null>(null);
  protected readonly selectedApplicationId = signal<string | null>(null);
  protected readonly draftHooks = signal<string[]>([]);
  protected readonly subscriptionDays = signal(DEFAULT_CLOSE_TO_EXPIRY_DAYS);
  protected readonly certificateDays = signal(DEFAULT_CLOSE_TO_EXPIRY_DAYS);
  protected readonly apiKeyDays = signal(DEFAULT_CLOSE_TO_EXPIRY_DAYS);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly successMessage = signal<string | null>(null);

  protected readonly applicationControl = new FormControl<string | null>(null);
  protected readonly apiControl = new FormControl<string | null>(null);

  protected readonly minCloseToExpiryDays = MIN_CLOSE_TO_EXPIRY_DAYS;
  protected readonly maxCloseToExpiryDays = MAX_CLOSE_TO_EXPIRY_DAYS;

  protected readonly applicationsResource = rxResource({
    stream: () => this.applicationService.list(1, 100).pipe(catchError(() => of({ data: [] } as ApplicationsResponse))),
  });

  protected readonly subscriptionsResource = rxResource({
    stream: () =>
      this.subscriptionService
        .list({ size: -1, statuses: ['ACCEPTED', 'PAUSED', 'PENDING'] })
        .pipe(catchError(() => of({ data: [], links: {}, metadata: {} } satisfies SubscriptionsResponse))),
  });

  protected readonly hooksResource = rxResource({
    stream: () => this.applicationNotificationService.getHooks().pipe(catchError(() => of([] as PortalNotificationHook[]))),
  });

  protected readonly applicationOptions = computed<SelectableOption[]>(() =>
    (this.applicationsResource.value()?.data ?? [])
      .map(app => ({ id: app.id, name: app.name }))
      .sort((left, right) => left.name.localeCompare(right.name)),
  );

  protected readonly apiOptions = computed<SelectableOption[]>(() => {
    const response = this.subscriptionsResource.value();
    const metadata: SubscriptionMetadata = response?.metadata ?? {};
    const byApi = new Map<string, SelectableOption>();

    for (const subscription of response?.data ?? []) {
      const apiId = subscription.api ?? subscription.reference_id;
      if (!apiId || byApi.has(apiId)) {
        continue;
      }
      byApi.set(apiId, {
        id: apiId,
        name: metadata[apiId]?.name ?? apiId,
      });
    }

    return [...byApi.values()].sort((left, right) => left.name.localeCompare(right.name));
  });

  private readonly applicationIdForSelectedApi = computed(() => {
    const apiId = this.selectedApiId();
    if (!apiId) {
      return null;
    }

    for (const subscription of this.subscriptionsResource.value()?.data ?? []) {
      const subscriptionApiId = subscription.api ?? subscription.reference_id;
      if (subscriptionApiId === apiId && subscription.application) {
        return subscription.application;
      }
    }

    return null;
  });

  protected readonly effectiveApplicationId = computed(() =>
    this.scope() === 'API' ? this.applicationIdForSelectedApi() : this.selectedApplicationId(),
  );

  protected readonly selectedHooksResource = rxResource({
    params: () => this.effectiveApplicationId(),
    stream: ({ params: applicationId }) => {
      if (!applicationId) {
        return of([] as string[]);
      }
      return this.applicationNotificationService.getNotifications(applicationId).pipe(catchError(() => of([] as string[])));
    },
  });

  protected readonly categoryGroups = computed(() => {
    const categories = buildPreferenceHookCategories(this.hooksResource.value() ?? [], this.scope());
    const enabled = new Set(this.draftHooks());

    return categories.map(category => ({
      name: category.name,
      label: this.categoryLabel(category.name),
      hooks: category.hooks.map(hook => ({
        ...hook,
        enabled: enabled.has(hook.id),
        isCloseToExpiry: CLOSE_TO_EXPIRY_HOOK_IDS.has(hook.id),
      })),
    }));
  });

  protected readonly isLoading = computed(
    () =>
      this.applicationsResource.isLoading() ||
      this.subscriptionsResource.isLoading() ||
      this.hooksResource.isLoading() ||
      this.selectedHooksResource.isLoading(),
  );

  protected readonly hasUnsavedChanges = computed(() => {
    const saved = this.selectedHooksResource.value();
    if (!saved) {
      return false;
    }
    const savedVisible = visibleNotificationHooks(saved).sort().join(',');
    const draftVisible = [...this.draftHooks()].sort().join(',');
    const savedSubscriptionDays = parseCloseToExpiryDays(saved, SUBSCRIPTION_CLOSE_TO_EXPIRY_DAYS_PREFIX);
    const savedCertificateDays = parseCloseToExpiryDays(saved, CERTIFICATE_CLOSE_TO_EXPIRY_DAYS_PREFIX);
    const savedApiKeyDays = parseCloseToExpiryDays(saved, APIKEY_CLOSE_TO_EXPIRY_DAYS_PREFIX);
    return (
      savedVisible !== draftVisible ||
      savedSubscriptionDays !== this.subscriptionDays() ||
      savedCertificateDays !== this.certificateDays() ||
      savedApiKeyDays !== this.apiKeyDays()
    );
  });

  protected readonly canSave = computed(() => !!this.effectiveApplicationId() && this.hasUnsavedChanges() && !this.saving());

  constructor() {
    effect(() => {
      const hooks = this.selectedHooksResource.value();
      if (!this.effectiveApplicationId() || hooks == null) {
        return;
      }
      this.draftHooks.set(visibleNotificationHooks(hooks));
      this.subscriptionDays.set(parseCloseToExpiryDays(hooks, SUBSCRIPTION_CLOSE_TO_EXPIRY_DAYS_PREFIX));
      this.certificateDays.set(parseCloseToExpiryDays(hooks, CERTIFICATE_CLOSE_TO_EXPIRY_DAYS_PREFIX));
      this.apiKeyDays.set(parseCloseToExpiryDays(hooks, APIKEY_CLOSE_TO_EXPIRY_DAYS_PREFIX));
      this.error.set(null);
      this.successMessage.set(null);
    });
  }

  protected setScope(scope: PreferenceScope | string | undefined): void {
    if (scope !== 'APPLICATION' && scope !== 'API') {
      return;
    }
    this.scope.set(scope);
    this.selectedApiId.set(null);
    this.selectedApplicationId.set(null);
    this.applicationControl.setValue(null);
    this.apiControl.setValue(null);
    this.draftHooks.set([]);
    this.clearFeedback();
  }

  protected onApplicationChange(applicationId: string | null): void {
    this.selectedApplicationId.set(applicationId);
    this.clearFeedback();
  }

  protected onApiChange(apiId: string | null): void {
    this.selectedApiId.set(apiId);
    this.draftHooks.set([]);
    this.clearFeedback();
  }

  protected toggleHook(hookId: string, enabled: boolean): void {
    this.draftHooks.update(current => {
      if (enabled) {
        return current.includes(hookId) ? current : [...current, hookId];
      }
      return current.filter(id => id !== hookId);
    });
    this.clearFeedback();
  }

  protected onCloseToExpiryDaysChange(hookId: string, event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    const days = Number(value);
    if (hookId === SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK_ID) {
      this.subscriptionDays.set(days);
    } else if (hookId === CERTIFICATE_CLOSE_TO_EXPIRY_HOOK_ID) {
      this.certificateDays.set(days);
    } else if (hookId === APIKEY_CLOSE_TO_EXPIRY_HOOK_ID) {
      this.apiKeyDays.set(days);
    }
    this.clearFeedback();
  }

  protected closeToExpiryDays(hookId: string): number {
    if (hookId === SUBSCRIPTION_CLOSE_TO_EXPIRY_HOOK_ID) {
      return this.subscriptionDays();
    }
    if (hookId === CERTIFICATE_CLOSE_TO_EXPIRY_HOOK_ID) {
      return this.certificateDays();
    }
    if (hookId === APIKEY_CLOSE_TO_EXPIRY_HOOK_ID) {
      return this.apiKeyDays();
    }
    return DEFAULT_CLOSE_TO_EXPIRY_DAYS;
  }

  protected save(): void {
    const applicationId = this.effectiveApplicationId();
    if (!applicationId || this.saving()) {
      return;
    }

    this.clearFeedback();
    this.saving.set(true);
    const hooks = persistNotificationHooks(this.draftHooks(), this.subscriptionDays(), this.certificateDays(), this.apiKeyDays());

    this.applicationNotificationService
      .updateNotifications(applicationId, hooks)
      .pipe(
        finalize(() => this.saving.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: saved => {
          this.draftHooks.set(visibleNotificationHooks(saved));
          this.subscriptionDays.set(parseCloseToExpiryDays(saved, SUBSCRIPTION_CLOSE_TO_EXPIRY_DAYS_PREFIX));
          this.certificateDays.set(parseCloseToExpiryDays(saved, CERTIFICATE_CLOSE_TO_EXPIRY_DAYS_PREFIX));
          this.apiKeyDays.set(parseCloseToExpiryDays(saved, APIKEY_CLOSE_TO_EXPIRY_DAYS_PREFIX));
          this.selectedHooksResource.reload();
          this.successMessage.set($localize`:@@notificationsPreferencesSaveSuccess:Notification preferences saved.`);
        },
        error: () => {
          this.error.set(
            $localize`:@@notificationsPreferencesSaveError:An error occurred while saving notification preferences. Please try again.`,
          );
        },
      });
  }

  protected reset(): void {
    const hooks = this.selectedHooksResource.value() ?? [];
    this.draftHooks.set(visibleNotificationHooks(hooks));
    this.subscriptionDays.set(parseCloseToExpiryDays(hooks, SUBSCRIPTION_CLOSE_TO_EXPIRY_DAYS_PREFIX));
    this.certificateDays.set(parseCloseToExpiryDays(hooks, CERTIFICATE_CLOSE_TO_EXPIRY_DAYS_PREFIX));
    this.apiKeyDays.set(parseCloseToExpiryDays(hooks, APIKEY_CLOSE_TO_EXPIRY_DAYS_PREFIX));
    this.clearFeedback();
  }

  protected categoryLabel(category: string): string {
    switch (category.toUpperCase()) {
      case 'SUBSCRIPTION':
        return $localize`:@@notificationsPreferencesCategorySubscription:Subscription`;
      case 'APIKEY':
        return $localize`:@@notificationsPreferencesCategoryApiKey:API Key`;
      case 'LIFECYCLE':
        return $localize`:@@notificationsPreferencesCategoryLifecycle:API Lifecycle`;
      case 'CERTIFICATE':
        return $localize`:@@notificationsPreferencesCategoryCertificate:Certificate`;
      default:
        return categoryDisplayName(category);
    }
  }

  private clearFeedback(): void {
    this.error.set(null);
    this.successMessage.set(null);
  }
}
