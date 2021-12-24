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

import { readYaml } from './yaml-parser';

describe('yamlToJson', () => {
  it('should not transform date format', () => {
    const given = `
      birthdate:
          type: string
          format: date
          example: 1980-12-12
      active:
          type: boolean
          example: TRUE
      email:
          type: string
          format: email
          example: john@doe.com
      home_phone:
          type: integer
          example: 0606060606
      mobile_phone:
          type: string
          example: +33606060606
      budget_balance:
          type: integer
          example: -300
      error_rate:
          type: number
          example: 0.8
      forecast_error_rate:
          type: number
          example: -0.1
      rank:
          type: number
          example: 05
    `;

    const expected = JSON.stringify({
      birthdate: { type: 'string', format: 'date', example: '1980-12-12' },
      active: { type: 'boolean', example: 'TRUE' },
      email: { type: 'string', format: 'email', example: 'john@doe.com' },
      home_phone: { type: 'integer', example: '0606060606' },
      mobile_phone: { type: 'string', example: '+33606060606' },
      budget_balance: { type: 'integer', example: '-300' },
      error_rate: { type: 'number', example: '0.8' },
      forecast_error_rate: { type: 'number', example: '-0.1' },
      rank: { type: 'number', example: '05' },
    });

    const having = JSON.stringify(readYaml(given));

    expect(having).toBe(expected);
  });
});
