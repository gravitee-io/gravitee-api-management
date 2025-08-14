import { Component } from '@angular/core';
import { BehaviorSubject, Observable, of, merge, combineLatest } from 'rxjs';
import { delay, switchMap, tap, map, startWith, filter } from 'rxjs/operators';
import {
  GioSelectSearchComponent,
  SelectOption
} from './gio-select-search.component';
import { MatCardModule } from '@angular/material/card';
import { AsyncPipe } from '@angular/common';

const httpStatusOptions: SelectOption[] = Array.from({ length: 50 }, (_, i) => ({
  value: `${(i + 1) * 10}`,
  label: `HTTP Status ${(i + 1) * 10}`,
}));

@Component({
  selector: 'app-wrapper-for-story',
  standalone: true,
  imports: [MatCardModule, GioSelectSearchComponent, AsyncPipe],
  template: `
    @if (componentState$ | async; as state) {
      <mat-card style="width: 500px; padding: 20px;">
        <h3>Loading States and Pagination</h3>
        <p style="color: #666; margin-bottom: 15px;">
          This example demonstrates automatic loading states:
          <br>• Search loading shows when typing (with 1s delay)
          <br>• Pagination loading shows when scrolling near the bottom (with 500ms delay)
          <br>• Load more is automatically triggered when scrolling near the bottom
          <br>• Try typing "2" or "4" to see search loading, or scroll down to trigger pagination
        </p>

        <gio-select-search
          [options]="state.options"
          [isLoading]="state.isLoading"
          [hasNextPage]="state.hasNextPage"
          label="Options with Loading"
          placeholder="Search options..."
          (loadMore)="onLoadMore()"
          (searchChange)="onSearchChange($event)"
          (opened)="onOpen()"
          (closed)="onClose()"
        />

        <div style="margin-top: 20px; font-size: 12px; color: #666;">
          <div>Component State: {{ isOpen ? 'Open' : 'Closed' }}</div>
          <div>Loading: {{ state.isLoading ? 'Yes' : 'No' }}</div>
          <div>Has Next Page: {{ state.hasNextPage ? 'Yes' : 'No' }}</div>
          <div>Last Search: {{ lastSearch || 'None' }}</div>
          <div>Displayed Options: {{ state.options.length }}</div>
          <div>Total Options: {{ allOptions.length }}</div>
        </div>
      </mat-card>
    }
  `,
})
export class GioSelectSearchWrapperStoryComponent {
  // Public properties
  allOptions: SelectOption[] = httpStatusOptions;
  lastSearch = '';
  loadMoreCount = 0;
  isOpen = false;

  // Private properties
  private isOpenSubject = new BehaviorSubject(false);
  private searchParams = new BehaviorSubject<{ searchTerm: string; start: number; end: number }>({
    searchTerm: '',
    start: 0,
    end: 10,
  });
  private accumulatedOptions: SelectOption[] = [];
  // private loadedOptionValues: Set<string> = new Set();

  // Observable for component state
  componentState$: Observable<{ options: SelectOption[]; isLoading: boolean; hasNextPage: boolean }> = combineLatest([
    this.searchParams.asObservable(),
    this.isOpenSubject.asObservable()
  ]).pipe(
    map(([searchParams, isOpen]) => ({ ...searchParams, isOpen })),
    switchMap(({ searchTerm, start, end, isOpen }) => {
      // If component is closed, return default state
      if (!isOpen) {
        return of({ options: [], isLoading: false, hasNextPage: false });
      }

      if (start === 0) {
        this.accumulatedOptions = [];
      }

      const loadingState = of({
        options: this.accumulatedOptions,
        isLoading: true,
        hasNextPage: false
      });

      return merge(loadingState, this.requestPageFromBackend(searchTerm, start, end));
    })
  );

  // Public methods
  onLoadMore(): void {
    const currentEnd = this.searchParams.value.end;
    const newEnd = currentEnd + 10;

    this.searchParams.next({
      ...this.searchParams.value,
      start: currentEnd,
      end: newEnd
    });

    this.loadMoreCount++;
  }

  onSearchChange(searchTerm: string): void {
    this.lastSearch = searchTerm;

    if (searchTerm && searchTerm.length > 0) {
      this.searchParams.next({
        searchTerm,
        start: 0,
        end: 10
      });
      console.log('Search started for:', searchTerm);
    } else if (searchTerm === '') {
      console.log('Search cleared, preserving accumulated options:', this.accumulatedOptions.length);
      // When clearing search, don't reset pagination - just update search term
      // This will trigger the observable to return accumulated options
      this.searchParams.next({
        searchTerm: '',
        start: this.searchParams.value.start,
        end: this.searchParams.value.end
      });

      // console.log('Search cleared, preserving accumulated options:', this.accumulatedOptions.length);
      // console.log('Current pagination state:', { start: this.searchParams.value.start, end: this.searchParams.value.end });
    }
  }

  // Component open/close methods
  onOpen(): void {
    console.log('Component opened');
    this.isOpen = true;
    this.isOpenSubject.next(true);
    // Trigger initial load when opening
    this.searchParams.next({
      searchTerm: '',
      start: 0,
      end: 10
    });
  }

  onClose(): void {
    this.isOpen = false;
    this.isOpenSubject.next(false);
  }

  private accumulateNewOptions(newOptions: SelectOption[], start: number, searchTerm: string): { options: SelectOption[]; isLoading: boolean; hasNextPage: boolean } {
    if (start === 0) {
      this.accumulatedOptions = [...newOptions];
    } else {
      const newUniqueOptions = newOptions.filter(option =>
        !this.accumulatedOptions.some(({ value }) => value === option.value)
      );
      this.accumulatedOptions = [...this.accumulatedOptions, ...newUniqueOptions];
    }

    const hasNextPage = searchTerm === '' && this.searchParams.value.end < this.allOptions.length;

    return {
      options: this.accumulatedOptions,
      isLoading: false,
      hasNextPage: hasNextPage
    };
  }

  /**
   * Private methods to mock how the backend would handle pagination and search
   */

  private requestPageFromBackend(searchTerm: string, start: number, end: number): Observable<{ options: SelectOption[]; isLoading: boolean; hasNextPage: boolean }> {
    return this.getOptionsSubset(searchTerm, start, end).pipe(
      map(newOptions => this.accumulateNewOptions(newOptions, start, searchTerm))
    );
  }

  // Return an observable of select options with search term and start and end
  private getOptionsSubset(searchTerm: string, start: number, end: number): Observable<SelectOption[]> {
    if (searchTerm && searchTerm.length > 0) {
      return of(this.allOptions.filter(option =>
        option.label.toLowerCase().includes(searchTerm.toLowerCase())
      ).slice(start, end)).pipe(delay(500));
    } else {
      return of(this.allOptions.slice(start, end)).pipe(delay(500));
    }
  }
}
