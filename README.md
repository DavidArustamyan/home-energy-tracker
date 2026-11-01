# Home Energy Service


A microservices reference implementation for monitoring and reasoning about household electricity usage.
The system accepts energy readings from devices, processes them asynchronously, stores time-series metrics, raises alerts when usage spikes, and exposes a unified API through an API Gateway with resilience
security and observability built in.

### Spring Boot 4, Spring Cloud Gateway, Apache Kafka, Keycloak, InfluxDB, MySQL, Prometheus/Grafana/ Spring AI (Ollama) 

## Services

| Service | Port |
|---|---|
| api-gateway | 9000 |
| user-service | 8080 |
| device-service | 8081 |
| ingestion-service | 8082 |
| usage-service | 8083 |
| alert-service | 8084 |
| insight-service | 8085 |

**Attribution.** This project is based on the open tutorial
> [leetjourney/home-energy-tracker](https://github.com/leetjourney/home-energy-tracker)
> (CC BY-NC 4.0). It has been modified and extended — see
> [Changes vs. the original](#changes-vs-the-original).
> 

## Getting started

```bash
docker compose up -d     # infrastructure
docker compose down      # stop
```

## Changes vs. the original

- Fixed broken JWT issuer property in api-gateway (`jwt,issuer-uri` → `jwt.issuer-uri`)
- Upgraded all services to Spring Boot 4.1 (insight-service: Boot 3.5 → 4.1, Spring AI 1.1 → 2.0)
- OpenAPI docs for ingestion- and insight-service, aggregated in the gateway
- Exception handling in user-service (`UserNotFoundException` + handler)
- Logging/timing aspects in device-service
- RESTful device creation endpoint (`POST /api/v1/device`)

## License

CC BY-NC 4.0 — see [LICENSE](LICENSE).