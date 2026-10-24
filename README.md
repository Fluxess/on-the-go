# on-the-go

Локальное Android-приложение: точки на карте Яндекса + GPS-оповещения (уведомление / будильник) во время поездки.

**Package / applicationId:** `ru.routealert.app` (не менять после публикации в RuStore)

Тексты для витрины: [`store/RUSTORE_LISTING.md`](store/RUSTORE_LISTING.md)

## Требования
- Android Studio Ladybug+ / JDK 17
- Android SDK 35
- Ключ [Yandex MapKit](https://developer.tech.yandex.ru/)

## Быстрый старт
1. Откройте проект в Android Studio.
2. Скопируйте `local.properties.example` → `local.properties`.
3. Укажите `sdk.dir` и `YANDEX_MAPKIT_API_KEY`.
4. Sync Gradle → Run на устройстве с GPS.

## Использование
1. Долгое нажатие на карте — новая точка.
2. Кнопка **Поиск** — адрес или координаты `45.03, 38.97`.
3. Радиус 10 м–10 км, тип алерта: уведомление / будильник / оба.
4. «Начать поездку» — в шторке уведомления live: точка и расстояние.
5. **Точки → Поделиться · Pro** — ссылка `onthego://import` и QR.

## Pro
- Шаринг точек ссылкой / QR (родители ↔ дети). Вкл/выкл в «Помощь» для теста.
- Поиск и live-уведомление доступны всем.

## Позже
- Карты Google и 2ГИС
- Импорт GPX
- Оплата Pro в сторе
