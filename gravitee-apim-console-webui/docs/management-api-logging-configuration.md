# Logging

Logging allows the API Publisher to better manage what should be logged when a request is handle by API Gateway.

Also, API Publisher has a fine-control on when request and response should be logged by defining a logging condition.

Here are some condition examples:

```text
#request.headers['X-DEBUG'] == 'true'
```

```text
#context.application == 'xxxx-xxx-xxx-xxxx'
```

```text
#request.headers['X-DEBUG'] == 'true' && #context.application == 'xxxx-xxx-xxx-xxxx'
```

```text
#request.timestamp < #date.now()
```
