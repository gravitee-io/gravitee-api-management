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

type JsonNode = Record<string, unknown> & { schemaFormat?: unknown; schema?: unknown; payload?: unknown };

/**
 * The AsyncAPI viewer (`@asyncapi/web-component`) has no schema parser for XML Schema: up to 2.x
 * it refuses the whole document ("Unknown schema format"), from 3.x it shows the payload as `any`.
 * Each XML Schema payload becomes a string schema quoting the XSD, so the rest of the document
 * renders and the XSD stays readable. Any other content is returned unchanged — content that
 * cannot be read included, so the viewer still reports its own errors.
 */
export function toRenderableAsyncApi(content: string): string {
  let document: unknown;
  try {
    document = readYaml(content);
  } catch {
    return content;
  }
  if (!isNode(document) || !('asyncapi' in document)) {
    return content;
  }
  return replaceXmlSchemaPayloads(document) ? JSON.stringify(document) : content;
}

function replaceXmlSchemaPayloads(node: JsonNode): boolean {
  let replaced = false;
  for (const [key, value] of Object.entries(node)) {
    if (!isNode(value)) {
      continue;
    }
    // AsyncAPI 3.x: a Multi Format Schema Object holds both the format and the schema.
    if (isXmlSchemaFormat(value.schemaFormat) && 'schema' in value) {
      node[key] = quoteXmlSchema(value.schemaFormat, value.schema);
      replaced = true;
    } else {
      replaced = replaceXmlSchemaPayloads(value) || replaced;
    }
  }
  // AsyncAPI 2.x: the message declares the format of its payload.
  if (isXmlSchemaFormat(node.schemaFormat) && 'payload' in node) {
    node.payload = quoteXmlSchema(node.schemaFormat, node.payload);
    delete node.schemaFormat;
    replaced = true;
  }
  return replaced;
}

function isNode(value: unknown): value is JsonNode {
  return typeof value === 'object' && value !== null;
}

function isXmlSchemaFormat(schemaFormat: unknown): schemaFormat is string {
  return typeof schemaFormat === 'string' && /\bx(ml|sd)\b/.test(schemaFormat.split(';')[0].toLowerCase());
}

function quoteXmlSchema(schemaFormat: string, schema: unknown): JsonNode {
  const source = typeof schema === 'string' ? schema.trim() : JSON.stringify(schema, null, 2);
  const longestBacktickRun = Math.max(0, ...(source.match(/`+/g) ?? []).map(run => run.length));
  const fence = '`'.repeat(Math.max(3, longestBacktickRun + 1));
  return {
    type: 'string',
    description: `This payload is described by an XML Schema (\`${schemaFormat}\`), which this viewer cannot render. The schema as written:\n\n${fence}xml\n${source}\n${fence}`,
  };
}
