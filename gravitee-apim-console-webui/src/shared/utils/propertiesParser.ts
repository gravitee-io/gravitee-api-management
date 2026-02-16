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
const LINE_BREAK = '\n';
const SINGLE_QUOTE = "'";
const DOUBLE_QUOTE = '"';
const EQUAL = '=';
const COMMENT = '#';

/**
 * Parse properties string format
 * Input format example: key1=value1\nkey2=value2\nkey3=value3
 * - key and value are separated by '='
 * - each property is separated by '\n'
 * - key can't be empty
 * - if line starts with '#' it's a comment and is ignored
 * - value can be quoted with ' or "
 */
export const parsePropertiesStringFormat = (input: string): { properties: { key: string; value: string }[]; errors: string[] } => {
  const defaultResult = {
    properties: [],
    errors: [],
  };
  if (!input) {
    return defaultResult;
  }

  const lines = splitToLine(input.trim()).filter(line => line.trim());

  const properties: { key: string; value: string }[] = [];
  const errors = [];
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    if (line.startsWith(COMMENT)) {
      continue;
    }

    const index = line.indexOf(EQUAL);
    if (index === -1) {
      errors.push(`Line ${i + 1} is not valid. It must contain '='`);
      continue;
    }
    const key = line.substring(0, index).trim();
    if (!key) {
      errors.push(`Line ${i + 1} is not valid. Key can't be empty`);
      continue;
    }
    if (properties.find(v => v.key === key)) {
      errors.push(`Line ${i + 1} is not valid. Key '${key}' is duplicated`);
      continue;
    }

    const value = line.substring(index + 1).trim();
    const unquotedValue = unquote(value);
    const sanitizedValue = removeUnnecessaryEscape(unquotedValue);

    properties.push({ key: key, value: sanitizedValue });
  }

  return {
    properties,
    errors,
  };
};

/**
 * Split input string into lines by \n but ignore \n inside quotes or double quotes
 *
 * @param input
 */
function splitToLine(input: string): string[] {
  const lines = [];
  let partialLine = '';
  let insideSingleQuote = false;
  let insideDoubleQuote = false;

  for (let i = 0; i < input.length; i++) {
    const char = input[i];
    const previousChar = i > 0 ? input[i - 1] : null;
    const nextChar = i < input.length - 1 ? input[i + 1] : null;

    if (
      (previousChar && `${previousChar}${char}` === `${EQUAL}${SINGLE_QUOTE}`) ||
      (nextChar && `${char}${nextChar}` === `${SINGLE_QUOTE}${LINE_BREAK}`)
    ) {
      insideSingleQuote = !insideSingleQuote;
    }
    if (
      (previousChar && `${previousChar}${char}` === `${EQUAL}${DOUBLE_QUOTE}`) ||
      (nextChar && `${char}${nextChar}` === `${DOUBLE_QUOTE}${LINE_BREAK}`)
    ) {
      insideDoubleQuote = !insideDoubleQuote;
    }
    if (char === LINE_BREAK && !insideSingleQuote && !insideDoubleQuote) {
      lines.push(partialLine);
      partialLine = '';
      continue;
    }
    partialLine += char;
  }
  if (partialLine) {
    lines.push(partialLine);
  }

  return lines;
}

function unquote(_value: string) {
  const value = _value.trim();

  if (value.startsWith(SINGLE_QUOTE) && value.endsWith(SINGLE_QUOTE)) {
    return value.substring(1, value.length - 1);
  }
  if (value.startsWith(DOUBLE_QUOTE) && value.endsWith(DOUBLE_QUOTE)) {
    return value.substring(1, value.length - 1);
  }

  return value;
}

function removeUnnecessaryEscape(value: string) {
  const KEEP_ESCAPE_LINE_BREAK_KEY = 'GIO_KEEP_ESCAPE_LINE_BREAK';
  return value
    .replace(/\\n/g, KEEP_ESCAPE_LINE_BREAK_KEY)
    .replace(/\\(.)/g, '$1')
    .replace(new RegExp(`${KEEP_ESCAPE_LINE_BREAK_KEY}`, 'g'), '\\n');
}
