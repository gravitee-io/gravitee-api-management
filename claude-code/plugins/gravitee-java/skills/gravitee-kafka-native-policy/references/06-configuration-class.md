# Configuration Class

The configuration class models the policy's user-configurable options. It implements `io.gravitee.policy.api.PolicyConfiguration` and is deserialized from the JSON schema defined in `schema-form.json`.

## Package Convention

```
com.graviteesource.policy.kafka.<policy_package>.configuration
```

## Style A: Lombok Getter/Setter (mutable, traditional)

Used by most existing policies (message-filtering, IP filtering, ACL):

```java
package com.graviteesource.policy.kafka.<policy_package>.configuration;

import io.gravitee.policy.api.PolicyConfiguration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class <PolicyName>PolicyConfiguration implements PolicyConfiguration {

    /**
     * Description of the field.
     */
    private String someField;

    /**
     * Boolean field with a default value.
     */
    private boolean enabled = true;
}
```

## Style B: Java Record (immutable, modern)

Used by newer policies (topic-mapping):

```java
package com.graviteesource.policy.kafka.<policy_package>.configuration;

import io.gravitee.policy.api.PolicyConfiguration;
import java.util.List;

public record <PolicyName>PolicyConfiguration(
    List<SomeItem> items
) implements PolicyConfiguration {

    public record SomeItem(String key, String value) {
        public SomeItem() {
            this(null, null);
        }
    }
}
```

## Notes

- The field names must match the property names in `schema-form.json` exactly (case-sensitive).
- For record-style configurations, provide a no-arg constructor on nested records for Jackson deserialization.
- Prefer Style A (Lombok) for configurations with defaults or complex validation.
- Prefer Style B (Record) for simple, immutable configurations.
- If fields support EL (Expression Language), store them as `String` and evaluate at runtime via `ctx.getTemplateEngine().eval(...)`.
