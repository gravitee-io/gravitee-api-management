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
import { Component, computed, input, linkedSignal } from '@angular/core';

export interface UserCellVM {
  displayName: string;
  email?: string;
  avatarUrl?: string;
  initials: string;
}

@Component({
  selector: 'app-user-cell',
  standalone: true,
  templateUrl: './user-cell.component.html',
  styleUrl: './user-cell.component.scss',
})
export class UserCellComponent {
  user = input.required<UserCellVM>();
  isCurrentUser = input(false);

  /** Resets automatically whenever the avatar URL changes (linkedSignal re-initializes from source). */
  protected avatarLoadFailed = linkedSignal<string | undefined, boolean>({
    source: () => this.user().avatarUrl,
    computation: () => false,
  });

  protected showAvatar = computed(() => !!this.user().avatarUrl && !this.avatarLoadFailed());

  protected onAvatarError(): void {
    this.avatarLoadFailed.set(true);
  }
}
