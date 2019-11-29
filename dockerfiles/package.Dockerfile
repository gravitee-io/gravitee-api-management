FROM gravitee-portal-webui/dependencies

RUN npm run build:prod

CMD ["bash", "-c", "cp -r /usr/build/dist /usr/target/ && chmod 777 -R /usr/target/"]
