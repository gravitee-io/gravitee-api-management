# Nginx

Here is a docker-compose to run APIM with nginx.

You can now access your component like this:

| Component      	| URL                      	 |
|----------------	|--------------------|
| Gateway        	| http://localhost/gateway/ 	 |
| Management API 	| http://localhost/management/ |
| Portal API 	    | http://localhost/portal/ |
| Console UI 	    | http://localhost/console/ |
| Portal UI 	    | http://localhost/  |
|                	| 	                  |

## How to run ?

`APIM_VERSION={APIM_VERSION} docker-compose up -d ` 

To be sure to fetch last version of images, you can do
`export APIM_VERSION={APIM_VERSION} && docker-compose down -v && docker-compose pull && docker-compose up`

