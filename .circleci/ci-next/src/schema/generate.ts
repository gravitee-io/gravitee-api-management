import { readFileSync, writeFileSync, mkdirSync } from 'fs';
import { compile } from 'json-schema-to-typescript';
import { dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));

async function main() {
  const schemaPath = `${__dirname}/schema.json`;
  const outputPath = `${__dirname}/generated/types.ts`;

  const schema = JSON.parse(readFileSync(schemaPath, 'utf-8'));

  // Workaround: break recursive $ref cycles by giving them standalone names.
  // Without titles, json-schema-to-typescript tries to inline these recursively
  // and crashes with a stack overflow.
  // See: https://github.com/bcherny/json-schema-to-typescript/issues/482
  if (schema.definitions?.logic) schema.definitions.logic.title = 'Logic';
  if (schema.definitions?.step) schema.definitions.step.title = 'Step';
  if (schema.definitions?.environment) schema.definitions.environment.title = 'Environment';

  const ts = await compile(schema, 'CircleCIConfig', {
    bannerComment: '/* Auto-generated from CircleCI schema.json — do not edit */',
  });

  // Post-process: fix TS2411 — `version` property conflicts with string index signature
  // in the `workflows` type. Remove the `version` field (CircleCI 2.0 legacy, not used in 2.1+).
  const fixed = ts.replace(
    /(\s+workflows\?: \{)\n\s+version\?: number \| string;\n(\s+\[k: string\]:)/,
    '$1\n$2',
  );

  // Add @ts-nocheck — generated types have TS2411 errors where named properties
  // in Step conflict with the [k: string] index signature. This is inherent to the
  // schema's use of additionalProperties. The SDK uses its own hand-crafted types.
  const withNoCheck = fixed.replace(
    '/* Auto-generated from CircleCI schema.json — do not edit */',
    '/* Auto-generated from CircleCI schema.json — do not edit */\n// @ts-nocheck',
  );

  mkdirSync(dirname(outputPath), { recursive: true });
  writeFileSync(outputPath, withNoCheck);
  console.log(`Generated types written to ${outputPath}`);
}

main().catch((err) => {
  console.error('Type generation failed:', err);
  process.exit(1);
});
