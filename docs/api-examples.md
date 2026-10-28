# REST API and local authentication

JSON responses use records rather than exposing JPA entities. API Basic authentication is for local demonstration. HTTPS and further hardening are required before internet use. Browser and API mutation requests both require CSRF; credentials alone are insufficient.

## PowerShell example (Windows PowerShell 5.1 or PowerShell 7)

```powershell
$base = 'http://localhost:18080'
$pair = 'customer1:DemoRide21!'
$headers = @{ Authorization = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pair)) }
$csrf = Invoke-RestMethod "$base/api/csrf" -Headers $headers -SessionVariable session
$headers[$csrf.headerName] = $csrf.token
$bike = Invoke-RestMethod "$base/api/motorcycles" -Method Post -Headers $headers -WebSession $session -ContentType 'application/json' -Body (@{registration='DEMO900';model='Fictional Tourer';note='Synthetic data'} | ConvertTo-Json)
$slots = Invoke-RestMethod "$base/api/slots" -Headers $headers -WebSession $session
$slot = $slots | Where-Object { $_.available -gt 0 } | Select-Object -First 1
$booking = Invoke-RestMethod "$base/api/bookings" -Method Post -Headers $headers -WebSession $session -ContentType 'application/json' -Body (@{motorcycleId=$bike.id;slotId=$slot.id;serviceType='REGULAR_SERVICE';note='Check brakes'} | ConvertTo-Json)
$booking
Invoke-RestMethod "$base/api/bookings/$($booking.id)/cancel" -Method Post -Headers $headers -WebSession $session
```

Select a **full** slot for waitlist requests, with the same booking body:

```json
{"motorcycleId":3,"slotId":1,"serviceType":"RUNNING_REPAIR","note":"Inspect vibration"}
```

IDs are illustrative; retrieve your own motorcycle and slot IDs. Repeating the registration example will correctly return a conflict.

| Method/path | Role | Success | Notes |
|---|---|---|---|
| GET /api/csrf | Authenticated | 200 | Retain cookie; use returned header name/token |
| GET /api/centers | Authenticated | 200 | Fictional centers |
| GET /api/slots?date=YYYY-MM-DD | Authenticated | 200 | Optional IST calendar date |
| GET /api/slots/{id} | Authenticated | 200 | Capacity and available count |
| POST /api/motorcycles | Customer | 201 | Registration/model/note |
| GET /api/motorcycles | Authenticated | 200 | Own garage |
| POST /api/bookings | Customer | 201 | Full/duplicate → 409 |
| GET /api/bookings | Authenticated | 200 | Own records; advisor sees workshop |
| POST /api/bookings/{id}/cancel | Owner/customer | 200 | Future confirmed only |
| POST /api/waitlist | Customer | 201 | Full slot only |
| GET /api/waitlist | Authenticated | 200 | Own queue history; advisor sees all |
| POST /api/waitlist/{id}/withdraw | Owner/customer | 200 | WAITING only |
| PATCH /api/advisor/bookings/{id}/status | Advisor | 200 | `{"status":"IN_SERVICE"}` then COMPLETED |

An advisor API client authenticates as advisor, fetches its own CSRF token/session and sends the PATCH using that session. Never reuse another account's CSRF/session pair.

Illustrative booking response (not execution evidence):

```json
{"id":1,"reference":"MSL-1","motorcycleId":1,"slotId":1,"serviceType":"REGULAR_SERVICE","note":null,"status":"CONFIRMED","history":[{"status":"CONFIRMED","occurredAt":"2030-01-01T10:00:00Z","detail":"Booking confirmed"}]}
```

Expected error contract:

```json
{"status":409,"message":"Slot is full; join the waitlist","path":"/api/bookings"}
```

400 = invalid input/type/length; 401 = missing or invalid credentials; 403 = forbidden or invalid CSRF; 404 = missing resource; 409 = duplicate/full/state/locking conflict. Spring Security may reject an unauthenticated mutation for missing CSRF before checking credentials. Do not interpret that 403 as successful authentication. Server-side exceptions are not exposed to clients. OpenAPI UI is deliberately omitted to keep dependencies minimal; this endpoint contract is the maintained API reference.
