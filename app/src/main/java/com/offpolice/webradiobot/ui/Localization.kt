package com.offpolice.webradiobot.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf

enum class AppLanguageSetting {
    AUTO,
    EN,
    RU,
    UK
}

enum class AppThemeSetting {
    SYSTEM,
    LIGHT,
    DARK
}

enum class ConnectionState {
    INITIALIZING,
    LOADING_RESOURCES,
    CONNECTING_SERVER,
    GETTING_STATIONS,
    READY
}

object Loc {
    private val en = mapOf(
        "app_subtitle" to "Thousands of stations from around the world",
        "loading_tagline" to "Thousands of stations from around the world",
        "search_placeholder" to "Search stations...",
        "all_stations" to "All Radio Stations",
        "nothing_found" to "Nothing found",
        "no_title" to "No title",
        "select_station" to "Select a station",
        "playing" to "Playing",
        "buffering" to "Buffering...",
        "paused" to "Paused",
        "error" to "Error",
        "idle" to "Idle",
        "tab_radio" to "Radio",
        "tab_favorites" to "Favorites",
        "tab_settings" to "Settings",
        "empty_favorites" to "No favorite stations",
        "about_text" to "Listen to thousands of radio stations from all over the world. Add your favorites for quick access.",
        "version_info" to "Version 1.0.0 • Android App",
        "api_error" to "Network error loading stations",
        "setting_language" to "Language",
        "setting_theme" to "Theme",
        "about_heading" to "About Us",
        "language_auto" to "System",
        "language_en" to "English",
        "language_ru" to "Russian",
        "language_uk" to "Ukrainian",
        "theme_system" to "System",
        "theme_light" to "Light",
        "theme_dark" to "Dark",
        "language_default_hint" to "System default",
        "theme_default_hint" to "System default",
        "socials_heading" to "Useful Links",
        "link_dev_api" to "API for Developers!",
        "state_init" to "Initializing...",
        "state_resources" to "Loading resources...",
        "state_connecting" to "Connecting to server...",
        "state_stations" to "Fetching station list...",
        "state_ready" to "Ready!",
        "btn_cancel" to "Cancel",
        "setting_privacy_policy" to "Privacy Policy",
        "privacy_title_first_launch" to "Privacy Policy",
        "privacy_scroll_hint" to "Please scroll through the entire Privacy Policy to enable acceptance",
        "privacy_accept_btn" to "Accept",
        "privacy_decline_btn" to "Decline",
        "privacy_decline_dialog_title" to "Policy Not Accepted",
        "privacy_decline_dialog_msg" to "To use WebRadioBot, you must accept the Privacy Policy. Would you like to review it again or exit?",
        "btn_close_app" to "Exit App",
        "btn_try_again" to "Review Policy",
        "privacy_policy_content" to """
Privacy Policy

Last updated: June 13, 2026

The WebRadioBot application (hereinafter referred to as the "Application") is designed for listening to online radio stations from around the world on Android devices.

1. Data Collection

The Application does not require registration and does not collect personal data of users.

When using the Application, the following may be processed:
• list of favorite radio stations and user preferences;
• technical information necessary for streaming audio and Application operation;
• crash reports (if diagnostic tools are used).

Preferences and favorites are stored strictly locally on the user's device and are not transmitted to the developer.

2. Use of Data

Information is used solely to:
• play selected radio station audio streams;
• save the list of favorite radio stations and custom settings;
• ensure proper Application operation;
• fix bugs and improve stability.

3. Network Connection & Resources

To search for radio stations and stream audio, the Application requires Internet access.

Internet connection is used exclusively to fetch data from public radio servers (Radio Browser API) and stream audio.

4. Data Transfer to Third Parties

The Application does not sell, transfer, or provide personal user data to third parties.

5. Security

All user settings are stored locally on the device. The developer takes reasonable measures to ensure Application security.

6. Children's Privacy

The Application is not specifically intended for children under 13 years of age.

7. Policy Changes

This Privacy Policy may be updated at any time. The current version is published within the Application.

8. Contacts

For questions related to the Privacy Policy, users may contact the developer via the links in the "Settings" section.
        """.trimIndent()
    )

