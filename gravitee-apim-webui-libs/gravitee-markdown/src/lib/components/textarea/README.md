# Textarea Component

Multi-line text input with label, validation, and error display. It is designed for GMD forms and can emit field state when `fieldKey` is provided.

## Usage

### Basic

```html
<gmd-textarea name="message" label="Message" placeholder="Type your message..."></gmd-textarea>
```

### Required with validation

```html
<gmd-textarea name="description" label="Description" required="true" minLength="10" maxLength="500"></gmd-textarea>
```

### Field key for form state

```html
<gmd-textarea name="notes" label="Notes" fieldKey="notes"></gmd-textarea>
```

### Readonly and disabled states

```html
<gmd-textarea name="readonly" label="Readonly" value="Cannot edit" readonly="true"></gmd-textarea>
<gmd-textarea name="disabled" label="Disabled" value="Disabled" disabled="true"></gmd-textarea>
```

## Properties

| Property      | Type                       | Default     | Description                                                 |
| ------------- | -------------------------- | ----------- | ----------------------------------------------------------- |
| `fieldKey`    | `string \| undefined`      | `undefined` | Key used to emit form state events.                         |
| `name`        | `string`                   | `''`        | Textarea name and id.                                       |
| `label`       | `string \| undefined`      | `undefined` | Label shown above the textarea.                             |
| `placeholder` | `string \| undefined`      | `undefined` | Placeholder text.                                           |
| `value`       | `string \| undefined`      | `undefined` | Initial value.                                              |
| `required`    | `boolean`                  | `false`     | Marks the field as required.                                |
| `minLength`   | `number \| string \| null` | `null`      | Minimum length constraint.                                  |
| `maxLength`   | `number \| string \| null` | `null`      | Maximum length constraint.                                  |
| `rows`        | `number`                   | `4`         | Visible number of rows.                                     |
| `readonly`    | `boolean`                  | `false`     | Makes the textarea read-only (focusable, value submitted).  |
| `disabled`    | `boolean`                  | `false`     | Disables the textarea (not focusable, value not submitted). |

## Theming

The textarea component can be customized using SCSS overrides. Use the mixin to override available tokens:

```scss
@use '@gravitee/gravitee-markdown' as gmd;

.my-scope {
  @include gmd.textarea-overrides(
    (
      // Label styling
      outlined-label-text-color: #111827,
      outlined-label-text-size: 0.875rem,
      outlined-label-text-weight: 500,

      // Field styling
      container-text-size: 1rem,
      outlined-input-text-color: #111827,
      outlined-outline-width: 1px,
      outlined-outline-color: #94a3b8,
      outlined-container-shape: 4px,
      outlined-container-color: #ffffff,
      outlined-focus-outline-color: #2563eb,
      outlined-disabled-container-color: #f5f5f5,

      // Error messages (also used for required indicator)
      error-text-color: #dc2626,
      subscript-text-size: 0.8125rem
    )
  );
}
```
