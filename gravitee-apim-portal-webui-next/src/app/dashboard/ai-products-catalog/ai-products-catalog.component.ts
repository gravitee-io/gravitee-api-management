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
import { Component, computed, DestroyRef, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';

import { LoaderComponent } from '../../../components/loader/loader.component';
import { AiProduct } from '../../../entities/ai-product';
import { AiProductService } from '../../../services/ai-product.service';
import { BreadcrumbService } from '../../../services/breadcrumb.service';

@Component({
  selector: 'app-ai-products-catalog',
  standalone: true,
  imports: [LoaderComponent, RouterLink],
  templateUrl: './ai-products-catalog.component.html',
  styleUrl: './ai-products-catalog.component.scss',
})
export default class AiProductsCatalogComponent {
  private readonly aiProductService = inject(AiProductService);
  private readonly breadcrumbService = inject(BreadcrumbService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly productsResource = rxResource({
    stream: () => this.aiProductService.list(),
  });

  isLoading = computed(() => this.productsResource.isLoading());
  products = computed<AiProduct[]>(() => this.productsResource.value()?.data ?? []);

  /** Comma-separated proxy names for a product card. */
  proxyNames(product: AiProduct): string {
    return (product.components ?? [])
      .map(component => component.name)
      .filter((name): name is string => Boolean(name))
      .join(', ');
  }

  constructor() {
    this.breadcrumbService.set([
      { id: 'ai-products', label: $localize`:@@aiProductsBreadcrumb:AI Products`, url: '/dashboard/ai-products' },
      { id: 'ai-products-catalog', label: $localize`:@@aiProductsCatalogBreadcrumb:Catalog`, url: '/dashboard/ai-products/catalog' },
    ]);
    this.destroyRef.onDestroy(() => this.breadcrumbService.clear());
  }
}
