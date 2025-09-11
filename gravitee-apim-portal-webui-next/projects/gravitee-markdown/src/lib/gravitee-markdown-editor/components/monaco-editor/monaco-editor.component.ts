/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { ChangeDetectorRef, Component, effect, ElementRef, input, NgZone, OnDestroy, output, signal } from '@angular/core';
import { isEqual, uniqueId } from 'lodash';
import * as Monaco from 'monaco-editor';
import { editor } from 'monaco-editor';

import { MonacoEditorService } from '../../services/monaco-editor.service';

@Component({
  selector: 'gmd-monaco-editor',
  templateUrl: './monaco-editor.component.html',
  styleUrl: './monaco-editor.component.scss',
  // eslint-disable-next-line @angular-eslint/prefer-standalone
  standalone: false,
})
export class MonacoEditorComponent implements OnDestroy {
  value = input<string>('');
  readOnly = input<boolean>(false);
  valueChange = output<string>();
  touched = output<void>();

  public standaloneCodeEditor?: editor.IStandaloneCodeEditor;

  private readonly isEditorSetup = signal<boolean>(false);
  private textModel?: editor.ITextModel;
  private toDisposes: Monaco.IDisposable[] = [];

  private readonly defaultOptions: editor.IStandaloneEditorConstructionOptions = {
    contextmenu: false,
    minimap: {
      enabled: false,
    },
    automaticLayout: true,
    scrollBeyondLastLine: false,
    theme: 'vs',
    renderLineHighlight: 'none',
    scrollbar: {
      verticalScrollbarSize: 5,
      horizontalScrollbarSize: 5,
    },
  };

  constructor(
    private readonly hostElement: ElementRef,
    private readonly monacoEditorService: MonacoEditorService,
    private readonly changeDetectorRef: ChangeDetectorRef,
    private readonly ngZone: NgZone,
  ) {
    this.monacoEditorService.loadEditor();

    // Effect to handle editor setup and value updates
    effect(() => {
      const newValue = this.value();
      if (this.standaloneCodeEditor && this.textModel) {
        const currentValue = this.textModel.getValue();
        if (currentValue !== newValue) {
          this.textModel.setValue(newValue);
        }
        this.autoFormatValue();
      }
    });

    // Effect to handle Monaco editor initialization
    effect(() => {
      const monaco = this.monacoEditorService.monaco();
      if (monaco && !this.isEditorSetup()) {
        this.setupEditor(monaco);
        this.isEditorSetup.set(true);
        this.changeDetectorRef.detectChanges();
      }
    });

    // Effect to handle readonly state
    effect(() => {
      const isReadOnly = this.readOnly();
      if (this.standaloneCodeEditor) {
        this.standaloneCodeEditor.updateOptions({
          readOnly: isReadOnly,
        });
      }
    });
  }

  public ngOnDestroy() {
    this.toDisposes.forEach(d => d.dispose());

    if (this.standaloneCodeEditor) {
      this.standaloneCodeEditor.dispose();
      this.standaloneCodeEditor = undefined;
    }

    if (this.textModel) {
      this.textModel.dispose();
      this.textModel = undefined;
    }
  }

  private setupEditor(monaco: typeof Monaco) {
    if (!this.hostElement) {
      throw new Error('No editor ref found.');
    }

    const domElement = this.hostElement.nativeElement;

    this.ngZone.runOutsideAngular(() => {
      this.textModel = monaco.editor.createModel(this.value(), 'markdown', monaco.Uri.parse(`code-${uniqueId()}`));
    });

    this.standaloneCodeEditor = monaco.editor.create(domElement, {
      ...this.defaultOptions,
      model: this.textModel,
      readOnly: this.readOnly(),
    });

    const onDidChangeContent = this.textModel?.onDidChangeContent(() => {
      const textModelValue = this.textModel?.getValue() ?? '';

      this.ngZone.run(() => {
        if (!isEqual(this.value(), textModelValue)) {
          setTimeout(() => {
            this.valueChange.emit(textModelValue);
          }, 0);
        }
      });
    });

    const onDidBlurEditorWidget = this.standaloneCodeEditor?.onDidBlurEditorWidget(() => {
      this.ngZone.run(() => {
        this.touched.emit();
      });
    });

    this.toDisposes = [onDidChangeContent, onDidBlurEditorWidget].filter(d => !!d) as Monaco.IDisposable[];
  }

  private autoFormatValue() {
    setTimeout(() => {
      this.standaloneCodeEditor
        ?.getAction('editor.action.formatDocument')
        ?.run()
        .finally(() => {
          this.changeDetectorRef.detectChanges();
        });
    }, 80);
  }
}
