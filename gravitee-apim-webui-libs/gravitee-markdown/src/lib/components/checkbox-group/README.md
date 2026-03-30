# `gmd-checkbox-group`

A checkbox group component that allows users to select multiple options from a list. Structurally modeled after `gmd-radio` — same fieldset/legend layout, same `GmdFormFieldBase` extension. Options are parsed from a comma-separated string or from EL fallback values. The selected value is serialized as a sorted, comma-separated string.

## Usage

```html
<gmd-checkbox-group
  name="features"
  label="Required Features"
  fieldKey="features"
  required="true"
  options="Authentication,Rate Limiting,Analytics,Caching"
>
</gmd-checkbox-group>
```

## Props

| Attribute  | Type    | Default | Description                                                            |
| ---------- | ------- | ------- | ---------------------------------------------------------------------- |
| `name`     | string  | `''`    | HTML name attribute for the checkbox inputs                            |
| `label`    | string  | —       | Display label shown above the group (renders as `<legend>`)            |
| `fieldKey` | string  | —       | Key used for form state tracking and data collection                   |
| `value`    | string  | —       | Comma-separated preselected values (e.g., `"Option 1,Option 3"`)       |
| `required` | boolean | `false` | Whether at least one option must be selected                           |
| `options`  | string  | `''`    | Comma-separated list of options (e.g., `"Option 1,Option 2,Option 3"`) |
| `disabled` | boolean | `false` | Disables all checkboxes and removes field from form state              |

## Options format

- Comma-separated: `"Option 1,Option 2,Option 3"`
- EL expression with fallback: `"{#api.metadata['features']}:Authentication,Rate Limiting,Analytics"`

When options use EL, the fallback list is used for preview/editing contexts where runtime values cannot be resolved.
If no fallback is provided for an EL expression, the component reports a `missingElFallback` config error.

## Value serialization

The field state `value` is serialized as a comma-separated string of selected items:

- Single selection: `"Option 1"`
- Multiple selections: `"Option 1,Option 3"`
- Nothing selected: `""`

## Validation

- `required`: Reports a `'required'` error when no option is selected. Error message is shown after the field is touched (blurred).

## Theming tokens

The component exposes CSS custom properties under the `checkbox-group` namespace:

| Token                                             | Default             | Description             |
| ------------------------------------------------- | ------------------- | ----------------------- |
| `--gmd-checkbox-group-outlined-label-text-size`   | `0.875rem`          | Label font size         |
| `--gmd-checkbox-group-outlined-label-text-weight` | `500`               | Label font weight       |
| `--gmd-checkbox-group-outlined-label-text-color`  | `inherit`           | Label text color        |
| `--gmd-checkbox-group-error-text-color`           | (theme error color) | Error message color     |
| `--gmd-checkbox-group-subscript-text-size`        | `0.8125rem`         | Error message font size |

Use the `checkbox-group-overrides` mixin from `public-api.scss` to apply token overrides.
