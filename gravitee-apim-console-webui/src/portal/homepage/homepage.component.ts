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

import { Component, computed, DestroyRef, effect, inject, signal, WritableSignal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { catchError, filter, startWith, switchMap, tap } from 'rxjs/operators';
import { EMPTY, Observable } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';

import { PortalHeaderComponent } from '../components/header/portal-header.component';
import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { PortalPagesService } from '../../services-ngx/portal-pages.service';
import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { PortalPageWithDetails } from '../../entities/portal/portal-page-with-details';
import { PortalPagesResponse } from '../../entities/portal/portal-pages-response';

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

  togglePublishActionText = computed(() => {
    return this.portalHomepage()?.published ? 'Unpublish' : 'Publish';
  });

  isSaveDisabled = computed(() => {
    const currentContent = this.contentValue();
    const savedContent = this.portalHomepage()?.content;
    const isEmpty = !currentContent?.trim();
    return !this.canUpdate() || isEmpty || currentContent === savedContent;
  });

  isTogglePublishActive = computed(() => {
    return this.canUpdate() && this.portalHomepage();
  });

  private readonly snackbarService = inject(SnackBarService);
  private readonly portalPagesService = inject(PortalPagesService);
  private readonly gioPermissionService = inject(GioPermissionService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly matDialog = inject(MatDialog);

  private readonly portalHomepage: WritableSignal<PortalPageWithDetails | null> = signal(null);
  private readonly canUpdate = signal(this.gioPermissionService.hasAnyMatching(['environment-documentation-u']));
  private readonly contentValue = toSignal(this.contentControl.valueChanges.pipe(startWith(this.contentControl.value)));

  constructor() {
    this.getPortalHomepage()
      .pipe(
        tap((portalPage) => this.portalHomepage.set(portalPage.pages[0])),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();

    effect(() => {
      this.contentControl.reset(this.portalHomepage()?.content || '');
    });

    effect(() => {
      this.canUpdate() ? this.contentControl.enable() : this.contentControl.disable();
    });
  }

  public togglePublish(): void {
    const isCurrentlyPublished = this.portalHomepage()?.published;
    const pageId = this.portalHomepage()?.id;

    const data: GioConfirmDialogData = {
      title: `${this.togglePublishActionText()} page?`,
      content: isCurrentlyPublished
        ? `This action will unpublish the page. It will no longer be visible on the developer portal, but you can publish it again at any time.`
        : `Your changes will be published. The updated page will be visible on your developer portal.`,
      confirmButton: this.togglePublishActionText(),
    };

    const toggleApiCall$ = isCurrentlyPublished
      ? this.portalPagesService.unpublishPage(pageId)
      : this.portalPagesService.publishPage(pageId);

    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        width: GIO_DIALOG_WIDTH.SMALL,
        data,
        role: 'alertdialog',
        id: 'confirmDialog',
      })
      .afterClosed()
      .pipe(
        filter((confirmed) => confirmed),
        switchMap((_) => toggleApiCall$),
        tap((updatedPage) => {
          this.portalHomepage.set(updatedPage);
          this.snackbarService.success(`Page has been ${updatedPage.published ? 'publish' : 'unpublish'}ed successfully.`);
        }),
        takeUntilDestroyed(this.destroyRef),
        catchError(({ error }) => {
          this.snackbarService.error(error?.message ?? `Failed to ${this.togglePublishActionText().toLowerCase()} page.`);
          return EMPTY;
        }),
      )
      .subscribe();
  }

  updatePortalPage(): void {
    this.portalPagesService
      .patchPortalPage(this.portalHomepage()?.id, {
        content: this.contentControl.value,
      })
      .pipe(
        tap((portalPage) => {
          this.snackbarService.success(`The page has been updated successfully`);
          this.portalHomepage.set(portalPage);
        }),
        catchError(() => {
          this.snackbarService.error('An error occurred while updating the homepage');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private getPortalHomepage(): Observable<PortalPagesResponse> {
    return this.portalPagesService.getHomepage().pipe(
      catchError(() => {
        this.snackbarService.error('An error occurred while loading the homepage');
        return EMPTY;
      }),
    );
  }
}