    private val ru = mapOf(
        "app_subtitle" to "Тысячи станций со всего мира",
        "loading_tagline" to "Тысячи станций со всего мира",
        "search_placeholder" to "Поиск станций...",
        "all_stations" to "Все радиостанции",
        "nothing_found" to "Ничего не найдено",
        "no_title" to "Без названия",
        "select_station" to "Выберите станцию",
        "playing" to "Играет",
        "buffering" to "Буфер...",
        "paused" to "Пауза",
        "error" to "Ошибка",
        "idle" to "Ожидание",
        "tab_radio" to "Радио",
        "tab_favorites" to "Избранное",
        "tab_settings" to "Настройки",
        "empty_favorites" to "Нет избранных станций",
        "about_text" to "Слушайте тысячи радиостанции со всего мира. Добавляйте любимые в избранное для быстрого доступа.",
        "version_info" to "Версия 1.0.0 • Android App",
        "api_error" to "Ошибка сети при загрузке станций",
        "setting_language" to "Язык",
        "setting_theme" to "Тема",
        "about_heading" to "О нас",
        "language_auto" to "Система",
        "language_en" to "Английский",
        "language_ru" to "Русский",
        "language_uk" to "Украинский",
        "theme_system" to "Система",
        "theme_light" to "Светлая",
        "theme_dark" to "Темная",
        "language_default_hint" to "Система по умолчанию",
        "theme_default_hint" to "система по умолчанию",
        "socials_heading" to "Полезные ссылки",
        "link_dev_api" to "API Для разработчиков!",
        "state_init" to "Инициализация...",
        "state_resources" to "Загрузка ресурсов...",
        "state_connecting" to "Подключение к серверу...",
        "state_stations" to "Получение списка станций...",
        "state_ready" to "Готово!",
        "btn_cancel" to "Отмена",
        "setting_privacy_policy" to "Политика конфиденциальности",
        "privacy_title_first_launch" to "Политика конфиденциальности",
        "privacy_scroll_hint" to "Пролистайте документ до конца, чтобы принять условия",
        "privacy_accept_btn" to "Принять",
        "privacy_decline_btn" to "Не принять",
        "privacy_decline_dialog_title" to "Политика не принята",
        "privacy_decline_dialog_msg" to "Для использования WebRadioBot необходимо принять Политику конфиденциальности. Желаете ознакомиться снова или выйти?",
        "btn_close_app" to "Выйти из приложения",
        "btn_try_again" to "Ознакомиться снова",
        "privacy_policy_content" to """
Политика конфиденциальности

Дата последнего обновления: 13 июня 2026 г.

Приложение WebRadioBot (далее — «Приложение») предназначено для прослушивания онлайн-радиостанций со всего мира на устройствах Android.

1. Сбор данных

Приложение не требует регистрации и не собирает персональные данные пользователей.

При использовании Приложения могут обрабатываться:
• список избранных радиостанций и пользовательские настройки;
• техническая информация, необходимая для воспроизведения аудиопотоков и работы Приложения;
• сведения о сбоях приложения (если используются инструменты диагностики).

Данные настроек и избранного хранятся исключительно локально на устройстве пользователя и не передаются разработчику.

2. Использование данных

Информация используется только для:
• воспроизведения выбранных аудиопотоков радиостанций;
• сохранения списка избранных радиостанций и индивидуальных настроек;
• обеспечения корректной работы Приложения;
• исправления ошибок и повышения стабильности.

3. Сетевое подключение и доступ к ресурсам

Для поиска радиостанций и воспроизведения аудиопотоков Приложению требуется доступ к сети Интернет.

Интернет-соединение используется исключительно для получения данных с публичных радиосерверов (Radio Browser API) и стриминга аудио.

4. Передача данных третьим лицам

Приложение не продает, не передает и не предоставляет персональные данные пользователей третьим лицам.

5. Безопасность

Все пользовательские настройки хранятся локально на устройстве. Разработчик принимает разумные меры для обеспечения безопасности Приложения.

6. Конфиденциальность детей

Приложение не предназначено специально для детей младше 13 лет.

7. Изменения политики

Настоящая Политика конфиденциальности может быть изменена в любое время. Актуальная версия публикуется вместе с Приложением.

8. Контакты

По вопросам, связанным с Политикой конфиденциальности, пользователь может связаться с разработчиком по ссылкам в разделе «Настройки».
        """.trimIndent()
    )

