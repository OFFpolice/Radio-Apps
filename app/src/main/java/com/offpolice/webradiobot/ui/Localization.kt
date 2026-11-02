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
        "scroll_to_top" to "Top",
        "setting_updates" to "App Updates",
        "check_updates" to "Check for Updates",
        "checking_updates" to "Checking for updates...",
        "current_version" to "Current version",
        "latest_version_installed" to "You have the latest version",
        "new_version_available" to "Update Available!",
        "update_now" to "Update Now",
        "update_later" to "Later",
        "downloading_update" to "Downloading update...",
        "install_update" to "Install Update",
        "update_downloaded" to "Download complete",
        "update_error" to "Could not check or download update",
        "changelog_title" to "What's new:",
        "no_changelog" to "Performance improvements and bug fixes.",
        "permission_unknown_sources" to "To install the update, please allow installing apps for WebRadioBot in Settings.",
        "open_settings" to "Open Settings",
        "update_size" to "Size",
        "update_version_arrow" to "Update to",
        "no_releases_title" to "No Releases Found",
        "no_releases_desc" to "There are no published releases yet in repository %s on GitHub. To enable updates, create a Release on GitHub and attach an APK file.",
        "no_apk_title" to "No APK Attached",
        "no_apk_desc" to "Release %s was found on GitHub, but no .apk file is attached to it.",
        "repo_label" to "GitHub Repository",
        "edit_repo" to "Change Repository",
        "save" to "Save",
        "btn_close" to "Close"
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
        "about_text" to "Слушайте тысячи радиостанций со всего мира. Добавляйте любимые в избранное для быстрого доступа.",
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
        "theme_default_hint" to "Система по умолчанию",
        "socials_heading" to "Полезные ссылки",
        "link_dev_api" to "API для разработчиков!",
        "state_init" to "Инициализация...",
        "state_resources" to "Загрузка ресурсов...",
        "state_connecting" to "Подключение к серверу...",
        "state_stations" to "Получение списка станций...",
        "state_ready" to "Готово!",
        "btn_cancel" to "Отмена",
        "scroll_to_top" to "Наверх",
        "setting_updates" to "Обновление приложения",
        "check_updates" to "Проверить обновления",
        "checking_updates" to "Проверка обновлений...",
        "current_version" to "Текущая версия",
        "latest_version_installed" to "У вас установлена последняя версия",
        "new_version_available" to "Доступно обновление!",
        "update_now" to "Обновить сейчас",
        "update_later" to "Позже",
        "downloading_update" to "Загрузка обновления...",
        "install_update" to "Установить обновление",
        "update_downloaded" to "Загрузка завершена",
        "update_error" to "Не удалось проверить или скачать обновление",
        "changelog_title" to "Что нового:",
        "no_changelog" to "Улучшение стабильности и исправление ошибок.",
        "permission_unknown_sources" to "Для установки обновления разрешите установку приложений для WebRadioBot в настройках.",
        "open_settings" to "Открыть настройки",
        "update_size" to "Размер",
        "update_version_arrow" to "Обновление до",
        "no_releases_title" to "Релизы не найдены",
        "no_releases_desc" to "В репозитории %s на GitHub пока нет ни одного опубликованного релиза. Чтобы выпустить обновление, создайте Release на GitHub и прикрепите APK-файл.",
        "no_apk_title" to "В релизе нет APK",
        "no_apk_desc" to "Найден релиз %s на GitHub, но к нему не прикреплён установочный файл .apk.",
        "repo_label" to "Репозиторий GitHub",
        "edit_repo" to "Сменить репозиторий",
        "save" to "Сохранить",
        "btn_close" to "Закрыть"
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
        "theme_default_hint" to "Система за замовчуванням",
        "socials_heading" to "Корисні посилання",
        "link_dev_api" to "API для розробників!",
        "state_init" to "Ініціалізація...",
        "state_resources" to "Завантаження ресурсів...",
        "state_connecting" to "Підключення до сервера...",
        "state_stations" to "Отримання списку станцій...",
        "state_ready" to "Готово!",
        "btn_cancel" to "Скасувати",
        "scroll_to_top" to "Догори",
        "setting_updates" to "Оновлення додатку",
        "check_updates" to "Перевірити оновлення",
        "checking_updates" to "Перевірка оновлень...",
        "current_version" to "Поточна версія",
        "latest_version_installed" to "У вас встановлена остання версія",
        "new_version_available" to "Доступне оновлення!",
        "update_now" to "Оновити зараз",
        "update_later" to "Пізніше",
        "downloading_update" to "Завантаження оновлення...",
        "install_update" to "Встановити оновлення",
        "update_downloaded" to "Завантаження завершено",
        "update_error" to "Не вдалося перевірити або завантажити оновлення",
        "changelog_title" to "Що нового:",
        "no_changelog" to "Покращення стабільності та виправлення помилок.",
        "permission_unknown_sources" to "Для встановлення оновлення дозвольте встановлення додатків для WebRadioBot у налаштуваннях.",
        "open_settings" to "Відкрити налаштування",
        "update_size" to "Розмір",
        "update_version_arrow" to "Оновлення до",
        "no_releases_title" to "Релізи не знайдено",
        "no_releases_desc" to "У репозиторії %s на GitHub ще немає жодного опублікованого релізу. Щоб випустити оновлення, створіть Release на GitHub та прикріпіть APK-файл.",
        "no_apk_title" to "У релізі немає APK",
        "no_apk_desc" to "Знайдено реліз %s на GitHub, але до нього не прикріплено інсталяційний файл .apk.",
        "repo_label" to "Репозиторій GitHub",
        "edit_repo" to "Змінити репозиторій",
        "save" to "Зберегти",
        "btn_close" to "Закрити"
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
