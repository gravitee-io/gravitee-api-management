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
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-gv-file-upload',
  templateUrl: './gv-file-upload.component.html'
})
export class GvFileUploadComponent implements OnInit {

  @Input() label: string;
  @Output()
  fileLoad: EventEmitter<File> = new EventEmitter<File>();

  constructor(
    private notificationService: NotificationService,
  ) {}

  ngOnInit() {
  }

  onFileSelected(event) {
    this.notificationService.reset();
    const reader = new FileReader();
    const file = event.target.files[0];
    if (file) {
      if (file.size > 500_000) {
        this.notificationService.warning(i18n('errors.picture_too_big'));
      } else if (file.type.startsWith('image/svg')) {
        this.notificationService.warning(i18n('errors.picture_format_forbidden'));
      } else {
        reader.readAsDataURL(file);
        reader.onload = (loadEvent: any) => {
          this.fileLoad.emit(loadEvent.target.result);
        };
      }
    }
  }
}
