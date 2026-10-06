# Coding Guidelines for gravitee-apim-distribution

## Running the inference plugin on a Mac

The `gravitee-inference-service` zip bundled by default only ships the Linux ONNX Runtime natives, which is what the images need. To run the plugin natively on macOS, build with `-Dgravitee-inference-service.classifier=macos`: it swaps in the `macos` zip, never both.

- **Never for an image.** A distribution built with the macOS zip and packaged into a Linux image starts, then fails when the plugin loads. `task docker-backend` does not rebuild anything: it packages whatever the `target/` folders hold. Build again without the property before building an image.
- **Never in `~/.m2/settings.xml` or the environment.** The property is read by every build, images included. Pass it per build.
