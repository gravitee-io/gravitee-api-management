# TCP Scenario 

## Configuration

Some minimal requirements has to be fulfilled to run the TCP Scenario.

In `scripts/config.json`:

- `"insecureTlSVerify": true` as we use a `self-signed` keystore, we need to indicate K6 client the call is legit.
- `"hosts": { "localhost": "0.0.0.0" }` is the equivalent of `/etc/hosts` in k6. Here the example is pretty simple, but it will need to be modified for other host names.
