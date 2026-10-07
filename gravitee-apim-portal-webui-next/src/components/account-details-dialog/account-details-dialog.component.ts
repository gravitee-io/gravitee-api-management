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
import { Component, computed, inject, linkedSignal, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { finalize } from 'rxjs';

import { CurrentUserService } from '../../services/current-user.service';
import { User } from '../../entities/user/user';

export interface AccountDetailsDialogData {
  user: User;
}

const MAX_AVATAR_BYTES = 500_000;

@Component({
  selector: 'app-account-details-dialog',
  templateUrl: './account-details-dialog.component.html',
  styleUrl: './account-details-dialog.component.scss',
  imports: [MatDialogModule, MatButtonModule],
  standalone: true,
})
export class AccountDetailsDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<AccountDetailsDialogComponent>);
  private readonly currentUserService = inject(CurrentUserService);
  private readonly data = inject<AccountDetailsDialogData>(MAT_DIALOG_DATA);

  readonly username: string;
  readonly firstName: string;
  readonly lastName: string;
  readonly email: string;
  readonly profileEditable: boolean;
  readonly userId: string;

  readonly pendingAvatarDataUrl = signal<string | null>(null);
  readonly isSaving = signal(false);
  readonly saveError = signal<string | null>(null);
  readonly avatarLoadFailed = linkedSignal<string | undefined, boolean>({
    source: () => this.serverAvatarUrl(),
    computation: () => false,
  });

  readonly serverAvatarUrl = computed(() => this.data.user._links?.avatar);
  readonly avatarPreviewUrl = computed(() => this.pendingAvatarDataUrl() ?? this.serverAvatarUrl());
  readonly showAvatarImage = computed(() => !!this.avatarPreviewUrl() && !this.avatarLoadFailed());
  readonly canSaveAvatar = computed(() => this.profileEditable && !!this.pendingAvatarDataUrl() && !this.isSaving());

  constructor() {
    const user = this.data.user ?? {};
    this.userId = user.id ?? '';
    this.username = user.display_name || user.reference || '—';
    this.firstName = user.first_name || '—';
    this.lastName = user.last_name || '—';
    this.email = user.email || '—';
    this.profileEditable = user.editable_profile === true;
  }

  onAvatarError(): void {
    this.avatarLoadFailed.set(true);
  }

  onAvatarFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file || !this.profileEditable) {
      return;
    }

    this.saveError.set(null);
    if (file.type === 'image/svg+xml' || file.name.toLowerCase().endsWith('.svg')) {
      this.saveError.set($localize`:@@accountDetailsAvatarSvgNotAllowed:SVG images are not supported. Choose a PNG or JPEG file.`);
      return;
    }
    if (!file.type.startsWith('image/')) {
      this.saveError.set($localize`:@@accountDetailsAvatarInvalidType:Choose an image file.`);
      return;
    }
    if (file.size > MAX_AVATAR_BYTES) {
      this.saveError.set($localize`:@@accountDetailsAvatarTooLarge:Image must be 500 KB or smaller.`);
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      const result = reader.result;
      if (typeof result === 'string') {
        this.pendingAvatarDataUrl.set(result);
      }
    };
    reader.onerror = () => {
      this.saveError.set($localize`:@@accountDetailsAvatarReadError:Could not read the selected file.`);
    };
    reader.readAsDataURL(file);
  }

  saveAvatar(): void {
    const avatar = this.pendingAvatarDataUrl();
    if (!this.profileEditable || !avatar || !this.userId) {
      return;
    }

    this.isSaving.set(true);
    this.saveError.set(null);
    this.currentUserService
      .updateProfile({ id: this.userId, avatar })
      .pipe(finalize(() => this.isSaving.set(false)))
      .subscribe({
        next: () => {
          this.pendingAvatarDataUrl.set(null);
          this.dialogRef.close(true);
        },
        error: () => {
          this.saveError.set($localize`:@@accountDetailsAvatarSaveError:Could not update your profile picture. Please try again.`);
        },
      });
  }
}
