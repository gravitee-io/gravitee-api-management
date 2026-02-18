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
import { UntypedFormControl } from '@angular/forms';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { isEqual } from 'lodash';
import { merge, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, map, scan, startWith, takeUntil } from 'rxjs/operators';

export interface Sort {
  active?: string;
  /** The sort direction */
  direction: 'asc' | 'desc' | '';
}

export interface GioTableWrapperPagination {
  /** The pagination index start with 1 */
  index: number;
  /** The pagination size */
  size: number;
}

export interface GioTableWrapperFilters {
  searchTerm: string;
  sort?: Sort;
  pagination: GioTableWrapperPagination;
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
  templateUrl: './gio-table-wrapper.component.html',
  styleUrls: ['./gio-table-wrapper.component.scss'],
  standalone: false,
})
export class GioTableWrapperComponent implements AfterViewInit, OnChanges {
  // Change filters value
  // Emit filtersChange on change
  @Input()
  filters: GioTableWrapperFilters = INITIAL_FILTERS_VALUE;

  @Input()
  searchLabel = 'Search';

  /** The current total number of items being paged (only for display) */
  @Input()
  length = 0;

  /** Disable search input */
  @Input()
  disableSearchInput = false;

  @Input()
  disablePageSize = false;

  /** Pagination available page size options */
  @Input()
  paginationPageSizeOptions = [5, 10, 25, 100];

  // Combine the paginator, sort and filter into a single output
  // Alway sent initial filters values
  @Output()
  filtersChange = new EventEmitter<GioTableWrapperFilters>();

  @ViewChild('paginatorTop') paginatorTop: MatPaginator;
  @ViewChild('paginatorBottom') paginatorBottom: MatPaginator;

  inputSearch = new UntypedFormControl(this.filters.searchTerm ?? INITIAL_FILTERS_VALUE.searchTerm);

  @ContentChild(MatSort)
  sort?: MatSort;

  private stateChanges = new Subject<void>();
  private unsubscribe$ = new Subject<boolean>();

  constructor(private changeDetectorRef: ChangeDetectorRef) {}

  ngOnChanges(changes: SimpleChanges): void {
    // Update values on changes - defer to avoid ExpressionChangedAfterItHasBeenCheckedError
    if (changes.filters && !isEqual(changes.filters.currentValue, changes.filters.previousValue)) {
      setTimeout(() => {
        this.initPaginator(this.filters?.pagination);
        this.initSearch(this.filters?.searchTerm);
        this.initSort(this.filters?.sort);
        this.stateChanges.next();
        this.changeDetectorRef.detectChanges();
      });
    }
  }

  ngAfterViewInit() {
    // Defer init to next tick to avoid ExpressionChangedAfterItHasBeenCheckedError
    // when MatSortHeader's aria-sort updates from programmatic sort initialization
    setTimeout(() => {
      this.initPaginator(this.filters?.pagination ?? INITIAL_FILTERS_VALUE.pagination);
      this.initSearch(this.filters?.searchTerm ?? INITIAL_FILTERS_VALUE.searchTerm);
      this.initSort(this.filters?.sort ?? INITIAL_FILTERS_VALUE.sort);
      this.changeDetectorRef.detectChanges();
    });

    // Keep top and bottom paginator in sync.
    this.paginatorTop.page.pipe(takeUntil(this.unsubscribe$)).subscribe(page => {
      this.paginatorBottom.pageIndex = page.pageIndex;
      this.paginatorBottom.pageSize = page.pageSize;
    });
    this.paginatorBottom.page.pipe(takeUntil(this.unsubscribe$)).subscribe(page => {
      this.paginatorTop.pageIndex = page.pageIndex;
      this.paginatorTop.pageSize = page.pageSize;
    });

    // Merge : stateChange, paginator, sort, inputSearch into single observable
    const observableToMerge = [
      this.stateChanges,
      this.inputSearch.valueChanges,
      this.paginatorBottom.page,
      this.paginatorTop.page,
      this.sort?.sortChange,
    ];

    merge(...observableToMerge.filter(observable => !!observable))
      .pipe(
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
        takeUntil(this.unsubscribe$),
      )
      .subscribe(filters => {
        this.filters = filters;
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
