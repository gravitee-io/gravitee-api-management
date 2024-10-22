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

import * as jsYAML from 'js-yaml';

import { readYaml } from './yaml-parser';

const binaryType = new jsYAML.Type('tag:yaml.org,2002:binary', {
  kind: 'scalar',
  resolve: (data: any) => {
    return typeof data === 'string' && /^[A-Za-z0-9+/=]*$/.test(data);
  },
  construct: (data: string) => {
    return Buffer.from(data, 'base64').toString('utf-8');
  },
  instanceOf: String,
  represent: (value: any) => {
    return Buffer.from(String(value), 'utf-8').toString('base64');
  },
});

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
          example: "0606060606"
      mobile_phone:
          type: string
          example: "+33606060606"
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
          example: "05"
    `;

    const expected = JSON.stringify({
      birthdate: { type: 'string', format: 'date', example: '1980-12-12' },
      active: { type: 'boolean', example: true },
      email: { type: 'string', format: 'email', example: 'john@doe.com' },
      home_phone: { type: 'integer', example: '0606060606' },
      mobile_phone: { type: 'string', example: '+33606060606' },
      budget_balance: { type: 'integer', example: -300 },
      error_rate: { type: 'number', example: 0.8 },
      forecast_error_rate: { type: 'number', example: -0.1 },
      rank: { type: 'number', example: '05' },
    });

    const having = JSON.stringify(readYaml(given));

    expect(having).toBe(expected);
  });

  it('should decode Base64 binary data correctly', () => {
    const given = `
      file:
        !!binary "U3dhZ2dlciByb2Nrcw=="
    `;

    const expected = JSON.stringify({
      file: 'Swagger rocks', // Decoded value from Base64
    });

    const having = JSON.stringify(readYaml(given));

    expect(having).toBe(expected);
  });

  // Test case for resolve method
  it('should correctly resolve Base64 validity', () => {
    expect(binaryType.resolve('U3dhZ2dlciByb2Nrcw==')).toBe(true); // Valid Base64
    expect(binaryType.resolve('InvalidBase64%')).toBe(false); // Invalid Base64
    expect(binaryType.resolve(null)).toBe(false); // Non-string
    expect(binaryType.resolve(123)).toBe(false); // Non-string
  });

  // Test case for construct method
  it('should construct a valid UTF-8 string from Base64', () => {
    const base64String = 'U3dhZ2dlciByb2Nrcw=='; // Base64 for "Swagger rocks"
    const expectedString = 'Swagger rocks';

    const constructedValue = binaryType.construct(base64String);
    expect(constructedValue).toBe(expectedString);
  });
});
