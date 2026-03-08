# StaffHelper

Клиентский Fabric-мод для Minecraft 1.21.8, который ускоряет работу стаффа: создание алиасов, HUD, AFK-Zone, визуальная часть для стафа.

## Что умеет мод:

### 1. Единое меню управления
- Открытие меню по `Right Shift`.
- Вкладки: `NickSearch`, `AFK Zone`, `CommandBuilder`, `Modules`, `Appearance`.
- Встроенный HUD Editor: перетаскивание и масштабирование виджетов.
  [Main Menu](src/main/resources/assets/main_menu.png)

### 2. NickSearch (поиск ников в онлайне)
- Отслеживает ники по шаблонам.
- Поиск по добавленным шаблонам.
- Показывает совпадения в отдельном HUD-виджете.
  [NickSearch Options](src/main/resources/assets/main_menu.png)
  [NickSearch example](src/main/resources/assets/ns-example.png)

### 3. AFK Zone + Multi-account check
- Настройка зоны по двум точкам (`POS1`/`POS2`).
- Отрисовка зоны для проверки размещения (outline/fill).
- Анализ игроков в выделенной зоне используя команду `/alts`.
- Отдельный HUD со списком нарушителей и бан-инфо(После выдачи наказания мод подтягивает причину и ID-бана).
- Бинд для копирования готового текста из виджета.
- Игнор-лист ников для исключений.
  [afk-zone options](src/main/resources/assets/afk_zone.png)
  [afk-zone example](src/main/resources/assets/ma-example.png)

### 4. Command Builder (алиасы команд)
- Создание своих шаблонов команд: `alias -> execute`.
- Плейсхолдеры: `{playername}`, `{time}`, `{reason}`.
- Автоподсказки в чате + поддержка `Tab`-дополнения.
- Работает и с ванильным чат-суггестором, и с AdvancedChatBox.
  [CommandBuilder](src/main/resources/assets/comm_build.png)

### 5. Modules
- STATS HUD
- Отображение: ник, роль, пинг, TPS (`now`, `5m`, `10m`, `15m`).
- Горизонтальный/вертикальный режим.
- Цветовая индикация пинга и TPS.

