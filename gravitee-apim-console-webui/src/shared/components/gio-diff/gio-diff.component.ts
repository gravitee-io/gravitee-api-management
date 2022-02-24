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
import * as Diff2Html from 'diff2html';
import * as Diff from 'diff';
import { Component, Input, OnChanges, SimpleChange, SimpleChanges, ViewEncapsulation } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import '@gravitee/ui-components/wc/gv-code';

@Component({
  selector: 'gio-diff',
  template: require('./gio-diff.component.html'),
  styles: [require('./gio-diff.component.scss')],
  encapsulation: ViewEncapsulation.None,
})
export class GioDiffComponent implements OnChanges {
  @Input()
  left: string;

  @Input()
  right: string;

  diffHTML: SafeHtml;

  outputFormat: 'row' | 'side-by-side' | 'line-by-line' = 'side-by-side';
  hasChanges = true;

  gvCodeOptions = {
    lineWrapping: true,
    lineNumbers: true,
  };

  constructor(private readonly sanitizer: DomSanitizer) {}

  ngOnChanges(changes: SimpleChanges) {
    if (this.propHasChanged(changes.left) || this.propHasChanged(changes.right)) {
      this.computeDiff();
    }
  }

  private computeDiff() {
    const diff = Diff.createPatch(' ', this.left, this.right);

    this.hasChanges = !!Diff.parsePatch(diff).some((d) => d.hunks.length);
    if (!this.hasChanges) {
      this.outputFormat = 'row';
    }

    if (this.outputFormat !== 'row') {
      const diff2Html = Diff2Html.html(diff as any, {
        drawFileList: false,
        matching: 'lines',
        outputFormat: this.outputFormat,
        renderNothingWhenEmpty: true,
      });
      this.diffHTML = this.sanitizer.bypassSecurityTrustHtml(diff2Html);
      return;
    }
  }

  private propHasChanged(change: SimpleChange) {
    return change && change.currentValue !== change.previousValue;
  }
}
