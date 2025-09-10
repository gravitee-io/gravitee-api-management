# MongoDB

Here is a docker-compose to run APIM with MongoDB as database.

---
> For more information, please read :
> https://documentation.gravitee.io/apim/getting-started/configuration/repositories/mongodb
---

## How to run?

💡 (Optional) If you have an enterprise license, you can export it as a Base64-encoded environment variable to gain full access to Gravitee features.
```cmd
  $ export LICENSE_KEY=*****
```
or move your license file into `./.license`.

if you're having an issue during ./license folder creation, please use 

```cmd
  $ cd ../.. && make prepare TARGET=mongodb
```

`APIM_VERSION={APIM_VERSION} docker-compose up -d ` 

To be sure to fetch last version of images, you can do
`export APIM_VERSION={APIM_VERSION} && docker-compose down -v && docker-compose pull && docker-compose up`

