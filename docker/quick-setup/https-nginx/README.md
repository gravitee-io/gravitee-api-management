# Nginx

Here is a docker-compose to run APIM Gateway with nginx https.

You can now access your component like this:

| Component      	| URL                      	    |
|----------------	|-------------------------------|
| Gateway        	| https://localhost/ 	          |
| Management API 	| http://localhost/management/ |
| Portal API 	    | http://localhost/portal/     |
| Console UI 	    | http://localhost/console/    |
| Portal UI 	    | http://localhost/dev/        |
|                	| 	                             |

## How to run?

Generate certificates using the script `./certificate/generate.sh`

⚠️ You need a license file to be able to run Enterprise Edition of APIM. Do not forget to add your license file into `./.license`.

`APIM_VERSION={APIM_VERSION} docker-compose up -d ` 

To be sure to fetch last version of images, you can do
`export APIM_VERSION={APIM_VERSION} && docker-compose down -v && docker-compose pull && docker-compose up`

## How to use?

Import the two APIS:
- [./echo-v2-1-0.json](echo-v2-1-0.json)
- [./echo-v4-1-0.json](echo-v4-1-0.json)

Then, you can call them with the following commands

```bash
curl --cert-type P12 --cert .certificates/client.keystore.p12:gravitee --cacert .certificates/ca.pem https://localhost/echo-v2
```
```bash
curl --cert-type P12 --cert .certificates/client.keystore.p12:gravitee --cacert .certificates/ca.pem https://localhost/echo-v4
```
