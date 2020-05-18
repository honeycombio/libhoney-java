# Building HoneyClient with HoneyClientBuilder
HoneyClientBuilder is a simple way to create HoneyClient instances.

## Initialization
This an example of creating a HoneyClient instance. The typical application will only have one HoneyClient instance.

```java
package com.example.myapp;

import io.honeycomb.libhoney.builders.HoneyClientBuilder;
import io.honeycomb.libhoney.HoneyClient;

public class MyApp{
    /// ...
    public static void main( final String[] args ) {
        final HoneyClientBuilder builder = new HoneyClientBuilder();
        // Configure builder
        builder.dataSet("test-dataset").writeKey("WRITE_KEY");
        // Build instance
        final HoneyClient client = builder.build();

        // ...

        // Call close at program termination to ensure all pending
        // spans are sent.
        client.close();
    }
}
```

## Working with a proxy
If the application server needs to use an HTTP proxy, configure it like this.

```java
package com.example.myapp;

import io.honeycomb.libhoney.HoneyClient;
import io.honeycomb.libhoney.builders.HoneyClientBuilder;

public class Example{
    HoneyClient client;

    public Example(){
        client = new HoneyClientBuilder()
                    .dataSet("test-dataset")
                    .writeKey("WRITE_KEY")
                    .addProxyNoCredential("https://myproxy.example.com")
                    .build();
    }
}
```

## Configuring Libhoney to disable sending events to Honeycomb
Setting the `Transport` to a custom implementation (i.e. mock) can be used to disable sending data to Honeycomb.

```java
package com.example.myapp;

import io.honeycomb.libhoney.HoneyClient;
import io.honeycomb.libhoney.builders.HoneyClientBuilder;
import com.example.MyApp.MockTransport;

public class Example{
    HoneyClient client;

    public Example(){
        client = new HoneyClientBuilder()
                    .dataSet("test-dataset")
                    .writeKey("WRITE_KEY")
                    .transport(new MockTransport())
                    .build();
    }
}
```
