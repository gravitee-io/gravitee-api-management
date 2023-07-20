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

import { Component, ElementRef, Injector, Input, SimpleChange } from '@angular/core';
import { UpgradeComponent } from '@angular/upgrade/static';

@Component({
  template: '',
  selector: 'documentation-edit-page',
  host: {
    class: 'bootstrap',
  },
})
export class DocumentationEditPageComponent extends UpgradeComponent {
  @Input() resolvedPage;
  @Input() resolvedGroups;
  @Input() resolvedFetchers;
  @Input() pagesToLink;
  @Input() folders;
  @Input() systemFolders;
  @Input() pageResources;
  @Input() categoryResources;
  @Input() attachedResources;

  constructor(elementRef: ElementRef, injector: Injector) {
    super('documentationEditPageAjs', elementRef, injector);
  }

  ngOnInit() {
    // Hack to Force the binding between Angular and AngularJS
    // Don't know why, but the binding is not done automatically when resolver is used
    this.ngOnChanges({
      resolvedPage: new SimpleChange(null, this.resolvedPage, true),
      resolvedGroups: new SimpleChange(null, this.resolvedGroups, true),
      resolvedFetchers: new SimpleChange(null, this.resolvedFetchers, true),
      pagesToLink: new SimpleChange(null, this.pagesToLink, true),
      folders: new SimpleChange(null, this.folders, true),
      systemFolders: new SimpleChange(null, this.systemFolders, true),
      pageResources: new SimpleChange(null, this.pageResources, true),
      categoryResources: new SimpleChange(null, this.categoryResources, true),
      attachedResources: new SimpleChange(null, this.attachedResources, true),
    });

    super.ngOnInit();
  }
}
