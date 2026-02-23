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
import { Component, computed, inject, input, output, Signal } from '@angular/core';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { ObservabilityBreakpointService } from '../../services/observability-breakpoint.service';

interface PaginationVM {
  hasPreviousPage: boolean;
  hasNextPage: boolean;
  currentPage: number;
  totalPages: number;
}

@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [MatButton, MatIcon],
  templateUrl: './pagination.component.html',
  styleUrl: './pagination.component.scss',
})
export class PaginationComponent {
  protected readonly isMobile = inject(ObservabilityBreakpointService).isMobile;

  totalResults = input.required<number>();
  currentPage = input.required<number>();
  pageSize = input<number>(10);

  selectPage = output<number>();

  pagination: Signal<PaginationVM> = computed(() => {
    const totalPages = Math.ceil(this.totalResults() / this.pageSize());

    return {
      hasPreviousPage: this.currentPage() > 1,
      hasNextPage: this.currentPage() < totalPages,
      currentPage: this.currentPage(),
      totalPages,
    };
  });

  goToPage(page: number) {
    if (page > 0 && page <= this.pagination().totalPages) {
      this.selectPage.emit(page);
    }
  }

  goToPreviousPage() {
    if (this.currentPage() > 0) {
      this.selectPage.emit(this.currentPage() - 1);
    }
  }

  goToNextPage() {
    if (this.currentPage() < this.pagination().totalPages) {
      this.selectPage.emit(this.currentPage() + 1);
    }
  }
}
