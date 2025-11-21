import { Component, ElementRef, signal, ViewChild, AfterViewInit, DestroyRef, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { GioIconsModule, GioLoaderModule } from '@gravitee/ui-particles-angular';
import { distinctUntilChanged, startWith, tap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject } from 'rxjs';
import { MatOptionModule } from '@angular/material/core';
import { MatCardModule } from '@angular/material/card';
import { CdkScrollable } from '@angular/cdk/scrolling';

export interface SelectOption {
  value: string;
  label: string;
  disabled?: boolean;
}

@Component({
  selector: 'gd-dropdown-search-overlay',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatButtonModule,
    MatCheckboxModule,
    GioIconsModule,
    GioLoaderModule,
    MatOptionModule,
    MatCardModule,
    CdkScrollable,
  ],
  templateUrl: './dropdown-search-overlay.component.html',
  styleUrl: './dropdown-search-overlay.component.scss',
})
export class DropdownSearchOverlayComponent implements OnInit, AfterViewInit {
  // Input from gio-select-search component
  options = signal<SelectOption[]>([]);
  selectedValues: string[] = [];
  placeholder = 'Search...';
  isLoading = false;
  hasNextPage = false;

  searchControl = new FormControl('');

  @ViewChild('searchInput', { static: false }) searchInput!: ElementRef<HTMLInputElement>;
  @ViewChild(CdkScrollable) scrollable: CdkScrollable | undefined;

  // Output to gio-select-search component
  selectionChange = new Subject<string>();
  clearSelectionChange = new Subject<void>();
  searchChange = new Subject<string>();
  loadMoreChange = new Subject<void>();
  close = new Subject<void>();

  // Scroll handling for automatic load more
  private readonly scrollThreshold = 100; // pixels from bottom to trigger load more

  private readonly destroyRef = inject(DestroyRef);

  ngOnInit() {
    this.searchControl.valueChanges
      .pipe(
        startWith(''),
        distinctUntilChanged(),
        tap(value => {
          this.scrollToTop();
          this.searchChange.next(value || '');
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  ngAfterViewInit() {
    this.scrollable
      ?.elementScrolled()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (!this.scrollable || this.isLoading || !this.hasNextPage) {
          return;
        }

        const element = this.scrollable?.getElementRef().nativeElement;
        const scrollPosition = element.scrollTop + element.clientHeight;
        const scrollHeight = element.scrollHeight;

        // Check if user has scrolled near the bottom
        if (scrollHeight - scrollPosition <= this.scrollThreshold) {
          this.loadMoreChange.next();
        }
      });
  }

  toggleOption(option: SelectOption) {
    if (option.disabled) return;
    this.selectionChange.next(option.value);
  }

  clearSelection() {
    this.clearSelectionChange.next();
  }

  private scrollToTop() {
    if (this.scrollable) {
      const element = this.scrollable.getElementRef().nativeElement;
      element.scrollTop = 0;
    }
  }
}
