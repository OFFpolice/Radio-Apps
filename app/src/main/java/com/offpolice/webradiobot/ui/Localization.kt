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
        "setting_animations" to "Animations",
        "setting_vibration" to "Vibration",
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
        "btn_cancel" to "Cancel"
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
        "setting_animations" to "Анимации",
        "setting_vibration" to "Вибрация",
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
        "btn_cancel" to "Отмена"
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
        "setting_animations" to "Анімації",
        "setting_vibration" to "Вібрація",
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
        "btn_cancel" to "Скасувати"
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
