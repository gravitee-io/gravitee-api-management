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
import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { GioFormFilePickerModule, NewFile } from '@gravitee/ui-particles-angular';
import { MatIcon } from '@angular/material/icon';
import { MarkdownComponent } from 'ngx-markdown';

import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { PageType } from '../../../../../entities/management-api-v2';
import { GioSwaggerUiModule } from '../../../../../components/documentation/gio-swagger-ui/gio-swagger-ui.module';
import { GioAsyncApiModule } from '../../../../../components/documentation/gio-async-api/gio-async-api-module';

const allowedFileExtensions = ['md', 'markdown', 'txt', 'json', 'yml', 'yaml'] as const;
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
  standalone: true,
  imports: [GioFormFilePickerModule, MatIcon, FormsModule, MarkdownComponent, GioSwaggerUiModule, GioAsyncApiModule],
})
export class ApiDocumentationV4FileUploadComponent implements OnInit, ControlValueAccessor {
  @Input()
  published = false;

  @Input()
  pageType: PageType;

  public _onChange: (_selection: string) => void = () => ({});

  protected _onTouched: () => void = () => ({});
  filePickerValue = [];
  importFile: File;
  importFileContent: string;
  acceptedFileExtensions: FileExtension[];
  accept: string;

  constructor(private readonly snackBarService: SnackBarService) {}

  ngOnInit(): void {
    this.acceptedFileExtensions = this.pageType === 'MARKDOWN' ? ['md', 'markdown', 'txt'] : ['json', 'yml', 'yaml'];
    // To work in file picker, accept must have . before file type
    this.accept = this.acceptedFileExtensions.map((e) => '.' + e).join();
  }

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
    if (!this.acceptedFileExtensions.includes(extension)) {
      this.resetImportFile('Invalid file format. Supported file formats: ' + this.acceptedFileExtensions.join(', '));
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
