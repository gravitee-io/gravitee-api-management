# Sharding tags

Shard tags let you constrain/deployed certain APIs to certain Gateway instances sharing the same shard tags.

You can create a new shard tag by clicking on the *New tag* input and press *SAVE* button.
A shard tag's ID will be automatically created.

To make the association between APIs and Gateway instances,
you must choose this shard tag in the API configuration page and copy/paste the shard tag's ID to the `gravitee.yml` file of the gateway instance:

```yaml
# Sharding tags configuration
# tags: products,!international
```