    private val uk = mapOf(
        "app_subtitle" to "Тисячі станцій з усього світу",
        "loading_tagline" to "Тисячі станцій з усього світу",
        "search_placeholder" to "Пошук станцій...",
        "all_stations" to "Всі радіостанції",
        "nothing_found" to "Нічого не знайдено",
        "no_title" to "Без назви",
        "select_station" to "Оберіть станцію",
        "playing" to "Грає",
        "buffering" to "Буфер...",
        "paused" to "Пауза",
        "error" to "Помилка",
        "idle" to "Очікування",
        "tab_radio" to "Радіо",
        "tab_favorites" to "Обране",
        "tab_settings" to "Налаштування",
        "empty_favorites" to "Немає обраних станцій",
        "about_text" to "Слухайте тисячі радіостанцій з усього світу. Додавайте улюблені в обране для швидкого доступу.",
        "version_info" to "Версія 1.0.0 • Android App",
        "api_error" to "Помилка мережі при завантаженні станцій",
        "setting_language" to "Мова",
        "setting_theme" to "Тема",
        "about_heading" to "Про нас",
        "language_auto" to "Система",
        "language_en" to "Англійська",
        "language_ru" to "Російська",
        "language_uk" to "Українська",
        "theme_system" to "Система",
        "theme_light" to "Світла",
        "theme_dark" to "Темна",
        "language_default_hint" to "Система за замовчуванням",
        "theme_default_hint" to "система за замовчуванням",
        "socials_heading" to "Корисні посилання",
        "link_dev_api" to "API Для розробників!",
        "state_init" to "Ініціалізація...",
        "state_resources" to "Завантаження ресурсів...",
        "state_connecting" to "Підключення до сервера...",
        "state_stations" to "Отримання списку станцій...",
        "state_ready" to "Готово!",
        "btn_cancel" to "Скасувати",
        "setting_privacy_policy" to "Політика конфіденційності",
        "privacy_title_first_launch" to "Політика конфіденційності",
        "privacy_scroll_hint" to "Прогортайте документ до кінця, щоб прийняти умови",
        "privacy_accept_btn" to "Прийняти",
        "privacy_decline_btn" to "Не прийняти",
        "privacy_decline_dialog_title" to "Політика не прийнята",
        "privacy_decline_dialog_msg" to "Для використання WebRadioBot необхідно прийняти Політику конфіденційності. Бажаєте ознайомитися знову чи вийти?",
        "btn_close_app" to "Вийти з додатка",
        "btn_try_again" to "Ознайомитися знову",
        "privacy_policy_content" to """
Політика конфіденційності

Дата останнього оновлення: 13 червня 2026 р.

Додаток WebRadioBot (далі — «Додаток») призначений для прослуховування онлайн-радіостанцій з усього світу на пристроях Android.

1. Збір даних

Додаток не вимагає реєстрації та не збирає персональні дані користувачів.

При використанні Додатка можуть оброблятися:
• список обраних радіостанцій та користувацькі налаштування;
• технічна інформація, необхідна для відтворення аудіопотоків та роботи Додатка;
• відомості про збої додатка (якщо використовуються інструменти діагностики).

Дані налаштувань та обраного зберігаються виключно локально на пристрої користувача і не передаються розробнику.

2. Використання даних

Інформація використовується тільки для:
• відтворення обраних аудіопотоків радіостанцій;
• збереження списку обраних радіостанцій та індивідуальних налаштувань;
• забезпечення коректної роботи Додатка;
• виправлення помилок та підвищення стабільності.

3. Мережеве підключення та доступ до ресурсів

Для пошуку радіостанцій та відтворення аудіопотоків Додатку потрібен доступ до мережі Інтернет.

Інтернет-з'єднання використовується виключно для отримання даних з публічних радіосерверів (Radio Browser API) та стрімінгу аудіо.

4. Передача даних третім особам

Додаток не продає, не передає та не надає персональні дані користувачів третім особам.

5. Безпека

Всі користувацькі налаштування зберігаються локально на пристрої. Розробник вживає розумних заходів для забезпечення безпеки Додатка.

6. Конфіденційність дітей

Додаток не призначений спеціально для дітей віком до 13 років.

7. Зміни політики

Ця Політика конфіденційності може бути змінена в будь-який час. Актуальна версія публікується разом з Додатком.

8. Контакти

З питань, пов'язаних з Політикою конфіденційності, користувач може зв'язатися з розробником за посиланнями в розділі «Налаштування».
        """.trimIndent()
    )

    fun get(key: String, setting: AppLanguageSetting): String {
        val lang = when (setting) {
            AppLanguageSetting.AUTO -> {
                val sysLang = java.util.Locale.getDefault().language
                when (sysLang) {
                    "ru" -> ru
                    "uk" -> uk
                    else -> en
                }
            }
            AppLanguageSetting.EN -> en
            AppLanguageSetting.RU -> ru
            AppLanguageSetting.UK -> uk
        }
        return lang[key] ?: en[key] ?: key
    }
}

val LocalLanguageSetting = compositionLocalOf { AppLanguageSetting.AUTO }

@Composable
@ReadOnlyComposable
fun stringLoc(key: String): String {
    return Loc.get(key, LocalLanguageSetting.current)
}
