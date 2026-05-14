# Servera — Удалённое управление серверами

Android-приложение для управления SSH-серверами. Позволяет выполнять команды, просматривать системную информацию и историю команд, используя безопасное зашифрованное хранение учётных данных.

---

## Содержание

- [Технологии](#технологии)
- [Архитектура](#архитектура)
- [Функциональность](#функциональность)
- [Структура проекта](#структура-проекта)
- [Навигация](#навигация)
- [Безопасность](#безопасность)
- [Цветовая схема](#цветовая-схема)
- [База данных](#база-данных)
- [Сборка и запуск](#сборка-и-запуск)

---

## Технологии

| Категория | Библиотека / Инструмент |
|---|---|
| UI | Jetpack Compose + Material 3 |
| DI | Dagger Hilt 2.51.1 |
| База данных | Room 2.6.1 |
| SSH | JSch |
| Навигация | Navigation Compose |
| Асинхронность | Coroutines + Flow |
| Безопасность | Android Keystore, EncryptedSharedPreferences |
| Биометрия | BiometricPrompt API |
| Компилятор аннотаций | KSP |
| Язык | Kotlin 2.0.21 |

**Версии SDK:** minSdk 26 · targetSdk 35 · compileSdk 36

---

## Архитектура

Приложение построено по принципу **Clean Architecture** с тремя слоями и паттерном **MVVM** на уровне представления.

```
presentation/   ← Compose UI + ViewModels (Hilt)
domain/         ← Entities, Use Cases, Repository interfaces (чистый Kotlin)
data/           ← Room, JSch SSH, Keystore, Impl репозиториев
di/             ← Hilt-модули
```

**Правила слоёв:**
- `domain` не зависит от Android-фреймворка и Room
- `data` реализует интерфейсы из `domain`
- `presentation` взаимодействует только с Use Cases через ViewModels

---

## Функциональность

### Аутентификация
- Вход по паролю (хэш SHA-256 в EncryptedSharedPreferences)
- Биометрическая аутентификация (отпечаток пальца / Face ID)
- Первичная установка пароля при первом запуске

### Управление серверами
- Добавление, редактирование, удаление SSH-серверов
- Настройка: хост, порт, логин, пароль / приватный ключ, таймаут
- Проверка статуса (онлайн / офлайн) при загрузке списка

### SSH-консоль
- Выполнение произвольных команд
- Отображение stdout, stderr, кода выхода, времени выполнения
- Быстрые команды (SuggestionChip)
- Копирование результата в буфер обмена
- Повтор последней команды

### Информация о сервере
- Имя хоста, ОС, CPU, RAM (всего / свободно), диск, аптайм
- Отдельная вкладка в консоли

### История команд
- Общая история по всем серверам
- Фильтрация по конкретному серверу
- Очистка истории

### Настройки
- Включение / отключение биометрии
- Информация о приложении

---

## Структура проекта

```
com.tivanstudio.servera
├── ServeraApplication.kt
├── MainActivity.kt
│
├── presentation/
│   ├── theme/           Color · Typography · Shape · Theme
│   ├── navigation/      Screen · AppNavGraph
│   ├── components/      AppBottomBar
│   ├── auth/            Login · CreatePassword
│   ├── servers/
│   │   ├── list/        ServerList
│   │   └── add/         AddServer
│   ├── console/
│   │   ├── ConsoleScreen (вкладки: Консоль + Инфо)
│   │   ├── execute/     ExecuteCommand
│   │   └── result/      CommandResult
│   ├── history/         History
│   └── settings/        Settings
│
├── domain/
│   ├── entity/          Server · CommandResult · CommandHistory · QuickCommand · ServerInfo
│   ├── repository/      ServerRepository · CommandHistoryRepository · QuickCommandRepository
│   │                    AuthRepository · SshClient (interfaces)
│   └── usecase/
│       ├── auth/        SetPassword · VerifyPassword · IsPasswordSet · IsBiometricEnabled
│       ├── server/      GetServers · AddServer · UpdateServer · DeleteServer · CheckServerStatus
│       ├── ssh/         ExecuteCommand · TestConnection · FetchServerInfo
│       ├── history/     GetCommandHistory · ClearHistory
│       └── quickcommand/ GetQuickCommands · SaveQuickCommand
│
├── data/
│   ├── db/
│   │   ├── entity/      ServerEntity · CommandHistoryEntity · QuickCommandEntity
│   │   ├── dao/         ServerDao · CommandHistoryDao · QuickCommandDao
│   │   ├── AppDatabase.kt
│   │   └── mapper/      ServerMapper · CommandHistoryMapper · QuickCommandMapper
│   ├── crypto/          KeystoreManager · EncryptionHelper
│   ├── ssh/             SshClientImpl
│   └── repository/      ServerRepositoryImpl · CommandHistoryRepositoryImpl
│                        QuickCommandRepositoryImpl · AuthRepositoryImpl
│
└── di/
    ├── DatabaseModule.kt
    ├── RepositoryModule.kt
    ├── SshModule.kt
    └── CommandResultHolder.kt   (Singleton для передачи результата между экранами)
```

---

## Навигация

```
Login ──────────────────────────────────────────────────────────┐
  └─► CreatePassword (первый запуск)                            │
                                                                 ▼
BottomBar: [ Серверы ] ──── [ История ] ──── [ Настройки ]

Серверы
  └─► AddServer (добавление / редактирование)
  └─► Console
        ├─ Вкладка Консоль ─► ExecuteCommand ─► Result
        └─ Вкладка Инфо
```

---

## Безопасность

| Механизм | Описание |
|---|---|
| Пароль приложения | SHA-256 хэш, хранится в EncryptedSharedPreferences (AES-256-SIV) |
| Учётные данные серверов | AES-256-GCM через Android Keystore, IV (12 байт) + шифртекст в Base64 |
| Биометрия | BiometricPrompt API, требует FragmentActivity |
| Защита экрана | `FLAG_SECURE` — блокирует скриншоты и запись экрана |

---

## Цветовая схема

| Токен | HEX | Применение |
|---|---|---|
| Background | `#0F172A` | Фон экранов |
| Surface | `#1E293B` | Карточки, нижняя панель |
| Elevated | `#334155` | Поля ввода |
| PrimaryGreen | `#22C55E` | Кнопки, успех |
| InfoBlue | `#3B8DF8` | Иконки информации |
| DangerRed | `#EF4444` | Удаление, ошибки |
| TextPrimary | `#F8FAFC` | Основной текст |
| TextSecondary | `#94A3B8` | Подписи, подсказки |

---

## База данных

Room БД версии 1, три таблицы:

| Таблица | Поля |
|---|---|
| `servers` | id, name, host, port, login, encryptedPassword, encryptedPrivateKey, timeout, createdAt |
| `command_history` | id, serverId, command, stdout, stderr, exitCode, executedAt |
| `quick_commands` | id, label, command, sortOrder |

---

## Сборка и запуск

**Требования:**
- Android Studio Hedgehog или новее
- JDK 11+
- Android SDK с API 26+

**Шаги:**
```bash
git clone <repo-url>
# Открыть папку Servera/ в Android Studio
# Sync Gradle → Run on device/emulator
```

**Разрешения в манифесте:**
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
<uses-permission android:name="android.permission.USE_FINGERPRINT" />
```
