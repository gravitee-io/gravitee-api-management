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
import { GraviteeMarkdownEditorModule } from '@gravitee/gravitee-markdown';

import { Component, computed, DestroyRef, effect, inject, Signal, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { catchError, startWith, tap } from 'rxjs/operators';
import { EMPTY, Observable } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';

import { PortalHeaderComponent } from '../components/header/portal-header.component';
import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { PortalPagesService } from '../../services-ngx/portal-pages.service';
import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { PortalPageWithDetails } from '../../entities/portal/portal-page-with-details';

@Component({
  selector: 'homepage',
  imports: [PortalHeaderComponent, ReactiveFormsModule, GraviteeMarkdownEditorModule, MatButtonModule, MatTooltipModule],
  templateUrl: './homepage.component.html',
  styleUrl: './homepage.component.scss',
})
export class HomepageComponent {
  contentControl = new FormControl({
    value: '',
    disabled: true,
  });
  isSaveDisabled = computed(() => {
    const currentContent = this.contentValue();
    const savedContent = this.portalHomepage().content;
    const isEmpty = !currentContent?.trim();
    return !this.canUpdate() || isEmpty || currentContent === savedContent;
  });

  private readonly snackbarService = inject(SnackBarService);
  private readonly portalPagesService = inject(PortalPagesService);
  private readonly gioPermissionService = inject(GioPermissionService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly canUpdate = signal(this.gioPermissionService.hasAnyMatching(['environment-documentation-u']));
  private readonly portalHomepage: Signal<PortalPageWithDetails> = toSignal(this.getPortalHomepage(), {
    initialValue: { content: '' } as PortalPageWithDetails,
  });
  private contentValue = toSignal(this.contentControl.valueChanges.pipe(startWith(this.contentControl.value)));

  constructor() {
    effect(() => {
      this.contentControl.setValue(this.portalHomepage().content);
    });

    effect(() => {
      if (this.canUpdate()) {
        this.contentControl.enable();
      }
    });
  }

  updatePortalPage(): void {
    this.portalPagesService
      .patchPortalPage(this.portalHomepage().id, {
        content: this.contentControl.value,
      })
      .pipe(
        tap((_) => this.snackbarService.success(`The page has been updated successfully`)),
        catchError(() => {
          this.snackbarService.error('An error occurred while updating the homepage');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private getPortalHomepage(): Observable<PortalPageWithDetails> {
    return this.portalPagesService.getHomepage().pipe(
      catchError(() => {
        this.snackbarService.error('An error occurred while loading the homepage');
        return EMPTY;
      }),
    );
  }
}
