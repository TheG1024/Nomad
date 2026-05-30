---
name: pr-check
description: Run standardized code review checklist for Nomad GPS Tracker pull requests
disable-model-invocation: true
---

# PR Check Skill

Run a standardized code review checklist for Nomad GPS Tracker pull requests.

## Usage

Invoke before submitting or reviewing PRs:
```
/pr-check
```

## Checklist

### 1. Code Quality
- [ ] No commented-out code
- [ ] No TODO/FIXME without tracking issue
- [ ] Consistent naming conventions
- [ ] Proper logging (no System.out.println)

### 2. GPS/Tracking Logic
- [ ] Device ID validation
- [ ] GPS coordinate bounds checking (lat: -90 to 90, lng: -180 to 180)
- [ ] Timestamp validity checks
- [ ] Null safety for optional fields (battery, speed, direction)

### 3. Security
- [ ] No hardcoded credentials
- [ ] API endpoints follow security config patterns
- [ ] WebSocket messages validated
- [ ] Input sanitization for user data

### 4. API Design
- [ ] RESTful conventions followed
- [ ] Proper HTTP status codes
- [ ] DTOs used instead of entities in responses
- [ ] OpenAPI annotations for new endpoints

### 5. Testing
- [ ] Unit tests for service layer
- [ ] Controller tests for endpoints
- [ ] Integration test for critical flows

### 6. Performance
- [ ] Redis caching where appropriate
- [ ] No N+1 queries
- [ ] WebSocket message batching

## Output

Generate a markdown report with:
- Files changed summary
- Checklist results
- Security concerns
- Performance suggestions
- Test coverage notes