---
name: api-doc
description: Generate and update OpenAPI documentation for Nomad GPS Tracker REST APIs
disable-model-invocation: true
---

# API Documentation Skill

Generate and update OpenAPI/Swagger documentation for the Nomad GPS Tracker REST endpoints.

## Usage

Invoke this skill when:
- Adding new REST endpoints
- Updating existing API documentation
- Generating OpenAPI spec files
- Creating API client libraries

## Process

1. **Scan REST Controllers**: Find all `@RestController` and `@RequestMapping` annotated classes
2. **Analyze Endpoint Patterns**: 
   - Device management (`/api/devices/**`)
   - GPS data (`/api/gps/**`, `/gps/**`)
   - Geofences (`/api/geofences/**`)
   - Fleet management (`/api/fleet/**`)
   - Commands (`/api/commands/**`)
   - Police alerts (`/api/police-alerts/**`)
3. **Document DTOs**: Include request/response models with validation rules
4. **Generate OpenAPI Spec**: Output YAML/JSON in `/docs/openapi.yaml`

## Examples

```
/api-doc
```

Will output:
- Summary of all endpoints
- Request/response schemas
- Authentication requirements
- Validation rules

## Notes

The project already uses SpringDoc OpenAPI (`springdoc-openapi-starter-webmvc-ui`).
Access at: `http://localhost:8080/swagger-ui.html`