# JPay — поточне групове завдання

Це мінімальний REST-зріз бізнес-кейсу платіжної оркестрації з [`JPay.md`](JPay.md).
Реалізовано базовий REST API, спільний конфігураційний модуль сповіщень і Swagger UI. Retry, failover, circuit breaker,
refunds, ledger, API-key authentication, ідемпотентне сховище, база даних та перевірка підпису
webhook залишені для наступних завдань.

## REST-контракти

| Метод | Ресурс | Призначення | Успішна відповідь |
|---|---|---|---|
| `POST` | `/api/v1/payments` | Створити платіж | `201 Created` |
| `GET` | `/api/v1/payments/{paymentId}` | Отримати платіж | `200 OK` |
| `POST` | `/api/v1/providers/{providerId}/webhook-events` | Прийняти подію провайдера | `202 Accepted` |

Назви ресурсів є іменниками у множині. `POST` використовується для створення ресурсу або події,
`GET` — для читання.

## Відповідність вимогам

1. REST-контракти наведені вище та реалізовані у двох контролерах.
2. Усі вхідні й вихідні DTO у пакетах `web.dto` — Java Records.
3. Вхідні DTO перевіряються анотаціями Jakarta Validation і `@Valid`. Невідомі JSON-поля
   глобально відхиляються параметром
   `spring.jackson.deserialization.fail-on-unknown-properties=true`.
4. `GlobalExceptionHandler` повертає бізнес-помилки, помилки валідації та некоректний JSON як
   `ProblemDetail` з типом `application/problem+json`.
5. Обидва контролери покриті `@WebMvcTest`: перевіряються успішна взаємодія, валідація,
   невідомі JSON-поля та бізнес-виняток.

Для запуску потрібна Java 25 або новіша:

```bash
./mvnw clean verify
./mvnw spring-boot:run
```

## Спільний конфігураційний модуль

Пакет `ukma.jpay.shared` містить модуль внутрішніх сповіщень. Spring Boot підключає
`SharedModuleAutoConfiguration` через `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
Автоконфігурація створює `NotificationPublisher`, якщо обидва прапорці ввімкнені
та в застосунку немає власного біна цього інтерфейсу.

| Властивість | Типове значення | Призначення |
|---|---|---|
| `jpay.shared.enabled` | `true` | Увімкнути модуль |
| `jpay.shared.notifications.enabled` | `true` | Створити стандартний видавець сповіщень |
| `jpay.shared.notifications.source` | `jpay` | Назва джерела сповіщень, не може бути порожньою |

`application-dev.properties` вмикає сповіщення з джерелом `jpay-dev`.
`application-prod.properties` вимикає їх за замовчуванням і задає джерело `jpay-prod`.
Властивості профілю доповнюють `application.properties`; аргументи командного рядка
можуть перевизначити їх.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod -Dspring-boot.run.arguments=--jpay.shared.notifications.enabled=true
```

Сервіс платежів публікує `payment.created` після створення і `payment.status-changed`
після оновлення статусу. `Notification` містить джерело, тип події, UUID ресурсу та час.
Споживачі підписуються методом `@EventListener`, що приймає `Notification`.
Доставка синхронна в межах застосунку; сповіщення не зберігаються та не надсилаються назовні.
Винятки слухачів передаються виклику, який публікує подію; зміна платежу на цей момент уже виконана.
Вимкнення модуля або стандартного видавця не заважає роботі платежів.

Тести `ApplicationContextRunner` перевіряють усі комбінації прапорців, власний видавець,
ієрархічні властивості, обидва профілі та перевизначення налаштувань профілю.

## Swagger UI

Після запуску Swagger UI доступний за адресою `http://localhost:8080/swagger-ui.html`,
OpenAPI JSON — `http://localhost:8080/v3/api-docs`.
Задокументовано всі три ендпоінти, успішні відповіді, помилки `ProblemDetail`
та приклади моделей. Приклад `CreatePaymentRequest`:

```json
{"amount":19.99,"currency":"USD","merchantReference":"order-1"}
```
