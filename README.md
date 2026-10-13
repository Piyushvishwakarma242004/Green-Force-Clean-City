# Green Force, Clean City! — Spring Boot backend

Java 17 · Spring Boot 3.3 · Spring Data JPA (Hibernate) · Spring Security (JWT) · H2 / MySQL

The original HTML pages are in `src/main/resources/static/` and are served by Spring, so everything
works from one URL. The original design is kept; these edits were made so the project works end to end:

| File | Change |
|---|---|
| `Login.html` | **Fixed a JavaScript syntax error** (the Firebase `messagingSenderId` string was split over two lines, so the whole script never ran and Google login could not work). Added email + password login, Google login now calls the backend, "Create an Account" links to `signup.html`. |
| `home.html` | Signup form calls the backend; "Report an Issue Now" button now opens `Report.html`. |
| `Track.html` | Each of the 6 cards links to `water.html?category=<its category>`. |
| `Report.html` | Success alert shows the tracking code. |
| `Admin.html`, `contact.html` | Only a `<script src="auth.js">` line added. `Admin.html` still shows its original sample data (the navbar "Admin" link now opens `admin-panel.html`, the live version). |

All pages include `auth.js`, which keeps the login session and turns the navbar "Login" link into
"My Issues" + "Logout (name)" when logged in. Reports made while logged in are linked to the account automatically.

## Run

Requirements: JDK 17+ and Maven (or just open the folder in IntelliJ / Eclipse / VS Code as a Maven project).

```bash
mvn spring-boot:run
```

Open <http://localhost:8080> (redirects to `home.html`).
Do **not** open the HTML files directly from disk / Live Server, or `submit_issue.php` will not reach the backend.

Default database is a local H2 file (`./data`), no setup needed. Uploaded photos go to `./uploads`.

### MySQL

```bash
# create nothing by hand, the database is created automatically
export DB_USER=root DB_PASSWORD=yourpassword
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

### Before deploying, set these environment variables

| Variable | Purpose |
|---|---|
| `JWT_SECRET` | at least 32 random characters (the built-in value is for development only) |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | first admin account (default `admin@greenforce.local` / `Admin@12345`) |
| `CORS_ORIGINS` | comma separated origins, only needed if a frontend is hosted elsewhere |
| `UPLOAD_DIR` | where photos are stored |

## New pages

| URL | Purpose |
|---|---|
| `/signup.html` | Create an account (name, email, password min 8). |
| `/my-issues.html` | Issues reported while logged in, with progress timeline. |
| `/water.html?category=...` | Tracking page used by all six cards on `Track.html`: live stats, filters, search, tracking-code lookup, timeline. |
| `/admin-panel.html` | Live admin: real issue list, change status (Issued / Verified / In Progress / Solved) with a note, delete. |

## What the existing frontend already talks to

| Page | Calls | Backend |
|---|---|---|
| `Report.html` | `POST submit_issue.php` (multipart: category, name, description, image, location) | `IssueSubmissionController` |
| `Login.html` | Firebase Google sign-in | new `POST /api/auth/firebase` exchanges the Firebase ID token for our JWT |
| `home.html`, `signup.html` | sign-up form | `POST /api/auth/signup` |
| `Login.html` | email login | `POST /api/auth/login` |
| `admin-panel.html` | (new page) | `/api/admin/*` |

## API

Public (no login)

| Method | URL | Notes |
|---|---|---|
| POST | `/submit_issue.php` or `/api/issues` | multipart; returns `{id, trackingCode, ...}`; links to the user if a Bearer token is sent |
| GET | `/api/issues` | `?category=&status=&q=&page=0&size=10` (status: `ISSUED`, `VERIFIED`, `IN_PROGRESS`, `SOLVED`) |
| GET | `/api/issues/{id}` | |
| GET | `/api/issues/track/{trackingCode}` | e.g. `GF-7K2M9X` |
| GET | `/api/issues/{id}/history` | status timeline |
| GET | `/api/issues/stats` | counts per status and per category |

Logged-in user (`Authorization: Bearer <token>`)

| Method | URL |
|---|---|
| POST | `/api/auth/signup`, `/api/auth/login`, `/api/auth/firebase` |
| GET | `/api/auth/me`, `/api/issues/mine` |

Admin only

| Method | URL | Body |
|---|---|---|
| GET | `/api/admin/issues`, `/api/admin/issues/{id}` | includes reporter name / email |
| PATCH | `/api/admin/issues/{id}/status` | `{"status":"IN_PROGRESS","note":"Crew assigned"}` |
| DELETE | `/api/admin/issues/{id}` | also removes the photo |

### Quick test

```bash
# report an issue exactly like Report.html does
curl -F category=Pothole -F name=Ravi -F "description=Big pothole near market" \
     -F "location=Lat: 23.18, Lon: 79.98" -F image=@photo.jpg http://localhost:8080/submit_issue.php

# admin login, then change status
TOKEN=$(curl -s -H 'Content-Type: application/json' \
  -d '{"email":"admin@greenforce.local","password":"Admin@12345"}' \
  http://localhost:8080/api/auth/login | sed -E 's/.*"token":"([^"]+)".*/\1/')

curl -X PATCH -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"status":"IN_PROGRESS","note":"Crew assigned"}' http://localhost:8080/api/admin/issues/1/status
```

Errors are always JSON: `{ "timestamp", "status", "message", "errors"? }`.

## Project layout

```
src/main/java/com/greenforce
  config/      SecurityConfig, WebConfig, DataSeeder
  controller/  Auth, IssueSubmission, Issue (public), Admin
  service/     AuthService, IssueService, FileStorageService
  security/    JwtService, JwtAuthFilter, FirebaseTokenVerifier
  entity/      User, Issue, IssueUpdate (+ Role, IssueStatus)
  repository/  Spring Data JPA repositories
  dto/         request / response records
  exception/   custom exceptions + GlobalExceptionHandler
src/main/resources/static/   the original frontend (unchanged)
```

## Notes

* `spring.jpa.hibernate.ddl-auto=update` is convenient for development; use Flyway/Liquibase for production.
* Uploads are limited to 10 MB and to JPG / PNG / GIF / WEBP.

## If you ran an older zip before: delete the old database once

Hibernate puts a CHECK constraint on enum columns and `ddl-auto=update` does not change it, so the new
`VERIFIED` status would be rejected by an old database. Stop the app and delete the `data` folder
(H2), or for MySQL run `DROP DATABASE green_force;`, then start again. A fresh install needs nothing.

## Troubleshooting

* Errors are JSON and include the URL, e.g. `No static resource x.html. [GET /x.html]`, so a 404 tells you which URL is wrong.
* Open the site as `http://localhost:8080` (not by double-clicking the HTML files).
* `Admin.html` (original sample page) redirects to `admin-panel.html`.
* Every navbar "Login" link used to show a fake "modal would open here" alert; `auth.js` removes it.

## Important for Google login

* The machine running the backend needs internet (it downloads Google's public keys to verify the token).
* `localhost` is authorised in Firebase by default. If you open the site on another domain, add it in
  Firebase console > Authentication > Settings > Authorized domains.
