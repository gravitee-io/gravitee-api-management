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
import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { ReactiveFormsModule, Validators, FormControl, FormGroup } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { GioFormSelectionInlineModule, GioSaveBarModule } from '@gravitee/ui-particles-angular';
import { MatAnchor } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { tap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { UiPortalMenuLinksService } from '../../../../services-ngx/ui-portal-menu-links.service';
import {
  PortalMenuLink,
  PortalMenuLinkVisibility,
  toReadableMenuLinkType,
  UpdatePortalMenuLink,
} from '../../../../entities/management-api-v2';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

@Component({
  selector: 'menu-link-edit',
  templateUrl: './menu-link-edit.component.html',
  styleUrls: ['./menu-link-edit.component.scss'],
  imports: [
    CommonModule,
    GioSaveBarModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
    MatAnchor,
    MatIcon,
    RouterLink,
    GioFormSelectionInlineModule,
  ],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MenuLinkEditComponent implements OnInit {
  menuLinkEditForm = new FormGroup({
    name: new FormControl<string>('', { validators: Validators.required }),
    target: new FormControl<string>('', { validators: Validators.required }),
    visibility: new FormControl<PortalMenuLinkVisibility>('PUBLIC', { validators: Validators.required }),
  });

  initialValue: PortalMenuLink;
  readableMenuLinkType: string;

  private destroyRef: DestroyRef = inject(DestroyRef);

  constructor(
    private activatedRoute: ActivatedRoute,
    private readonly uiPortalMenuLinksService: UiPortalMenuLinksService,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.uiPortalMenuLinksService
      .get(this.activatedRoute.snapshot.params.menuLinkId)
      .pipe(
        tap((portalMenuLink) => {
          this.initialValue = portalMenuLink;
          this.readableMenuLinkType = toReadableMenuLinkType(portalMenuLink.type);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((_) => this.reset());
  }

  reset() {
    this.menuLinkEditForm.controls.name.setValue(this.initialValue.name);
    this.menuLinkEditForm.controls.target.setValue(this.initialValue.target);
    this.menuLinkEditForm.controls.visibility.setValue(this.initialValue.visibility);
    this.menuLinkEditForm.markAsPristine();
  }

  submit() {
    const updatedPortalMenuLink: UpdatePortalMenuLink = {
      name: this.menuLinkEditForm.controls.name.value,
      target: this.menuLinkEditForm.controls.target.value,
      visibility: this.menuLinkEditForm.controls.visibility.value,
      order: this.initialValue.order,
    };
    this.uiPortalMenuLinksService
      .update(this.initialValue.id, updatedPortalMenuLink)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        error: ({ error }) => {
          this.snackBarService.error(error?.message ?? 'An error occurred while updating menu link.');
        },
        next: (portalMenuLink) => {
          this.snackBarService.success(`Menu link '${portalMenuLink.name}' updated successfully`);
          this.menuLinkEditForm.markAsPristine();
          this.initialValue = portalMenuLink;
        },
      });
  }
}
