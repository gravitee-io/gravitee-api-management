// Snippet: Mix host-owned meta fields with a plugin JSON Schema
//
// Use when a host module renders a plugin config (immutable JSON Schema from
// the backend) alongside host-owned meta fields (name, description, or any
// extra logic validated by your favourite schema lib).
// One useForm shared across both — `composeResolvers` merges Zod + ajv
// errors so RHF surfaces them on the right inputs.
// Do NOT use for the simple case (no meta) — see snippets/json-schema-form-simple.tsx.
// Do NOT install Zod just for this — any RHF-compatible resolver works:
// `yupResolver`, `joiResolver`, `valibotResolver`, or a hand-written
// `Resolver<T>`. Only the first argument to `composeResolvers` changes.
//
// Replace {PLACEHOLDERS} with your actual values.
// See Storybook "Composed/JsonSchemaForm → With external (hand-coded) fields".

import { zodResolver } from '@hookform/resolvers/zod';
import { useMemo } from 'react';
import { type Control, type Resolver, useForm } from 'react-hook-form';
import { z } from 'zod';
import type { JsonSchema } from '@gravitee/graphene-core';
import {
  Button,
  composeResolvers,
  extractDefaults,
  Field,
  FieldError,
  FieldLabel,
  Input,
  JsonSchemaForm,
  jsonSchemaResolver,
} from '@gravitee/graphene-core';

// Host-owned meta — extend with z.union / z.refine / async checks as needed.
const metaSchema = z.object({
  name: z.string().min(1, 'Name is required'),
});

type FormValues = z.infer<typeof metaSchema> & { config: Record<string, unknown> };

interface PluginConfigPageProps {
  readonly pluginSchema: JsonSchema;
  readonly initialValue?: Partial<FormValues>;
  // Host environment / feature flags read by `gioConfig.displayIf` / `gioConfig.disableIf`
  // conditions of the form `{ "context.X": value }`. Remove if your schemas declare no
  // `context.X` references.
  readonly environment: 'production' | 'staging' | 'dev';
  readonly onSave: (data: FormValues) => void;
}

export function PluginConfigPage({ pluginSchema, initialValue, environment, onSave }: PluginConfigPageProps) {
  // `basePath: 'config'` must match the `name` prop on <JsonSchemaForm> below.
  // Cast widens zodResolver to the composite values shape — each inner resolver reads
  // only its own slice of `values` at runtime, so the cast is sound.
  const resolver = useMemo(
    () =>
      composeResolvers<FormValues>(
        zodResolver(metaSchema) as unknown as Resolver<FormValues>,
        jsonSchemaResolver<FormValues>(pluginSchema, { basePath: 'config' }),
      ),
    [pluginSchema],
  );

  // Defaults: schema defaults seed `config`, then saved values override per key. Spread
  // `initialValue` at the top level for meta; explicit merge on `config` so saved-but-partial
  // configs do NOT wipe schema defaults for fields the user never touched (or that the schema
  // added in a newer plugin version). For deeply nested schemas, swap `Object.assign` for a
  // recursive merge (e.g. lodash.merge).
  const schemaDefaults = useMemo(
    () => (extractDefaults(pluginSchema) as Record<string, unknown>) ?? {},
    [pluginSchema],
  );

  // Stable identity for the `context` prop on JsonSchemaForm — inline `{{ environment }}`
  // would create a fresh ref each render and re-render every field downstream.
  const formContext = useMemo(() => ({ environment }), [environment]);

  const form = useForm<FormValues>({
    resolver,
    mode: 'onTouched',
    criteriaMode: 'all',
    defaultValues: {
      name: '',
      ...initialValue,
      config: { ...schemaDefaults, ...(initialValue?.config ?? {}) },
    },
  });
  const { name: nameError } = form.formState.errors;

  return (
    <form noValidate onSubmit={form.handleSubmit(onSave)} className="flex flex-1 flex-col gap-4 p-8">
      {/* Host meta — validated by Zod. Duplicate this block for `description`, etc. */}
      <Field data-invalid={!!nameError}>
        <FieldLabel htmlFor="name">Name</FieldLabel>
        <Input id="name" {...form.register('name')} aria-invalid={!!nameError} />
        {nameError && <FieldError errors={[nameError]} />}
      </Field>

      {/* Plugin sub-tree — fields registered under `values.config.*`. JsonSchemaForm is typed
          against the generic RHF FieldValues; cast widens the narrow composite control.
          `context` is memoized at the call site (here, via the `formContext` const below) so
          a fresh ref does not bust JsonSchemaForm's internal memo every parent render. */}
      <JsonSchemaForm
        schema={pluginSchema}
        control={form.control as unknown as Control}
        name="config"
        context={formContext}
      />

      <Button type="submit">Save</Button>
    </form>
  );
}
