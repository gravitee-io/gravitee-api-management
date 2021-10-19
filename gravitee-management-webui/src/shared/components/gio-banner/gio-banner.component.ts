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
import { Component, Input } from '@angular/core';

@Component({
  selector: 'gio-banner',
  template: require('./gio-banner.component.html'),
  styles: [require('./gio-banner.component.scss')],
})
export class GioBannerComponent {
  @Input()
  type: 'error' | 'info' | 'success' | 'warning' = 'info';
}

@Component({
  selector: 'gio-banner-error',
  template: require('./gio-banner.component.html'),
  styles: [require('./gio-banner.component.scss')],
})
export class GioBannerErrorComponent extends GioBannerComponent {
  type = 'error' as const;
}

@Component({
  selector: 'gio-banner-info',
  template: require('./gio-banner.component.html'),
  styles: [require('./gio-banner.component.scss')],
})
export class GioBannerInfoComponent extends GioBannerComponent {
  type = 'info' as const;
}

@Component({
  selector: 'gio-banner-success',
  template: require('./gio-banner.component.html'),
  styles: [require('./gio-banner.component.scss')],
})
export class GioBannerSuccessComponent extends GioBannerComponent {
  type = 'success' as const;
}

@Component({
  selector: 'gio-banner-warning',
  template: require('./gio-banner.component.html'),
  styles: [require('./gio-banner.component.scss')],
})
export class GioBannerWarningComponent extends GioBannerComponent {
  type = 'warning' as const;
}
