/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.apim.core.analytics.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.With;

<<<<<<< HEAD:gravitee-apim-rest-api/gravitee-apim-rest-api-service/src/main/java/io/gravitee/apim/core/analytics/model/EnvironmentAnalyticsQueryParameters.java
@Data
@Builder
public class EnvironmentAnalyticsQueryParameters {

    @With
    List<String> apiIds;

    long from;
    long to;
=======
import { ReplaceSpacesPipe } from '../../pipes/replace-spaces.pipe';

@Component({
  selector: 'file-preview',
  standalone: true,
  imports: [GioClipboardModule, ReplaceSpacesPipe],
  templateUrl: './file-preview.component.html',
  styleUrl: './file-preview.component.scss',
})
export class FilePreviewComponent {
  @Input() payload: string;
>>>>>>> c7cb597a7c (fix: code preview component fix):gravitee-apim-console-webui/src/shared/components/file-preview/file-preview.component.ts
}
