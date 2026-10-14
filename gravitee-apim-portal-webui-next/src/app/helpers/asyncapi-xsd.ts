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

type JsonRecord = Record<string, unknown>;

// application/xml, text/xml, application/xsd, application/xsd+xml, ... — parameters such as `;version=1.0` are ignored.
const XML_SCHEMA_FORMAT = /^(?:application|text)\/(?:[\w.-]+\+)?(?:xml|xsd)$/i;

function isRecord(value: unknown): value is JsonRecord {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function isXmlSchemaFormat(schemaFormat: unknown): boolean {
  return typeof schemaFormat === 'string' && XML_SCHEMA_FORMAT.test(schemaFormat.split(';')[0].trim());
}

function xsdAsStringSchema(source: string): JsonRecord {
  const xsd = source.trimEnd();
  const longestBacktickRun = Math.max(0, ...(xsd.match(/`+/g) ?? []).map(run => run.length));
  const fence = '`'.repeat(Math.max(3, longestBacktickRun + 1));
  return { type: 'string', description: `XML Schema (XSD):\n\n${fence}xml\n${xsd}\n${fence}` };
}

function rewrite(node: unknown): unknown {
  if (Array.isArray(node)) {
    return node.map(rewrite);
  }
  if (!isRecord(node)) {
    return node;
  }

  const rewritten: JsonRecord = {};
  Object.entries(node).forEach(([key, value]) => (rewritten[key] = rewrite(value)));

  if (isXmlSchemaFormat(node['schemaFormat'])) {
    // AsyncAPI 3: a multi format schema object { schemaFormat, schema }, replaced as a whole.
    if (typeof node['schema'] === 'string') {
      return xsdAsStringSchema(node['schema']);
    }
    // AsyncAPI 2: the message declares the format and its payload is the raw schema.
    if (typeof node['payload'] === 'string') {
      delete rewritten['schemaFormat'];
      rewritten['payload'] = xsdAsStringSchema(node['payload']);
    }
  }
  return rewritten;
}

/**
 * The AsyncAPI viewer has no parser for XML Schema: it either drops the payload or fails the whole page.
 * Replaces every XSD payload by a string schema quoting the XSD in a code block, so the rest of the document
 * renders and the reader still sees the schema. Content without an XSD payload, and content that is not
 * a parsable document (left to the viewer to report), is returned untouched.
 */
export function inlineXsdPayloads(content: string): string {
  if (!content || !/xml|xsd/i.test(content)) {
    return content;
  }
  try {
    const document = readYaml(content);
    const rewritten = rewrite(document);
    return JSON.stringify(rewritten) === JSON.stringify(document) ? content : JSON.stringify(rewritten);
  } catch {
    return content;
  }
}
