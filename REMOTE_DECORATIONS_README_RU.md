# Remote decorations (ники → символ + цвет) через GitHub JSON

## Что сделано

1) **Убрано ручное хардкод-списком** в `NameDecorations` и двух миксинах (TAB/неймтег).
   Теперь декор берётся из общего хранилища `NickDecorationsStore`.

2) Добавлен **пуллер удалённого JSON**: `RemoteNickDecorationsPoller`.
   - Раз в `remoteDecorationsIntervalSeconds` секунд (по умолчанию 60) скачивает JSON по URL.
   - Поддерживает `ETag` (If-None-Match), чтобы не качать заново, если файл не менялся.
   - Парсит JSON и обновляет `NickDecorationsStore`.

3) В `StaffHelperClient` добавлен запуск пуллера при старте клиента.

4) В `StaffHelperConfig` добавлены поля:
   - `remoteDecorationsEnabled` (bool)
   - `remoteDecorationsUrl` (строка)
   - `remoteDecorationsIntervalSeconds` (int)

5) Везде, где мод добавляет символ к никам (чат/TAB/неймтег), используется общий метод:
   - `NameDecorations.withDecorationIfTarget(nick, baseText)`

## Как использовать

1) У тебя уже есть JSON в GitHub: `staffhelper_decorations.json`.
2) Raw URL (именно raw) уже прописан по умолчанию в конфиге мода:
   - `https://raw.githubusercontent.com/DontiMonti/staffhelper-bd/refs/heads/main/staffhelper_decorations.json`
3) Открой `config/staffhelper.json` (папка `.minecraft/config/`) и пропиши:

```json
{
  "remoteDecorationsEnabled": true,
  "remoteDecorationsUrl": "https://raw.githubusercontent.com/DontiMonti/staffhelper-bd/refs/heads/main/staffhelper_decorations.json",
  "remoteDecorationsIntervalSeconds": 60
}
```

После этого мод будет **каждую минуту** подтягивать изменения и применять их без перезапуска (максимум — задержка до следующего опроса).

## Форматы JSON (2 варианта)

### Вариант A — объект `players`

```json
{
  "version": 1,
  "players": {
    "DontiMonti": { "symbol": "★", "color": "#FFD700" },
    "WerKuK":     { "symbol": "★", "color": "#FF69B4" }
  }
}
```

- Ключи в `players` — это ники.
- `symbol` — любой символ/строка (можно даже несколько символов).
- `color` — `#RRGGBB` или `RRGGBB`.

### Вариант B — массив `entries`

```json
{
  "version": 1,
  "entries": [
    { "nick": "DontiMonti", "symbol": "★", "color": "#FFD700" },
    { "nick": "WerKuK",    "symbol": "★", "color": "#FF69B4" }
  ]
}
```

## Примечания

- Ник сравнивается **без учёта регистра** (внутри всё приводится к lower-case).
- `remoteDecorationsUrl` можно не указывать: при первом запуске по умолчанию уже стоит твой raw URL.

- Если удалённый JSON недоступен/битый — декорации просто **не применяются** (локальных/офлайн-значений нет).
- Минимальный интервал опроса ограничен до **15 секунд** (защита от спама запросами).

