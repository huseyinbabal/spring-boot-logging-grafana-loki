# Spring Boot logging, from zero to Grafana Loki

A small Spring Boot order service that logs at every level, writes its logs as JSON, lets you change the level at runtime, and ships everything to Grafana Loki with Grafana Alloy. At the end you can find a failed payment, with its stack trace, in one Grafana query.

Article: https://huseyinbabal.dev/posts/spring-boot-logging-grafana-loki/

| Part | Version |
| --- | --- |
| Spring Boot | 4.1.1 |
| Java | 25 |
| Grafana Alloy | 1.20.1 |
| Loki | 3.7.8 |
| Grafana | 13.2.4 |

## What's inside

```text
app/             the "orders" service (Spring Boot, Maven)
compose.yaml     orders + Alloy + Loki + Grafana
config.alloy     Alloy: find containers labelled logging=alloy, read their stdout, push to Loki
grafana/         provisions Loki as Grafana's default data source
```

The fake payment provider in `PaymentClient` makes every order id ending in 3 time out once (a WARN line) and every id ending in 7 fail (an ERROR line with a stack trace). Order ids start at 41, so the seventh order, 47, is the one that fails.

## Run it

You need Docker with Compose. The first start builds the app image.

```bash
docker compose up -d
```

Create seven orders and pay for them:

```bash
for item in book pen lamp mug desk chair bag; do curl -s -X POST "localhost:8080/orders?item=$item"; echo; done
for id in $(seq 41 47); do echo "$id $(curl -s -X POST localhost:8080/orders/$id/pay)"; done
```

```text
41 paid
...
46 paid
47 payment failed
```

## Read the logs

Every line is JSON (Spring Boot's structured logging, `logging.structured.format.console: logstash` in `application.yml`):

```bash
docker compose logs --no-log-prefix orders | grep 'order 42 created' | jq .
```

## Change the level at runtime

```bash
# current level of the app's package
curl -s localhost:8080/actuator/loggers/dev.huseyincodes.orders; echo

# switch it to DEBUG without a restart
curl -s -o /dev/null -w '%{http_code}\n' -X POST localhost:8080/actuator/loggers/dev.huseyincodes.orders \
  -H 'Content-Type: application/json' -d '{"configuredLevel":"DEBUG"}'

# the cache hits and misses now show up
curl -s localhost:8080/orders/41 -o /dev/null; curl -s localhost:8080/orders/99 -o /dev/null
docker compose logs --no-log-prefix --since 10s orders | jq -r '.level + "  " + .message'

# back to INFO
curl -s -o /dev/null -X POST localhost:8080/actuator/loggers/dev.huseyincodes.orders \
  -H 'Content-Type: application/json' -d '{"configuredLevel":"INFO"}'
```

## Find the error in Grafana

Open http://localhost:3000, go to **Explore** (Loki is already the data source) and run:

```text
{app="orders"}                              every line from the orders app
{app="orders"} | json | level="ERROR"       only the errors, with the stack trace
{app="orders"} |= "order 47"                everything that happened to order 47
```

Grafana runs with anonymous admin access so you don't need to log in. That's fine on your laptop; don't expose this setup to a network.

## Clean up

```bash
docker compose down
```
