# API Proxy

Configure your connection to the backend endpoint(s).

1. Endpoints

Declare new endpoint by clicking on the (+) button and configure existing endpoints by clicking on the *settings* button.

2. Load Balancing

Choose among 4 kinds of load balancing method to manage how to dispatch API calls.

3. Fail over

Configure redirect calls strategy in case of some endpoint failures.

4. CORS

CORS configuration to handle connection from front-end applications.

5. Deployment

Choose registered sharding tags to choose on which gateway the API must be deployed. Sharding tags are configured at Gateway level.
User must belong to a group authorized to deploy on the tag.

6. Multi-tenant

Enable multi-tenant mode to route incoming calls to the corresponding tenant endpoint. Tenants are configured at Gateway level.

7. Logging

Enable logging at to ease debugging. Logs will be available under API Logs section.
