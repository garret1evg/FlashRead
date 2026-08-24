package com.evgeniich.flashread.data.repository

import com.evgeniich.flashread.core.locale.AppLanguage
import com.evgeniich.flashread.platform.AppLanguageStorage

class AppLanguageRepository(
    private val onLoad: () -> AppLanguage = { AppLanguage.fromStorage(AppLanguageStorage.load()) },
    private val onSave: (AppLanguage) -> Unit = { AppLanguageStorage.save(it.toStorage()) },
) {
    fun save(language: AppLanguage) = onSave(language)

    fun load(): AppLanguage = onLoad()
}
