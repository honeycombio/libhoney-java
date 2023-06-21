# Local Development

## Requirements

Java 7+: https://www.java.com/en/download/

## Build

Build with Java 7 and above by running the following from the root directory:

```shell
./mvnw package
```

## Run Tests

To run all tests:

```shell
./mvnw test
```

To run individual tests:

```shell
./mvnw test -pl libhoney "-Dtest=HoneyClientTest"
```
