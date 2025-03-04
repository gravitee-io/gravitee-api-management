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
import { Pipe, PipeTransform, SecurityContext } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';

import { Api } from '../../../projects/portal-webclient-sdk/src/lib';
import { MarkdownService } from '../services/markdown.service';
import { ConfigurationService } from '../services/configuration.service';

@Pipe({
  name: 'markdownDescription',
})
export class MarkdownDescriptionPipe implements PipeTransform {
  constructor(
    private configService: ConfigurationService,
    private markdown: MarkdownService,
    private sanitizer: DomSanitizer,
  ) {}

  transform(api: Api): Api {
    return api?.description
      ? {
          ...api,
          description: this.renderDescription(api.description),
        }
      : api;
  }

  private renderDescription(description: string) {
    const baseURL = this.configService.get('baseURL');
    const markdown = this.markdown.render(description, baseURL, undefined, []);
    const safeHtml = this.sanitizer.bypassSecurityTrustHtml(markdown);
    const rawHtml = this.sanitizer.sanitize(SecurityContext.HTML, safeHtml);
    return this.replaceAnchors(rawHtml);
  }

  /**
   * Resolving links would require fetching pages for the API in order to resolve relative links between pages.
   *
   * Because the transform method is used once per API for all APIs in {@link FilteredCatalogComponent#_loadCards},
   * anchor tags get replaced by another one in order to avoid displaying dead links or a getting into trouble
   * regarding performances.
   *
   * @param html
   * @private
   */
  private replaceAnchors(html: string): string {
    const openingAnchor = /<a[^>]*>/g;
    const closingAnchor = /<\/a>/g;
    return html.replace(openingAnchor, '<b>').replace(closingAnchor, '</b>');
  }
}
