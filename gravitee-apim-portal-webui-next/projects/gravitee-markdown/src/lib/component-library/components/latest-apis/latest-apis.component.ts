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
import { Component, input, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Api } from '../../../services/api-search.service';
import { ApiSearchFactory } from '../../../services/api-search.factory';
import { CardComponent } from '../card/card.component';
import { CardActionsComponent } from '../card/card-actions.component';
import { ButtonComponent } from '../button/button.component';

@Component({
  selector: 'app-latest-apis',
  standalone: true,
  imports: [CommonModule, CardComponent, CardActionsComponent, ButtonComponent],
  templateUrl: './latest-apis.component.html',
  styleUrls: ['./latest-apis.component.scss']
})
export class LatestApisComponent implements OnInit {
  // Content inputs
  title = input<string>('Discover the latest APIs');
  subtitle = input<string>('');
  
  // Configuration inputs
  maxApis = input<number>(5);
  category = input<string>('all');
  searchQuery = input<string>('');
  
  // Card styling inputs
  cardElevation = input<0 | 1 | 2 | 3 | 4 | 5>(0);
  cardBackgroundColor = input<string>('#ffffff');
  cardBorderRadius = input<string>('8px');
  
  // Action button inputs
  actionButtonText = input<string>('View Details');
  actionButtonVariant = input<'filled' | 'outlined' | 'text'>('outlined');
  actionButtonType = input<'internal' | 'external'>('internal');
  
  // State signals
  apis = signal<Api[]>([]);
  loading = signal<boolean>(false);
  error = signal<string>('');

  // Computed property for API details URLs
  apiDetailsUrls = computed(() => {
    return this.apis().map(api => ({
      id: api.id,
      url: `/apis/${api.id}`
    }));
  });

  private apiSearchFactory = inject(ApiSearchFactory);

  ngOnInit() {
    this.loadApis();
  }

  private loadApis() {
    this.loading.set(true);
    this.error.set('');

    this.apiSearchFactory.getService().search(1, this.category(), this.searchQuery(), this.maxApis())
      .subscribe({
        next: (response: any) => {
          this.apis.set(response.data);
          this.loading.set(false);
        },
        error: (err: any) => {
          this.error.set(err.message || 'Failed to load APIs');
          this.loading.set(false);
        }
      });
  }
} 