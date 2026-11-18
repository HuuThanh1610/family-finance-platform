# OpenAPI Contracts

Mỗi Spring Boot service expose OpenAPI tại runtime:

`http://localhost:<port>/v3/api-docs`

Có thể export và đặt snapshot vào thư mục này khi API ổn định:

```bash
curl -o family-service.json http://localhost:8082/v3/api-docs
```
