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
import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeHtml, SafeStyle, SafeScript, SafeUrl, SafeResourceUrl } from '@angular/platform-browser';

@Pipe({
  name: 'safe',
})
export class SafePipe implements PipeTransform {
  constructor(protected sanitizer: DomSanitizer) {}

  public transform(value: any, type: string): SafeHtml | SafeStyle | SafeScript | SafeUrl | SafeResourceUrl {
    switch (type) {
      // No Sonar because the bypass is deliberate and should only be used with safe data
      case 'html':
        return this.sanitizer.bypassSecurityTrustHtml(value); // NOSONAR
      case 'style':
        return this.sanitizer.bypassSecurityTrustStyle(value); // NOSONAR
      case 'script':
        return this.sanitizer.bypassSecurityTrustScript(value); // NOSONAR
      case 'url':
        return this.sanitizer.bypassSecurityTrustUrl(value); // NOSONAR
      case 'resourceUrl':
        return this.sanitizer.bypassSecurityTrustResourceUrl(value); // NOSONAR
      default:
        throw new Error(`Invalid safe type specified: ${type}`);
    }
  }
}
