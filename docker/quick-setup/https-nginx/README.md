# Nginx

Here is a docker-compose to run APIM with nginx https.

You can now access your component like this:

| Component      	| URL                      	    |
|----------------	|-------------------------------|
| Gateway        	| https://localhost/gateway/ 	  |
| Management API 	| https://localhost/management/ |
| Portal API 	    | https://localhost/portal/     |
| Console UI 	    | https://localhost/console/    |
| Portal UI 	    | https://localhost/            |
|                	| 	                             |

## How to run ?

Generate certificates

`openssl req -new -newkey rsa:4096 -x509 -sha256 -days 365 -nodes -out ./certificate/nginx-certificate.crt -keyout ./certificate/nginx.key`

⚠️ You need a license file to be able to run Enterprise Edition of APIM. Do not forget to add your license file into `./.license`.

`APIM_VERSION={APIM_VERSION} docker-compose up -d ` 

To be sure to fetch last version of images, you can do
`export APIM_VERSION={APIM_VERSION} && docker-compose down -v && docker-compose pull && docker-compose up`



