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
import { Component, DestroyRef, effect, EventEmitter, inject, input, OnInit, Output } from '@angular/core';
import { GioFormFilePickerModule, NewFile } from '@gravitee/ui-particles-angular';
import { CommonModule } from '@angular/common';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

@Component({
  selector: 'api-import-file-picker',
  imports: [CommonModule, FormsModule, GioFormFilePickerModule, ReactiveFormsModule],
  templateUrl: './api-import-file-picker.component.html',
  styleUrls: ['./api-import-file-picker.component.scss'],
})
export class ApiImportFilePickerComponent implements OnInit {
  private snackBarService = inject(SnackBarService);
  private destroyRef = inject(DestroyRef);
  allowedFileExtensions = input<string[]>(['yml', 'yaml', 'json', 'wsdl', 'xml', 'js']);
  filePickerControl = new FormControl();
  importType: string;
  accept: string;
  @Output() onFilePicked = new EventEmitter<{ importFile: File; importFileContent: string; importType: string }>();

  constructor() {
    effect(() => {
      this.accept = this.allowedFileExtensions()
        .map(ext => '.' + ext)
        .join(',');
    });
  }

  ngOnInit() {
    this.filePickerControl.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(value => this.onImportFile(value));
  }

  protected async onImportFile(event: (NewFile | string)[] | undefined) {
    if (!event || event.length !== 1) {
      this.resetImportFile();
      return;
    }
    const file = event[0];
    if (!(file instanceof NewFile)) {
      return;
    }

    const extension = file.name.split('.').pop().toLowerCase();
    if (!this.allowedFileExtensions().includes(extension)) {
      this.resetImportFile('Invalid file format. Supported file formats: ' + this.allowedFileExtensions().join(', '));
      return;
    }
    const fileContent = await this.getFileContent(file.file);

    // Find import type with extension and file content for json format
    switch (extension) {
      case 'wsdl':
      case 'xml':
        this.importType = 'WSDL';
        break;

      case 'json':
        try {
          const json: any = JSON.parse(fileContent);
          this.importType = this.determineImportType(json);
        } catch (error) {
          this.resetImportFile('Invalid JSON file.');
        }
        break;

      case 'yml':
      case 'yaml':
        this.importType = 'SWAGGER';
        break;

      case 'js':
        this.importType = 'JavaScript';
        break;

      default:
        this.resetImportFile('Invalid file format.');
        return;
    }

    if (this.importType) {
      this.onFilePicked.emit({ importFile: file.file, importFileContent: fileContent, importType: this.importType });
    }
  }

  private getFileContent(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => {
        resolve(reader.result as string);
      };
      reader.onerror = reject;
      reader.readAsText(file);
    });
  }

  private resetImportFile(message?: string) {
    if (message) {
      this.snackBarService.error(message);
    }
    this.onFilePicked.emit({ importType: undefined, importFile: undefined, importFileContent: undefined });
  }

  private isSwaggerJsonContent(fileContent: unknown): boolean {
    return (
      fileContent instanceof Object &&
      (Object.prototype.hasOwnProperty.call(fileContent, 'swagger') ||
        Object.prototype.hasOwnProperty.call(fileContent, 'swaggerVersion') ||
        Object.prototype.hasOwnProperty.call(fileContent, 'openapi'))
    );
  }

  private determineImportType(jsonNode: any) {
    // Check if it's a swagger. if not consider is gravitee api definition
    if (this.isSwaggerJsonContent(jsonNode)) {
      return 'SWAGGER';
    }
    if (jsonNode.api?.definitionVersion === 'V4') {
      return 'MAPI_V2';
    }
    return 'GRAVITEE';
  }
}
