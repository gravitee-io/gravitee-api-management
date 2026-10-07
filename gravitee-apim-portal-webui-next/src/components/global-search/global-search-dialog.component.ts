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
import { AfterViewInit, Component, DestroyRef, ElementRef, inject, signal, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
import { catchError, debounceTime, distinctUntilChanged, of, switchMap, tap } from 'rxjs';

import { GlobalSearchResult, PortalGlobalSearchService } from '../../services/portal-global-search.service';
import { LoaderComponent } from '../loader/loader.component';

@Component({
  selector: 'app-global-search-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatIconModule, MatInputModule, LoaderComponent],
  templateUrl: './global-search-dialog.component.html',
  styleUrl: './global-search-dialog.component.scss',
})
export class GlobalSearchDialogComponent implements AfterViewInit {
  @ViewChild('searchInput') searchInput?: ElementRef<HTMLInputElement>;

  private readonly destroyRef = inject(DestroyRef);
  private readonly dialogRef = inject(MatDialogRef<GlobalSearchDialogComponent>);
  private readonly globalSearchService = inject(PortalGlobalSearchService);
  private readonly router = inject(Router);

  protected readonly searchControl = new FormControl<string>('', { nonNullable: true });
  protected readonly results = signal<GlobalSearchResult[]>([]);
  protected readonly loading = signal(false);
  protected readonly hasQuery = signal(false);

  constructor() {
    this.searchControl.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        tap(query => {
          const trimmed = query.trim();
          this.hasQuery.set(!!trimmed);
          this.loading.set(!!trimmed);
          if (!trimmed) {
            this.results.set([]);
          }
        }),
        switchMap(query => {
          const trimmed = query.trim();
          if (!trimmed) {
            return of([] as GlobalSearchResult[]);
          }
          return this.globalSearchService.search(trimmed).pipe(catchError(() => of([] as GlobalSearchResult[])));
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(results => {
        this.results.set(results);
        this.loading.set(false);
      });
  }

  ngAfterViewInit(): void {
    queueMicrotask(() => this.searchInput?.nativeElement.focus());
  }

  close(): void {
    this.dialogRef.close();
  }

  selectResult(result: GlobalSearchResult): void {
    this.dialogRef.close();
    this.router.navigate(['/documentation', result.rootId], { queryParams: { selectedId: result.navItemId } });
  }

  resultIcon(kind: GlobalSearchResult['kind']): string {
    switch (kind) {
      case 'API':
        return 'api';
      case 'API_PRODUCT':
        return 'inventory_2';
      case 'FOLDER':
        return 'folder';
      case 'PAGE':
        return 'description';
      default:
        return 'search';
    }
  }

  resultKindLabel(kind: GlobalSearchResult['kind']): string {
    switch (kind) {
      case 'API':
        return $localize`:@@globalSearchResultKindApi:API`;
      case 'API_PRODUCT':
        return $localize`:@@globalSearchResultKindApiProduct:API product`;
      case 'FOLDER':
        return $localize`:@@globalSearchResultKindFolder:Folder`;
      case 'PAGE':
        return $localize`:@@globalSearchResultKindPage:Page`;
      default:
        return '';
    }
  }
}
