/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { hexToHSL } from './hex-to-hsl';

describe('hex to HSL convertor', () => {
  it('should parse valid hex code', () => {
    expect(hexToHSL('#6c59bd')).toEqual({ h: 251, s: 43, l: 55 });
  });
  it('should parse invalid hex code + valid default hex code', () => {
    expect(hexToHSL('#abc21', '#6c59bd')).toEqual({ h: 251, s: 43, l: 55 });
  });
  it('should parse default hex code if hex code is empty', () => {
    expect(hexToHSL('', '#6c59bd')).toEqual({ h: 251, s: 43, l: 55 });
  });
  it('should throw error if invalid hex code + no default hex code', () => {
    expect(() => {
      hexToHSL('#abc21');
    }).toThrow('Could not parse Hex Color: #abc21');
  });
  it('should throw error if invalid hex code + invalid default hex code', () => {
    expect(() => {
      hexToHSL('#abc21', '#ed154eff');
    }).toThrow('Could not parse Hex Color: #ed154eff');
  });
});
