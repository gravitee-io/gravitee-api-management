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
import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ContentChild,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { merge, of, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, map, scan, startWith, takeUntil } from 'rxjs/operators';

export interface GioTableWrapperFilters {
  searchTerm: string;
  sort?: {
    /** The id of the column being sorted */
    active: string;
    /** The sort direction */
    direction: 'asc' | 'desc' | '';
  };
  pagination: {
    /** The pagination index start with 1 */
    index: number;
    /** The pagination size */
    size: number;
  };
}

const INITIAL_FILTERS_VALUE: GioTableWrapperFilters = {
  pagination: {
    index: 1,
    size: 10,
  },
  searchTerm: '',
};

@Component({
  selector: 'gio-table-wrapper',
  template: require('./gio-table-wrapper.component.html'),
  styles: [require('./gio-table-wrapper.component.scss')],
})
export class GioTableWrapperComponent implements AfterViewInit, OnChanges {
  // Change filters value
  // Emit filtersChange on change
  @Input()
  filters: GioTableWrapperFilters = INITIAL_FILTERS_VALUE;

  /** The current total number of items being paged (only for display) */
  @Input()
  length = 0;

  /** Disable search input */
  @Input()
  disableSearchInput = false;

  // Combine the paginator, sort and filter into a single output
  // Alway sent initial filters values
  @Output()
  filtersChange = new EventEmitter<GioTableWrapperFilters>();

  @ViewChild('paginatorTop') paginatorTop: MatPaginator;
  @ViewChild('paginatorBottom') paginatorBottom: MatPaginator;

  inputSearch = new FormControl(this.filters.searchTerm ?? INITIAL_FILTERS_VALUE.searchTerm);

  @ContentChild(MatSort)
  sort?: MatSort;

  private stateChanges = new Subject<void>();
  private unsubscribe$ = new Subject<boolean>();

  constructor(private changeDetectorRef: ChangeDetectorRef) {}

  ngOnChanges(changes: SimpleChanges): void {
    // Update values on changes
    if (changes.filters) {
      this.initPaginator(this.filters?.pagination);

      this.initSearch(this.filters?.searchTerm);

      this.initSort(this.filters?.sort);

      this.stateChanges.next();
      this.changeDetectorRef.detectChanges();
    }
  }

  ngAfterViewInit() {
    // Init values with filters or default initial values
    this.initPaginator(this.filters?.pagination ?? INITIAL_FILTERS_VALUE.pagination);

    this.initSearch(this.filters?.searchTerm ?? INITIAL_FILTERS_VALUE.searchTerm);
    this.initSort(this.filters?.sort ?? INITIAL_FILTERS_VALUE.sort);
    this.changeDetectorRef.detectChanges();

    // Keep top and bottom paginator in sync.
    this.paginatorTop.page.pipe(takeUntil(this.unsubscribe$)).subscribe((page) => {
      this.paginatorBottom.pageIndex = page.pageIndex;
      this.paginatorBottom.pageSize = page.pageSize;
    });
    this.paginatorBottom.page.pipe(takeUntil(this.unsubscribe$)).subscribe((page) => {
      this.paginatorTop.pageIndex = page.pageIndex;
      this.paginatorTop.pageSize = page.pageSize;
    });

    // Merge : stateChange, paginator, sort, inputSearch into single observable
    merge(
      this.stateChanges,
      this.inputSearch.valueChanges,
      this.paginatorBottom.page,
      this.paginatorTop.page,
      this.sort?.sortChange ?? of(),
    )
      .pipe(
        takeUntil(this.unsubscribe$),
        map(() => {
          // In each event get all values
          const filters: GioTableWrapperFilters = {
            searchTerm: this.inputSearch.value,
            ...(this.sort ? { sort: { active: this.sort.active, direction: this.sort.direction } } : {}),
            pagination: {
              // paginatorTop is used as master. keep it in sync before
              index: this.paginatorTop.pageIndex + 1,
              size: this.paginatorTop.pageSize,
            },
          };
          return filters;
        }),
        distinctUntilChanged(),
        scan((prev, curr) => {
          if (prev.searchTerm !== curr.searchTerm) {
            const firstIndexPagination = {
              ...curr.pagination,
              index: 1,
            };
            this.initPaginator(firstIndexPagination);
            return {
              ...curr,
              pagination: firstIndexPagination,
            };
          }
          return curr;
        }),
        debounceTime(300),
        // Alway start with initial filters values
        startWith(this.filters ?? INITIAL_FILTERS_VALUE),
      )
      .subscribe((filters) => {
        this.filtersChange.emit(filters);
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.complete();
  }

  private initSearch(searchTerm: GioTableWrapperFilters['searchTerm']) {
    if (this.inputSearch) {
      this.inputSearch.setValue(searchTerm ?? '', { emitEvent: false });
    }
  }

  private initPaginator(pagination: GioTableWrapperFilters['pagination']) {
    if (this.paginatorTop && this.paginatorBottom && pagination) {
      this.paginatorTop.pageIndex = pagination.index - 1;
      this.paginatorTop.pageSize = pagination.size;
      this.paginatorBottom.pageIndex = pagination.index - 1;
      this.paginatorBottom.pageSize = pagination.size;
    }
  }

  private initSort(sort: GioTableWrapperFilters['sort']) {
    if (this.sort && sort) {
      this.sort.direction = sort.direction;
      this.sort.active = sort.active;
    }
  }
}
