# MECHANIUM external addon example

A small Java 21 addon showing the descriptor, lifecycle, content registration and event listener API.

## Build

The API must first be installed into the local Maven repository from the MECHANIUM checkout:

```bash
mvn -pl mechanium-api -am install
cd examples/mechanium-addon-example
mvn clean package
```

The output JAR is:

```text
target/mechanium-addon-example-1.0.0.jar
```

## Install

Copy the JAR to:

```text
plugins/MECHANIUM/addons/
```

Restart the server. The example depends on the built-in `agriculture` module and registers a small demonstration category, item and shapeless recipe. The example is not intended as a production content pack.

## Notes

- Both `mechanium-api` and `paper-api` are declared as `provided`; do not shade copies into the addon.
- The `mechanium-addon.properties` descriptor must be at the root of the JAR.
- The loader executes addon code inside the server process. It is not a security sandbox.
- The example uses the current API v1 descriptor contract.
