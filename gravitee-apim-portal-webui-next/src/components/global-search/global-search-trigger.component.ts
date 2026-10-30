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
import { Component, HostListener, inject, input } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

import { GlobalSearchDialogComponent } from './global-search-dialog.component';

@Component({
  selector: 'app-global-search-trigger',
  imports: [MatIconModule],
  templateUrl: './global-search-trigger.component.html',
  styleUrl: './global-search-trigger.component.scss',
})
export class GlobalSearchTriggerComponent {
  compact = input(false);

  private readonly matDialog = inject(MatDialog);

  @HostListener('window:keydown', ['$event'])
  handleKeyboardShortcut(event: KeyboardEvent): void {
    if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
      event.preventDefault();
      this.openSearch();
    }
  }

  openSearch(): void {
    if (this.matDialog.openDialogs.some(dialog => dialog.componentInstance instanceof GlobalSearchDialogComponent)) {
      return;
    }

    this.matDialog.open(GlobalSearchDialogComponent, {
      panelClass: 'global-search-dialog-panel',
      backdropClass: 'global-search-dialog-backdrop',
      autoFocus: false,
      maxWidth: '95vw',
      width: '752px',
    });
  }
}
