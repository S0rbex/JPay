# JPay — поточне групове завдання

Це мінімальний REST-зріз бізнес-кейсу платіжної оркестрації з [`JPay.md`](JPay.md).
У поточній роботі реалізовані лише п’ять вимог викладача. Retry, failover, circuit breaker,
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
