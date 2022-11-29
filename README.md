# spring-boot-with-metrics

This demo app shows how a Spring Boot application can expose a Prometheus metrics endpoint for scraping.

- For Java 11+

🚼 The app was initially created with [Spring Initializr][init] and then by following the [RESTful service tutorial on spring.io][rest-tutorial].

## To run

To run the app:

    mvn clean spring-boot:run
    
This will expose Prometheus metrics at `/actuator/prometheus`. This is a simple `key value` listing of metrics. You can check them out using `curl`, for example:

    $ curl http://localhost:8080/actuator/prometheus
    # HELP process_uptime_seconds The uptime of the Java virtual machine
    # TYPE process_uptime_seconds gauge
    process_uptime_seconds 10.284
    # HELP jvm_threads_states_threads The current number of threads having NEW state
    # TYPE jvm_threads_states_threads gauge
    jvm_threads_states_threads{state="runnable",} 9.0
    jvm_threads_states_threads{state="blocked",} 0.0
    jvm_threads_states_threads{state="waiting",} 11.0
    ...
    # TYPE tomcat_sessions_alive_max_seconds gauge
    tomcat_sessions_alive_max_seconds 0.0
    ...    

Next, try accessing the API in the app at `http://localhost:8080/greeting` either in a web browser or using `curl`. Check the `/actuator/prometheus` endpoint again. You should see the `http_server_requests_seconds_sum` metric increase. You will also see a new metric `greeting_time_seconds` which is a custom metric added to the app:

    $ curl http://localhost:8080/actuator/prometheus | grep greeting_time_seconds
    # HELP greeting_time_seconds Time taken to return greeting
    # TYPE greeting_time_seconds summary
    greeting_time_seconds{class="com.tutorialworks.demos.springbootwithmetrics.GreetingController",exception="none",method="greeting",quantile="0.5",} 0.0
    greeting_time_seconds{class="com.tutorialworks.demos.springbootwithmetrics.GreetingController",exception="none",method="greeting",quantile="0.9",} 0.0
    ...

## Timing a custom method

The method in the application which responds to the REST request has been annotated with `@Timed`, so Micrometer will capture the execution time of this method:

```java
@GetMapping("/greeting")
@Timed(value = "greeting.time", description = "Time taken to return greeting",
        percentiles = {0.5, 0.90})
public Greeting greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
    return new Greeting(counter.incrementAndGet(), String.format(template, name));
}
```

Once you make a request to the service at <http://localhost:8080/greeting>, you will also see a new metric exposed, `greeting_time_seconds` exposed, which shows the execution time of the `greeting` method:

    # HELP greeting_time_seconds Time taken to return greeting
    # TYPE greeting_time_seconds summary
    greeting_time_seconds{class="com.tutorialworks.demos.springbootwithmetrics.GreetingController",exception="none",met
    hod="greeting",quantile="0.9",} 0.02097152
    greeting_time_seconds_count{class="com.tutorialworks.demos.springbootwithmetrics.GreetingController",exception="non
    e",method="greeting",} 1.0
    greeting_time_seconds_sum{class="com.tutorialworks.demos.springbootwithmetrics.GreetingController",exception="none"
    ,method="greeting",} 0.021689345
    # HELP greeting_time_seconds_max Time taken to return greeting
    # TYPE greeting_time_seconds_max gauge
    greeting_time_seconds_max{class="com.tutorialworks.demos.springbootwithmetrics.GreetingController",exception="none",method="greeting",} 0.021689345


From the [Micrometer docs][timerdocs]:

> All implementations of _Timer_ report at least the total time and count of events as separate time series.

## Getting metrics into Prometheus

Now we need to get these metrics into Prometheus.

In another terminal, use Podman to start an ephemeral Prometheus in a container, and use the `host` networking option, which will allow the container to access the Spring Boot app on `localhost`:

    podman run --net=host \
        -v ./prometheus.yml:/etc/prometheus/prometheus.yml:Z \
        prom/prometheus
        
Check the Prometheus console at <http://localhost:9090>.

- Go to _Targets_, you should see the Spring Boot app being scraped successfully.

- On the _Graph_ page, you should be able to type in a metric from the application (e.g. `tomcat_sessions_active_current_sessions`, or `greeting_time_seconds`) and see the raw data, or plot a graph

## Troubleshooting

Inside the Prometheus container, you can check that you can access the Spring Boot metrics:

    $ podman exec -it <container-name> sh
    $$ wget -S -O - http://localhost:8080/actuator/prometheus


[rest-tutorial]: https://spring.io/guides/gs/rest-service/
[init]: https://start.spring.io
[timerdocs]: https://micrometer.io/docs/concepts#_timers
