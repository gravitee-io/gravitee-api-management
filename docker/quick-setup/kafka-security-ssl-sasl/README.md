# Kafka security

Implementation of Kafka authentication and authorisation using different approaches.


## Run locally

Every directory has its own compose file and scripts to test. In the compose file
all services are using a network with name `kafka-cluster-network` which means,
all other containers outside the compose file could access Kafka and Zookeeper nodes by
being attached to this network. For example

```
docker run -it --network kafka-cluster-network confluentinc/cp-kafka:5.0.1 kafka-topics --zookeeper \ zookeeper-1:22181 --list
```

### Setup environment variables

There are 2 environment variables need to be configured

- `export KAFKA_SSL_SECRETS_DIR=$PWD/secrets`
- `export KAFKA_SASL_SCRAM_SECRETS_DIR=$PWD/sasl-scram/secrets`  


### SSL only

To start Kafka and Zookeeper cluster configured only with SSL, you could run the script `start_ssl_only_cluster.sh`

### SASL/SCRAM

Configured both Zookeeper and Kafka to use SASL/SCRAM. To run it

- Make sure you have the SSL keystore and truststore generated and stored in the directory
`kafka-security-ssl-sasl/secrets`
- Run command `kafka-security-ssl-sasl/start_sasl_scram_cluster.sh`
- To run console producer and consumer, you could check the commands in the file `kafka-security-ssl-sasl/sasl-scram/kafka-consumers-producers.sh`
- To add any new account to connect to Kafka, you could find commands in the script file `sasl-scram/add_kafka_accounts_in_zookeeper.sh`


## References

- [Redhat tutorial to configure Zookeeper and Kafka](https://access.redhat.com/documentation/en-us/red_hat_amq_streams/1.0-beta/html/using_amq_streams_on_red_hat_enterprise_linux_rhel/configuring_zookeeper#assembly-configuring-zookeeper-authentication-str)
- [Confluent reference / configure SCRAM](https://docs.confluent.io/current/kafka/authentication_sasl/authentication_sasl_scram.html)
- [Cloudera slides to configure SCRAM](https://www.slideshare.net/JeanPaulAzar1/kafka-tutorial-kafka-security)
- [Blog describe steps to configure SASL/SCRAM](https://sharebigdata.wordpress.com/category/kafka/multiple-saslplainscram/)
