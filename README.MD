# StaffHelper

Клиентский Fabric-мод для Minecraft 1.21.8, который ускоряет работу стаффа: создание алиасов, HUD, AFK-Zone, визуальная часть для стафа.

## Что умеет мод

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
