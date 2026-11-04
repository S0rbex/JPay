# JPay: розгортання в Docker — звіт

## 1. Архітектура

```
інтернет ──> [VPS або тунель] ──> :8080 контейнер app (Spring Boot, JRE 25) ──> контейнер db (PostgreSQL 17)
                                          │                                         │
                                       volume app-logs                          volume db-data
```

- Вихід в інтернет: VPS з відкритим портом 8080 (або HTTPS-проксі перед ним). Тунель (Cloudflare Tunnel, ngrok) працює,
  лише поки увімкнений комп'ютер, тому VPS надійніший. За проксі чи тунелем задається `JPAY_FORWARD_HEADERS=framework`,
  інакше редиректи ведуть на `http`; при прямому відкритому порті лишається `none`.
- Контейнер `db` не публікує порт назовні, `app` бачить його за іменем `db` у внутрішній мережі compose.
- Контейнер `app` запускається не від root, з `read_only`, `cap_drop: ALL` і `no-new-privileges`.
- Дані Postgres і журнали лежать у іменованих томах, тому переживають перезапуск і перебудову.

## 2. Чому Docker

- Однакове середовище на ноутбуці й на сервері: потрібні лише Docker і файл `.env`, без встановлення JDK і Postgres.
- Збірка двоетапна: образ з JDK збирає jar, у фінальний образ потрапляє лише JRE і `app.jar`.
- Один командний запуск (`docker compose up -d --build`) піднімає застосунок, БД і чекає на готовність БД (`healthcheck`).
- Секрети не потрапляють в образ: паролі й ключ JWT приходять зі змінних середовища.

## 3. Файли налаштувань

| Файл | Призначення |
|---|---|
| `Dockerfile` | двоетапна збірка, користувач без прав, порт 8080 |
| `compose.yaml` | сервіси `db` і `app`, томи, healthcheck, обмеження безпеки |
| `.env.example` | шаблон змінних; справжній `.env` в git не додається (`.gitignore`, `.dockerignore`) |
| `.dockerignore` | виключає `.git`, `target`, `docs`, `.env` з контексту збірки |

Змінні: `JPAY_PORT`, `JPAY_FORWARD_HEADERS`, `JPAY_DB_NAME`, `JPAY_DB_USER`, `JPAY_DB_PASSWORD`, `JPAY_JWT_SECRET`,
`JPAY_USER_PASSWORD`, `JPAY_ADMIN_PASSWORD`. Без `JPAY_DB_PASSWORD`, `JPAY_JWT_SECRET` і паролів ролей compose не стартує.

## 4. Запуск

```
cp .env.example .env
# у .env: довгі паролі та JPAY_JWT_SECRET=$(openssl rand -hex 32)
docker compose up -d --build
```

Значення `change-me` Docker прийме без помилки, тоді адмінку відкрито всім, тому їх обов'язково замінити.

## 5. Перевірки

Результати локального запуску (`docker compose up -d --build` зі згенерованими випадковими паролями):

| Перевірка | Очікується | Результат |
|---|---|---|
| `GET /login` | 200 | 200 |
| `POST /api/v1/auth/login` з паролем із `.env` | 200 і токен | 200, токен виданий |
| `GET /api/v1/merchants` з токеном admin | 200 | 200 |
| `GET /api/v1/merchants` без токена | 401 | 401 |
| `POST /api/v1/auth/login` зі старим паролем `admin123` | 401 | 401 |
| `mvn test` у контейнері Maven | без збоїв | 96 тестів, 0 збоїв |

## 6. Зовнішнє тестування (ngrok)

Обраний спосіб виходу в інтернет: тунель ngrok (`ngrok http 8080`), публічна адреса `https://bridged-zen-remindful.ngrok-free.dev`,
`JPAY_FORWARD_HEADERS=framework`. Застосунок і Postgres працюють у контейнерах на комп'ютері, тунель вимагає, щоб він був увімкнений.

| Перевірка | Очікується | Результат |
|---|---|---|
| `GET <url>/login` | 200 | 200 |
| `POST <url>/api/v1/auth/login` | 200 і токен | 200, токен виданий |
| `GET <url>/api/v1/merchants` з токеном | 200 | 200 |
| `GET <url>/api/v1/merchants` без токена | 401 | 401 |

Знімки екрана: `screenshots/12-ngrok-tunnel.png` (тунель онлайн), `screenshots/13-login-external.png` (сторінка входу за публічною адресою).
