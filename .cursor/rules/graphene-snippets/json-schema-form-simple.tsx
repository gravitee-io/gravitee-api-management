// Snippet: Render a plugin JSON Schema in a single useForm
//
// Use when a host module receives a JSON Schema (from a Gravitee plugin or
// API definition) and the form has **no fields besides those declared in the
// schema**. One useForm, one resolver, one submit handler.
// Do NOT use for hand-coded forms — write the fields directly with Graphene
// primitives (Field, Input, Select…) instead.
// Do NOT use when the form has host-owned meta fields (name, description,
// etc.) alongside the schema — see snippets/json-schema-form-with-meta.tsx.
//
// Replace {PLACEHOLDERS} with your actual values.
// See Storybook "Composed/JsonSchemaForm → Playground" for an interactive demo.

import { useMemo } from 'react';
import { useForm } from 'react-hook-form';
import { Button, extractDefaults, JsonSchemaForm, jsonSchemaResolver } from '@gravitee/graphene-core';
import type { JsonSchema } from '@gravitee/graphene-core';

interface PluginConfigPageProps {
  // Immutable: passing a new reference re-builds the form and recompiles ajv.
  readonly pluginSchema: JsonSchema;

  // Pass a stable reference (e.g. from a TanStack Query / SWR cache, or wrap with `useMemo`
  // at the call site). A fresh object on every parent render defeats the `defaultValues`
  // memo and re-seeds the form, wiping in-progress user edits.
  readonly initialValue?: Record<string, unknown>;
  readonly onSave: (data: Record<string, unknown>) => void;
}

export function PluginConfigPage({ pluginSchema, initialValue, onSave }: PluginConfigPageProps) {
  // Memoize so `jsonSchemaResolver` does not recompile ajv on every parent render.
  const resolver = useMemo(() => jsonSchemaResolver(pluginSchema), [pluginSchema]);
  const defaultValues = useMemo(
    () => initialValue ?? (extractDefaults(pluginSchema) as Record<string, unknown>) ?? {},
    [pluginSchema, initialValue],
  );

  const form = useForm({
    resolver,
    defaultValues,
    criteriaMode: 'all', // surface every failing ajv keyword per field (FieldError renders them as a list)
    mode: 'onTouched', // validate on first blur, then on every change
  });

  // `noValidate` disables the browser's HTML5 validation popups so the resolver +
  // <FieldError> remain the single source of validation messages.
  return (
    <form noValidate onSubmit={form.handleSubmit(onSave)} className="flex flex-1 flex-col gap-4 p-8">
      <JsonSchemaForm schema={pluginSchema} control={form.control} name="" />
      <Button type="submit">Save</Button>
    </form>
  );
}
