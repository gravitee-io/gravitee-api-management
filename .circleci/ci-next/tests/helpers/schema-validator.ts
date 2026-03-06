import Ajv from 'ajv';
import addFormats from 'ajv-formats';

const SCHEMA_URL = 'https://raw.githubusercontent.com/CircleCI-Public/circleci-yaml-language-server/main/schema.json';

let cachedValidator: ReturnType<Ajv['compile']> | null = null;

export async function getValidator() {
  if (cachedValidator) return cachedValidator;
  const res = await fetch(SCHEMA_URL);
  const schema = await res.json();
  const ajv = new Ajv({ allErrors: true, strict: false });
  addFormats(ajv);
  cachedValidator = ajv.compile(schema);
  return cachedValidator;
}

export async function validateCircleCIConfig(config: Record<string, unknown>) {
  const validate = await getValidator();
  const valid = validate(config);
  return { valid, errors: validate.errors };
}
