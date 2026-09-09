package com.evgeniich.flashread.data.repository

import com.evgeniich.flashread.core.theme.AppTheme
import com.evgeniich.flashread.platform.AppThemeStorage

class AppThemeRepository(
    private val onLoad: () -> AppTheme = { AppTheme.fromStorage(AppThemeStorage.load()) },
    private val onSave: (AppTheme) -> Unit = { AppThemeStorage.save(it.toStorage()) },
) {
    fun save(theme: AppTheme) = onSave(theme)

    fun load(): AppTheme = onLoad()
}