#####
- AutoBox Beta
- После входа на сервер может автоматически отправлять выбранную команду (Box#1 / Box#2).
  [Modules](src/main/resources/assets/modules.png)

### 6. Appearance
- Возможность перемещать виджеты.
- Возможность отключить анимацию блеска.
- Пресеты цветовых тем, а так-же кастомная тема из 2ух цветов.
  [Appearance](src/main/resources/assets/appear.png)

### 7. Vanish-индикатор
- Автоматически определяет включение/выключение vanish по сообщениям сервера.
- Показывает отдельный HUD-индикатор, пока vanish активен.
  [hud](src/main/resources/assets/hud.png)

### 8. Роли и декорации ников (удалённая синхронизация)
- Подтягивает роли и декорации (символ/цвет) из Supabase.
- Применяет декорации в
  - чате;[chat example](src/main/resources/assets/deco_chat_example.png)
  - TAB;[tab example](src/main/resources/assets/deco_tab_example.png)
  - отдельном неймтеге над игроками.[nametag example](src/main/resources/assets/deco_nametag_example.png)

### 9. Проверка обновлений
- При входе проверяет актуальность версии.
- Показывает уведомление, если нужно обновиться.

### 10. Контроль доступа
- Доступ к функциям мода ограничен таблицей `allowed_users` в Supabase.
- Если игрок не в списке — мод показывает уведомление и отключает рабочие данные.



### 11. Работа с базой данных

#### 11.1 Инициализация и конфиг
- Берутся `supabaseProjectUrl`, `supabaseAnonKey`, имена таблиц из конфига: [StaffHelperConfig.java:90](https://github.com/DontiMonti/staffhelper/blob/71c7fd3ebbfa3a29c67d77b5860ababfe4e8d603/src/main/java/com/dmsh/staffhelper/config/StaffHelperConfig.java#L90).
- Базовый URL формируется как `https://<project>.supabase.co/rest/v1/<table>`: [SupabaseApi.java:139](https://github.com/DontiMonti/staffhelper/blob/71c7fd3ebbfa3a29c67d77b5860ababfe4e8d603/src/client/java/com/dmsh/staffhelper/util/SupabaseApi.java#L139).

#### 11.2 Принятие запросов
- Каждый read-запрос отправляет заголовки:
  - `apikey: <anon key>`
  - `Authorization: Bearer <anon key>`
  - `Accept: application/json`

#### 11.3 Что берется из бд
  - roles: [RemoteRolesPoller.java:134](https://github.com/DontiMonti/staffhelper/blob/71c7fd3ebbfa3a29c67d77b5860ababfe4e8d603/src/client/java/com/dmsh/staffhelper/util/RemoteRolesPoller.java#L134)
  - decorations: [RemoteNickDecorationsPoller.java:140](https://github.com/DontiMonti/staffhelper/blob/71c7fd3ebbfa3a29c67d77b5860ababfe4e8d603/src/client/java/com/dmsh/staffhelper/util/RemoteNickDecorationsPoller.java#L140)
  - updates: [UpdateNotifyFeature.java:101](https://github.com/DontiMonti/staffhelper/blob/71c7fd3ebbfa3a29c67d77b5860ababfe4e8d603/src/client/java/com/dmsh/staffhelper/feature/UpdateNotifyFeature.java#L101)
  - allowed_users: [AllowedUsersAccessGate.java:105](https://github.com/DontiMonti/staffhelper/blob/71c7fd3ebbfa3a29c67d77b5860ababfe4e8d603/src/client/java/com/dmsh/staffhelper/util/AllowedUsersAccessGate.java#L105)

#### 11.4 Обмен информацией
  - Переодический `GET` запрос.
  - `get` запускается при старте клиента: [StaffHelperClient.java:36](https://github.com/DontiMonti/staffhelper/blob/71c7fd3ebbfa3a29c67d77b5860ababfe4e8d603/src/client/java/com/dmsh/staffhelper/StaffHelperClient.java#L36)
  - Переодический запрос `get` 10-15сек: [StaffHelperConfig.java:197](https://github.com/DontiMonti/staffhelper/blob/71c7fd3ebbfa3a29c67d77b5860ababfe4e8d603/src/main/java/com/dmsh/staffhelper/config/StaffHelperConfig.java#L197)

#### 11.5 Хранение ключей
  - Ключи при сохранении конфига шифруются локально и пишутся в `...Encrypted` поля: [StaffHelperConfig.java:119](https://github.com/DontiMonti/staffhelper/blob/71c7fd3ebbfa3a29c67d77b5860ababfe4e8d603/src/main/java/com/dmsh/staffhelper/config/StaffHelperConfig.java#L119), [ConfigSecretCodec.java:33](https://github.com/DontiMonti/staffhelper/blob/71c7fd3ebbfa3a29c67d77b5860ababfe4e8d603/src/main/java/com/dmsh/staffhelper/config/ConfigSecretCodec.java#L33).

## Горячие клавиши по умолчанию
Все клавиши настраиваются через ванильное меню майнкрафта.

| Клавиша | Действие |
|---|---|
| `Right Shift` | Открыть меню StaffHelper |
| `P` | Скопировать и очистить AFK-список |
| `O` | Запустить массовую `/alts`-проверку по AFK Zone |

## Пример Command Builder

```text
Name: 1.6
Alias: /1.6 {playername} {time} {reason}
Execute: /ban {playername} {time} {reason}
Time options: 12h, 1d, 2d
Reason options: 1.6, 1.6(3+ Аккаунта на одном IP)
```

Теперь ввод `/1.6 Aggnesr 12h 1.6` автоматически превратится в нужную команду наказания.
```text
Alias: /3.1 {playername}
Execute: /ban {playername} 7d 3.1
```

Аналогично ввод `/3.1 Aggnesr` приведет к вызову команды из `execute`
## Совместимость

- Minecraft: `1.21.8`
- Java: `21+`
- Fabric Loader: `0.18.4+`
- Fabric API: `0.136.1+1.21.8`

## Конфиг

Файл настроек создаётся автоматически:

```text
.minecraft/config/staffhelper.json
```

## Автор

- `DontiMonti`
