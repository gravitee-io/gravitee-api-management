/**
 * CircleCI pipeline parameter helpers.
 */

export type ParameterType = 'string' | 'boolean' | 'integer' | 'enum';

export interface ParameterDefinition {
  type: ParameterType;
  default?: string | boolean | number;
  enum?: string[];
  description?: string;
}

export const param = {
  string(defaultValue?: string, description?: string): ParameterDefinition {
    return { type: 'string', ...(defaultValue !== undefined && { default: defaultValue }), ...(description && { description }) };
  },

  boolean(defaultValue?: boolean, description?: string): ParameterDefinition {
    return { type: 'boolean', ...(defaultValue !== undefined && { default: defaultValue }), ...(description && { description }) };
  },

  integer(defaultValue?: number, description?: string): ParameterDefinition {
    return { type: 'integer', ...(defaultValue !== undefined && { default: defaultValue }), ...(description && { description }) };
  },

  enum(defaultValue: string, values: string[], description?: string): ParameterDefinition {
    return { type: 'enum', default: defaultValue, enum: values, ...(description && { description }) };
  },
};

export function serializeParameters(params: Record<string, ParameterDefinition>): Record<string, unknown> {
  const result: Record<string, unknown> = {};
  for (const [name, def] of Object.entries(params)) {
    const p: Record<string, unknown> = { type: def.type };
    if (def.default !== undefined) p.default = def.default;
    if (def.enum) p.enum = def.enum;
    if (def.description) p.description = def.description;
    result[name] = p;
  }
  return result;
}
