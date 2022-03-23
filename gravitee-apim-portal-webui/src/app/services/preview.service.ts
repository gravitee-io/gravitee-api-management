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
import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

import { NotificationService } from './notification.service';

@Injectable({
  providedIn: 'root',
})
export class PreviewService {
  private readonly previewSource: BehaviorSubject<boolean>;

  constructor(private notificationService: NotificationService) {
    this.previewSource = new BehaviorSubject<boolean>(false);
  }

  isActive(): boolean {
    const state = this.previewSource.getValue();
    if (state === true) {
      this.notificationService.info('On preview mode');
    }
    return state;
  }

  activate() {
    this.previewSource.next(true);
  }

  deactivate() {
    this.previewSource.next(false);
  }
}
