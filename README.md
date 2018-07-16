# libhoney-java
Java library for sending events to [Honeycomb](https://honeycomb.io). For more information, see the [documentation](https://honeycomb.io/docs/) and [Java SDK guide](https://honeycomb.io/docs/connect/java).

Javadocs are available [here](https://honeycombio.github.io/libhoney-java/) or within the distributed sources.

## Local Build

Build with Java 7 and above by running the following from the root directory:
```
./mvnw package
```

## Installation

Add the following dependency to your maven build:
```
<dependency>
  <groupId>io.honeycomb.libhoney</groupId>
  <artifactId>libhoney-java</artifactId>
  <version>1.0.0</version>
</dependency>
```
For gradle builds add:
```
compile group: 'io.honeycomb.libhoney', name: 'libhoney-java', version: '1.0.0'
```

## Example
Honeycomb can calculate all sorts of statistics, so send the values you care about and let us crunch the
averages, percentiles, lower/upper bounds, cardinality -- whatever you want -- for you.

```java
public class SendImmediately {
    public static HoneyClient initializeClient() {
        return create(options()
            .setWriteKey("myTeamWriteKey")
            .setDataset("Cluster Dataset")
            .build()
        );
    }

    public static void main(String... args) throws UnknownHostException {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("randomString", UUID.randomUUID().toString());
        dataMap.put("cpuCores", Runtime.getRuntime().availableProcessors());
        dataMap.put("hostname", InetAddress.getLocalHost().getHostName());

        try (HoneyClient honeyClient = initializeClient()) {
            honeyClient.send(dataMap);
        }
    }
}
```

See our [examples repo](https://github.com/honeycombio/examples/tree/master/java-webapp) for a sample TODO webapp demonstrating how to use features of the library.
