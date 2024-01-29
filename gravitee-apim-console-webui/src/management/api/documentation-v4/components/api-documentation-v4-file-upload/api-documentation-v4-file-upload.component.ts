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
import { Component, forwardRef, Input } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { NewFile } from '@gravitee/ui-particles-angular';

import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';

const allowedFileExtensions = ['md', 'markdown', 'txt'] as const;
type FileExtension = (typeof allowedFileExtensions)[number];

@Component({
  selector: 'api-documentation-file-upload',
  templateUrl: './api-documentation-v4-file-upload.component.html',
  styleUrls: ['./api-documentation-v4-file-upload.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ApiDocumentationV4FileUploadComponent),
      multi: true,
    },
  ],
})
export class ApiDocumentationV4FileUploadComponent implements ControlValueAccessor {
  @Input()
  published = false;

  public _onChange: (_selection: string) => void = () => ({});

  protected _onTouched: () => void = () => ({});
  filePickerValue = [];
  importFile: File;
  importFileContent: string;

  constructor(private readonly snackBarService: SnackBarService) {}

  async onImportFile(event: (NewFile | string)[] | undefined) {
    if (!event || event.length !== 1) {
      this.resetImportFile();
      return;
    }
    const file = event[0];

    if (!(file instanceof NewFile)) {
      return;
    }

    const extension = file.name.split('.').pop().toLowerCase() as FileExtension;
    if (!allowedFileExtensions.includes(extension)) {
      this.resetImportFile('Invalid file format. Supported file formats: ' + allowedFileExtensions.join(', '));
      return;
    }
    const fileContent = await getFileContent(file.file);
    this.importFile = file.file;
    this.importFileContent = fileContent;
    this._onChange(fileContent);
  }
  private resetImportFile(message?: string) {
    if (message) {
      this.snackBarService.error(message);
    }
    this.importFile = undefined;
    this.importFileContent = undefined;
    this._onChange(undefined);
  }

  // From ControlValueAccessor interface
  public registerOnChange(fn: (value: string) => void): void {
    this._onChange = fn;
  }

  // From ControlValueAccessor interface
  public registerOnTouched(fn: () => void): void {
    this._onTouched = fn;
  }

  writeValue(content: string): void {
    this.importFileContent = content;
  }

  protected readonly allowedFileExtensions = allowedFileExtensions;
}

const getFileContent = (file: File): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      resolve(reader.result as string);
    };
    reader.onerror = reject;
    reader.readAsText(file);
  });
};
