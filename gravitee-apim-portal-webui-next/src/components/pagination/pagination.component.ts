/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { Component, computed, input, output, Signal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatFormField } from '@angular/material/form-field';
import { MatOption, MatSelect } from '@angular/material/select';

const MAX_VISIBLE_WITHOUT_ELLIPSIS = 7;
const WINDOW_SIZE = 5;
type PaginationItem = number | 'ellipsis';

interface PaginationVM {
  hasPreviousPage: boolean;
  hasNextPage: boolean;
  currentPage: number;
  totalPages: number;
}

@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [MatButton, MatFormField, MatOption, MatSelect],
  templateUrl: './pagination.component.html',
  styleUrl: './pagination.component.scss',
})
export class PaginationComponent {
  totalResults = input.required<number>();
  currentPage = input.required<number>();
  pageSize = input<number>(10);
  pageSizeOptions = input<number[]>([]);

  selectPage = output<number>();
  pageSizeChange = output<number>();

  pagination: Signal<PaginationVM> = computed(() => {
    const totalPages = Math.ceil(this.totalResults() / this.pageSize());

    return {
      hasPreviousPage: this.currentPage() > 1,
      hasNextPage: this.currentPage() < totalPages,
      currentPage: this.currentPage(),
      totalPages,
    };
  });

  pageNumbers: Signal<PaginationItem[]> = computed(() => {
    const { currentPage, totalPages } = this.pagination();
    if (totalPages <= 0) return [];
    if (totalPages <= MAX_VISIBLE_WITHOUT_ELLIPSIS) {
      return Array.from({ length: totalPages }, (_, i) => i + 1);
    }
    let start = Math.max(1, currentPage - Math.floor(WINDOW_SIZE / 2));
    const end = Math.min(totalPages, start + WINDOW_SIZE - 1);
    if (end - start + 1 < WINDOW_SIZE) {
      start = Math.max(1, end - WINDOW_SIZE + 1);
    }
    const result: PaginationItem[] = [];
    result.push(1);
    if (start > 2) {
      result.push('ellipsis');
    }
    for (let p = start; p <= end; p++) {
      if (p !== 1 && p !== totalPages) {
        result.push(p);
      }
    }
    if (end < totalPages - 1) {
      result.push('ellipsis');
    }
    if (totalPages > 1) {
      result.push(totalPages);
    }
    return result;
  });

  goToPage(page: number) {
    const totalPages = this.pagination().totalPages;
    if (totalPages <= 0) {
      return;
    }
    const boundedPage = Math.max(1, Math.min(page, totalPages));
    if (boundedPage === this.currentPage()) {
      return;
    }
    this.selectPage.emit(boundedPage);
  }

  goToPreviousPage() {
    if (this.currentPage() > 1) {
      this.selectPage.emit(this.currentPage() - 1);
    }
  }

  goToNextPage() {
    const { currentPage, totalPages } = this.pagination();
    if (totalPages > 0 && currentPage < totalPages) {
      this.selectPage.emit(currentPage + 1);
    }
  }

  onPageSizeChange(size: number) {
    this.pageSizeChange.emit(size);
  }
}
