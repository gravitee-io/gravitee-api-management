/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { Component, computed, DestroyRef, effect, inject, signal } from '@angular/core';
import { rxResource, takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { ActivatedRoute, Router } from '@angular/router';
import { of, switchMap } from 'rxjs';

import { LoaderComponent } from '../../../components/loader/loader.component';
import { AiProduct, AiProductPlan } from '../../../entities/ai-product';
import { AiProductService } from '../../../services/ai-product.service';
import { ApplicationService } from '../../../services/application.service';
import { BreadcrumbService } from '../../../services/breadcrumb.service';
import { CurrentUserService } from '../../../services/current-user.service';
import { SubscriptionService } from '../../../services/subscription.service';

@Component({
  selector: 'app-ai-product-catalog-details',
  standalone: true,
  imports: [LoaderComponent, MatButton],
  templateUrl: './ai-product-catalog-details.component.html',
  styleUrl: './ai-product-catalog-details.component.scss',
})
export default class AiProductCatalogDetailsComponent {
  private readonly aiProductService = inject(AiProductService);
  private readonly subscriptionService = inject(SubscriptionService);
  private readonly applicationService = inject(ApplicationService);
  private readonly currentUserService = inject(CurrentUserService);
  private readonly breadcrumbService = inject(BreadcrumbService);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  private readonly params = toSignal(this.activatedRoute.params, { initialValue: {} as Record<string, string> });
  readonly productId = computed(() => this.params()['productId'] ?? '');

  private readonly productResource = rxResource({
    params: () => (this.productId() ? { id: this.productId() } : undefined),
    stream: ({ params }) => this.aiProductService.get(params.id),
  });

  isLoading = computed(() => this.productResource.isLoading());
  product = computed<AiProduct | undefined>(() => this.productResource.value());
  plans = computed<AiProductPlan[]>(() => this.product()?.plans ?? []);
  /** The plan a new subscriber will request — the product's (first) published plan. */
  selectedPlan = computed<AiProductPlan | undefined>(() => this.plans()[0]);

  isSubscribing = signal(false);
  errorMessage = signal<string | null>(null);

  constructor() {
    this.breadcrumbService.set([
      { id: 'ai-products', label: $localize`:@@aiProductsBreadcrumb:AI Products`, url: '/dashboard/ai-products' },
      { id: 'ai-products-catalog', label: $localize`:@@aiProductsCatalogBreadcrumb:Catalog`, url: '/dashboard/ai-products/catalog' },
    ]);
    // Append the product name to the breadcrumb once it loads.
    effect(() => {
      const product = this.product();
      if (product) {
        this.breadcrumbService.set([
          { id: 'ai-products', label: $localize`:@@aiProductsBreadcrumb:AI Products`, url: '/dashboard/ai-products' },
          { id: 'ai-products-catalog', label: $localize`:@@aiProductsCatalogBreadcrumb:Catalog`, url: '/dashboard/ai-products/catalog' },
          { id: 'ai-product', label: product.name, url: `/dashboard/ai-products/catalog/${product.id}` },
        ]);
      }
    });
    this.destroyRef.onDestroy(() => this.breadcrumbService.clear());
  }

  /**
   * Request a subscription to the product's published plan. Reuses the user's first application
   * (creating one if they have none) and asks for a unique key per subscription (EXCLUSIVE).
   * The subscription starts PENDING; an admin approves it and sets the user's limits.
   */
  subscribe(): void {
    const plan = this.selectedPlan();
    if (!plan || this.isSubscribing()) {
      return;
    }
    this.isSubscribing.set(true);
    this.errorMessage.set(null);

    this.applicationService
      .list(1, 100, true)
      .pipe(
        switchMap(response => {
          const user = this.currentUserService.user();
          // IMPORTANT: subscribe with an application the user OWNS — otherwise they can't read their own
          // subscription/keys on the portal (the detail page requires read permission on the application).
          const owned = (response.data ?? []).find(application => application.owner?.id && application.owner.id === user.id);
          if (owned) {
            return of(owned);
          }
          const name =
            user.display_name || [user.first_name, user.last_name].filter(Boolean).join(' ') || $localize`:@@aiProductDefaultApplicationName:My application`;
          return this.applicationService.create({
            name,
            description: 'Application for AI Products access',
            settings: { app: { type: 'Other' } },
            api_key_mode: 'EXCLUSIVE',
          });
        }),
        switchMap(application =>
          this.subscriptionService.subscribe({
            application: application.id,
            plan: plan.id,
            api_key_mode: 'EXCLUSIVE',
            request: 'Requested from the AI Products catalog',
          }),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.isSubscribing.set(false);
          // Land on the subscribed list, where the new request shows as Pending.
          this.router.navigate(['/dashboard/ai-products']);
        },
        error: (error: HttpErrorResponse) => {
          this.isSubscribing.set(false);
          this.errorMessage.set(error?.error?.message ?? $localize`:@@aiProductSubscribeError:Could not request access. Please try again.`);
        },
      });
  }
}
