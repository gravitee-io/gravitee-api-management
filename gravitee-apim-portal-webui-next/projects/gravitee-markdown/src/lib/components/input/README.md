# Input Component

Single-line text input with label, validation, and error display. It is designed for GMD forms and can emit field state when `fieldKey` is provided.

## Usage

### Basic

```html
<gmd-input name="email" label="Email" placeholder="you@example.com"></gmd-input>
```

### Required with validation

```html
<gmd-input name="company" label="Company" required="true" minLength="2" maxLength="50"></gmd-input>
```

### Field key for form state

```html
<gmd-input name="email" label="Email" fieldKey="email" required="true"></gmd-input>
```

### Readonly and disabled states

```html
<gmd-input name="readonly" label="Readonly" value="Cannot edit" readonly="true"></gmd-input>
<gmd-input name="disabled" label="Disabled" value="Disabled" disabled="true"></gmd-input>
```

## Properties

| Property | Type | Default | Description |
| --- | --- | --- | --- |
| `fieldKey` | `string \| undefined` | `undefined` | Key used to emit form state events. |
| `name` | `string` | `''` | Input name and id. |
| `label` | `string \| undefined` | `undefined` | Label shown above the input. |
| `placeholder` | `string \| undefined` | `undefined` | Placeholder text. |
| `value` | `string \| undefined` | `undefined` | Initial value. |
| `required` | `boolean` | `false` | Marks the field as required. |
| `minLength` | `number \| string \| null` | `null` | Minimum length constraint. |
| `maxLength` | `number \| string \| null` | `null` | Maximum length constraint. |
| `pattern` | `string \| undefined` | `undefined` | RegExp pattern constraint. |
| `readonly` | `boolean` | `false` | Makes the input read-only (focusable, value submitted). |
| `disabled` | `boolean` | `false` | Disables the input (not focusable, value not submitted). |

## Theming

The input component can be customized using SCSS overrides. Use the mixin to override available tokens:

```scss
@use '@gravitee/gravitee-markdown' as gmd;

.my-scope {
  @include gmd.input-overrides((
    // Label styling
    label-text-color: #111827,
    label-text-size: 0.875rem,
    label-text-weight: 500,

    // Required indicator
    required-text-color: #dc2626,

    // Field styling
    field-text-size: 1rem,
    field-text-color: #111827,
    field-outline-width: 1px,
    field-outline-color: #94a3b8,
    field-outline-radius: 4px,
    field-background-color: #ffffff,
    field-focus-outline-color: #2563eb,

    // Error messages
    error-text-color: #dc2626,
    error-text-size: 0.8125rem,
  ));
}
```
