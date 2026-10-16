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
import { CORE_SCHEMA, loadAll, mergeTag } from 'js-yaml';

type JsonNode = Record<string, unknown> & { asyncapi?: unknown; schemaFormat?: unknown; schema?: unknown; payload?: unknown };

// application/xml, text/xml, application/xsd, application/xsd+xml, ... — parameters such as `;version=1.0` are ignored.
const XML_SCHEMA_FORMAT = /^(?:application|text)\/(?:[\w.-]+\+)?(?:xml|xsd)$/i;

// A rewritten document is handed to the viewer as JSON, where an unresolved `<<` key would be a
// plain property.
const ASYNCAPI_YAML_SCHEMA = CORE_SCHEMA.withTags(mergeTag);

// JSON.stringify writes a value shared through YAML aliases once per reference: a document that would expand past
// this size (one per node, plus one per 64 characters of text or key), or that loops back, would stall the page once
// rewritten, so it is left to the viewer. It also bounds the walks below, aliases included.
const MAX_EXPANDED_SIZE = 100_000;

/**
 * The AsyncAPI viewer (`@asyncapi/web-component`) has no schema parser for XML Schema: up to 2.x
 * it refuses the whole document ("Unknown schema format"), from 3.x it shows the payload as `any`.
 * Each XML Schema payload becomes a string schema quoting the XSD, so the rest of the document
 * renders and the XSD stays readable. Any other content is returned unchanged — content that
 * cannot be read included, so the viewer still reports its own errors.
 */
export function toRenderableAsyncApi<T extends string | null | undefined>(content: T): T | string {
  // Most documents have no XML Schema at all: skip parsing them.
  if (!content || !/xml|xsd/i.test(content)) {
    return content;
  }
  try {
    const document = readAsyncApiDocument(content);
    if (!document || expandedSize(document, new Map()) > MAX_EXPANDED_SIZE) {
      return content;
    }
    // AsyncAPI 2.x declares the payload format on the message; 3.x moved it into the payload, a
    // Multi Format Schema Object, and rejects it on the message.
    const replaced = String(document.asyncapi).startsWith('2.') ? replaceMessagePayloads(document) : replaceMultiFormatSchemas(document);
    // Indented, so the viewer's error line numbers still tell the values apart.
    return replaced ? JSON.stringify(document, null, 2) : content;
  } catch {
    // Unreadable YAML, or aliases nested deeper than the call stack.
    return content;
  }
}

function readAsyncApiDocument(content: string): JsonNode | undefined {
  const documents = loadAll(content, { schema: ASYNCAPI_YAML_SCHEMA });
  const [document] = documents;
  return documents.length === 1 && isNode(document) && 'asyncapi' in document ? document : undefined;
}

// `sizes` holds each node's expanded size once measured, so a shared node is measured once; a node still being
// measured reads as infinite, which a loop back to it reports.
function expandedSize(node: unknown, sizes: Map<JsonNode, number>): number {
  if (!isNode(node)) {
    return typeof node === 'string' ? 1 + Math.floor(node.length / 64) : 1;
  }
  const measured = sizes.get(node);
  if (measured !== undefined) {
    return measured;
  }
  sizes.set(node, Infinity);
  let size = 1;
  for (const [key, value] of Object.entries(node)) {
    size += Math.floor(key.length / 64) + expandedSize(value, sizes);
    if (size > MAX_EXPANDED_SIZE) {
      break;
    }
  }
  sizes.set(node, size);
  return size;
}

function replaceMultiFormatSchemas(node: JsonNode): boolean {
  let replaced = false;
  for (const [key, value] of Object.entries(node)) {
    if (!isNode(value)) {
      continue;
    }
    if (isXmlSchemaFormat(value.schemaFormat) && 'schema' in value) {
      node[key] = quoteXmlSchema(value.schemaFormat, value.schema);
      replaced = true;
    } else {
      replaced = replaceMultiFormatSchemas(value) || replaced;
    }
  }
  return replaced;
}

function replaceMessagePayloads(node: JsonNode): boolean {
  let replaced = false;
  for (const value of Object.values(node)) {
    if (isNode(value)) {
      replaced = replaceMessagePayloads(value) || replaced;
    }
  }
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
  return typeof schemaFormat === 'string' && XML_SCHEMA_FORMAT.test(schemaFormat.split(';')[0].trim());
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
