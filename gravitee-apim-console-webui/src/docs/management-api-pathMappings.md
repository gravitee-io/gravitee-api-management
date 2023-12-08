# API Path Mappings

The path mappings allow you to get top resources on analytics dashboard.

The resources will be grouped according to this  mapping.

Example:

```text
/products
/products/1
/products/2
/products/2
/products/1/characteristics
```


With the following path mapping:
```text
/products
/products/:productId
```

You will get the result below:
```text
/products => 1 hit
/products/:productId => 3 hits
```
