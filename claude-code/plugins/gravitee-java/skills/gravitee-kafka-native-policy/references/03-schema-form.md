# schema-form.json (Configuration Schema)

Located at `src/main/resources/schemas/schema-form.json`. Defines the JSON Schema used to render the policy configuration form in the Gravitee console UI and validate configuration at runtime.

## Template (Minimal)

```json
{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "object",
    "additionalProperties": false,
    "properties": {
        "exampleField": {
            "type": "string",
            "title": "Example Field",
            "description": "Description shown in the UI."
        }
    },
    "required": ["exampleField"]
}
```

## Supported Field Types

### String with EL (Expression Language) support

```json
{
    "filter": {
        "type": "string",
        "title": "Filter condition",
        "description": "Supports EL expressions evaluated at runtime.",
        "gioConfig": {
            "el": true
        }
    }
}
```

### Boolean with default

```json
{
    "enabled": {
        "type": "boolean",
        "title": "Enable feature",
        "description": "Toggle this feature on or off.",
        "default": true
    }
}
```

### Enum (dropdown)

```json
{
    "mode": {
        "type": "string",
        "title": "Processing mode",
        "enum": ["STRICT", "PERMISSIVE"],
        "default": "STRICT"
    }
}
```

### Integer with constraints

```json
{
    "maxMessages": {
        "type": "integer",
        "title": "Max messages",
        "minimum": 1,
        "maximum": 10000,
        "default": 100
    }
}
```

### Array of objects

```json
{
    "mappings": {
        "type": "array",
        "title": "Topic mappings",
        "items": {
            "type": "object",
            "properties": {
                "client": {
                    "type": "string",
                    "title": "Client-side name",
                    "gioConfig": { "el": true }
                },
                "broker": {
                    "type": "string",
                    "title": "Broker-side name",
                    "gioConfig": { "el": true }
                }
            },
            "required": ["client", "broker"]
        }
    }
}
```

## Notes

- Use `"gioConfig": { "el": true }` to enable Expression Language evaluation in string fields.
- `additionalProperties: false` is recommended to prevent unexpected configuration.
- The schema must match the structure of the `PolicyConfiguration` Java class exactly.
- Fields marked as `required` in the schema should also be validated in the configuration class.
