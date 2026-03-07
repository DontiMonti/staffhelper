# Перенос StaffHelper на Supabase (free plan)

## 1) Создай таблицы (SQL Editor)

```sql
create table if not exists public.staffhelper_decorations (
  nick text primary key,
  symbol text not null default '*',
  color text not null default '#FFFFFF',
  active boolean not null default true,
  updated_at timestamptz not null default now()
);

create table if not exists public.staffhelper_roles (
  nick text primary key,
  role text not null,
  active boolean not null default true,
  updated_at timestamptz not null default now()
);

create table if not exists public.staffhelper_updates (
  id int primary key default 1 check (id = 1),
  latest_version text not null,
  updated_at timestamptz not null default now()
);

insert into public.staffhelper_updates (id, latest_version)
values (1, '2.0.0')
on conflict (id) do nothing;
```

## 2) Включи RLS и чтение для anon

```sql
alter table public.staffhelper_decorations enable row level security;
alter table public.staffhelper_roles enable row level security;
alter table public.staffhelper_updates enable row level security;

drop policy if exists decor_read_anon on public.staffhelper_decorations;
create policy decor_read_anon
on public.staffhelper_decorations
for select
to anon
using (true);

drop policy if exists roles_read_anon on public.staffhelper_roles;
create policy roles_read_anon
on public.staffhelper_roles
for select
to anon
using (true);

drop policy if exists updates_read_anon on public.staffhelper_updates;
create policy updates_read_anon
on public.staffhelper_updates
for select
to anon
using (true);
```

## 3) Где взять ключи

- `Project URL`: `Project Settings -> Data API -> Project URL`
- `Anon key`: `Project Settings -> Data API -> anon public`
- `Write key` (для меню создателя): лучше использовать `service_role` только на твоём ПК, не в публичной сборке.

## 4) Как заполнить в моде

Открой `Ctrl+Alt+F6` (creator menu), вкладка `Settings`:

- `Project URL`
- `Anon Key`
- `Write Key` (опционально)
- Таблицы:
  - `staffhelper_decorations`
  - `staffhelper_roles`
  - `staffhelper_updates`
- `Creator UUID` (рекомендуется заполнить)

Нажми `Save Settings` и `Test Read`.

После `Save Settings` ключи в `config/staffhelper.json` сохраняются в зашифрованном виде (`enc:v1`, AES-GCM), а не как открытый текст.

## 5) Управление из меню

- `Decorations`: upsert/delete декорации
- `Roles`: upsert/delete роли
- `Version`: обновить `latest_version`
- `Force Sync`: принудительно подтянуть данные в клиент

## Безопасность

- Если в моде сохранён `service_role`, любой, кто получит этот конфиг/мод, сможет писать в БД.
- Для публичных сборок держи `service_role` только в своей локальной среде.
