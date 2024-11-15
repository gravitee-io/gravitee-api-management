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

import { Component } from '@angular/core';

const yamlFileExample = `
ratelimit-headers:
    description: Response must include ratelimit-x headers
    message: '{{description}}; missing {{property}}'
    severity: error
    given: $..responses.*
    then:
      - field: headers.ratelimit-limit
        function: truthy
      - field: headers.ratelimit-remaining
        function: truthy
      - field: headers.ratelimit-reset
        function: truthy

  properties-must-include-examples:
    description: Object properties must include examples
    given: $..properties..properties.*
    severity: error
    message: '{{description}}; {{property}}'
    then:
      function: ensurePropertiesExample
`;

@Component({
  selector: 'app-api-score-rulesets',
  templateUrl: './api-score-rulesets.component.html',
  styleUrl: './api-score-rulesets.component.scss',
})
export class ApiScoreRulesetsComponent {
  public readonly yamlFileExample = yamlFileExample;
}
