# JPay — поточне групове завдання

Це мінімальний REST-зріз бізнес-кейсу платіжної оркестрації з [`JPay.md`](JPay.md).
У поточній роботі реалізований сервісний шар для UC-1 (створення платежу з вибором провайдера)
та UC-4 (обробка вебхука провайдера). Retry, failover, circuit breaker, повна модель повернень,
ledger, API-key authentication, ідемпотентне сховище, реальна база даних та перевірка підпису
webhook залишені для наступних завдань.

## Модулі та контракти (Spring Modulith)

| Модуль | Що приховано (internal) | Що відкрито іншим модулям (`@NamedInterface`) |
|---|---|---|
| `common` | — | `error` — `BusinessException`, `ProblemType` |
| `payments` | `repository`, `provider`, реалізації сервісів | `domain` — `Payment`, `TransactionStatus`, `PaymentStatusChanged`; `service` — `PaymentService` |
| `webhooks` | усе | — (лише контролер, викликає `payments.service`) |
| `notifications` | усе | — (лише слухає подію `PaymentStatusChanged`) |

Реалізації (`PaymentServiceImpl`, `InMemoryPaymentRepository`, мок-клієнти провайдерів,
`MerchantNotificationListener`) — package-private класи. Інші модулі бачать і використовують
лише інтерфейси `PaymentService`, `PaymentProviderClient` тощо — залежність виключно через
контракти. Межі модулів і відсутність доступу до internal-пакетів перевіряються тестом
`ModularityTest` (`ApplicationModules.of(...).verify()`).

## REST-контракти

| Метод | Ресурс | Призначення | Успішна відповідь |
|---|---|---|---|
| `POST` | `/api/v1/payments` | Створити платіж | `201 Created` |
| `GET` | `/api/v1/payments/{paymentId}` | Отримати платіж | `200 OK` |
| `POST` | `/api/v1/providers/{providerId}/webhook-events` | Прийняти подію провайдера | `202 Accepted` |

Назви ресурсів є іменниками у множині. `POST` використовується для створення ресурсу або події,
`GET` — для читання.

## Бізнес-правила

1. **Маршрутизація за валютою та пріоритетом.** `PaymentServiceImpl` отримує впорядковану
   колекцію `List<PaymentProviderClient>` (Strategy, впроваджена Spring-ом за порядком
   `@Order`, без `@Qualifier`) і обирає перший провайдер, що підтримує валюту запиту.
   `stripe` (`@Order(1)`) обслуговує `USD`/`EUR`, `liqpay` (`@Order(2)`) — `UAH`/`USD`.
   Якщо жоден провайдер не підтримує валюту — `UnsupportedCurrencyException` (`422`).
2. **Переходи станів лише за матрицею нижче.** Будь-яка інша спроба переходу відхиляється
   як `InvalidStateTransitionException` (`409`) на рівні домену (`Payment.transitionTo`),
   до будь-якого запису в сховище.
3. **Терміновий стан незмінний.** З `FAILED` та `REFUNDED` неможливий жодний подальший перехід.
4. **Повернення лише з `SUCCEEDED`.** Вебхук `PAYMENT_REFUNDED` дозволений лише після
   `PAYMENT_SUCCEEDED`.
5. **Кожна зміна статусу публікує подію.** `PaymentServiceImpl` після успішного збереження
   публікує `PaymentStatusChanged`, яку асинхронно обробляє `notifications`-модуль.

### Матриця переходів станів

| з \ у | `PROCESSING` | `SUCCEEDED` | `FAILED` | `REFUNDED` |
|---|---|---|---|---|
| `INITIATED` | ✅ | ❌ | ❌ | ❌ |
| `PROCESSING` | ❌ | ✅ | ✅ | ❌ |
| `SUCCEEDED` | ❌ | ❌ | ❌ | ✅ |
| `FAILED` | ❌ | ❌ | ❌ | ❌ |
| `REFUNDED` | ❌ | ❌ | ❌ | ❌ |

```
INITIATED → PROCESSING → SUCCEEDED → REFUNDED
                ↓
              FAILED
```

## Типи помилок (`ProblemDetail`, `application/problem+json`)

| `type` (urn) | HTTP | Причина |
|---|---|---|
| `urn:jpay:problem:validation-failed` | 400 | Помилка Jakarta Validation |
| `urn:jpay:problem:unknown-field` | 400 | Невідоме поле в JSON |
| `urn:jpay:problem:malformed-json` | 400 | Некоректний JSON |
| `urn:jpay:problem:payment-not-found` | 404 | Платіж не знайдено |
| `urn:jpay:problem:invalid-state-transition` | 409 | Недозволений перехід стану |
| `urn:jpay:problem:unsupported-currency` | 422 | Жоден провайдер не підтримує валюту |

## Асинхронна обробка подій

`payments`-модуль публікує доменну подію `PaymentStatusChanged` через
`ApplicationEventPublisher` — без прямої залежності від `notifications`. Модуль
`notifications` підписується методом, доданим `@ApplicationModuleListener`
(`@Async` + `@Transactional(REQUIRES_NEW)` + `@TransactionalEventListener`), і лише логує
сповіщення мерчанту. Оскільки `@TransactionalEventListener` спрацьовує після коміту
транзакції, застосунок підключає `spring-boot-starter-jdbc` з вбудованою H2 — це дає
робочий `PlatformTransactionManager`, потрібний лише для того, щоб транзакція навколо
`PaymentServiceImpl.create`/`updateStatus` (обидва методи `@Transactional`) могла
комітитись і запускати слухача. Дані платежів залишаються в `InMemoryPaymentRepository`
(`ConcurrentHashMap`) — H2 тут не зберігає жодних бізнес-даних.

## Тестування

- Усі бізнес-сервіси (`PaymentServiceImplTest`) покриті швидкими Mockito-тестами —
  `@ExtendWith(MockitoExtension.class)`, сервіс створюється через `new`, без Spring-контексту.
- `TransactionStatusTest` — параметризований тест повної матриці переходів.
- `ModularityTest` — `ApplicationModules.of(JPayApplication.class).verify()`.
- Контролери (`PaymentControllerTest`, `ProviderWebhookControllerTest`) — `@WebMvcTest`
  з мокованим `PaymentService`, без повного контексту застосунку.

Для запуску потрібна Java 25 або новіша:

```bash
./mvnw clean verify
./mvnw spring-boot:run
```
