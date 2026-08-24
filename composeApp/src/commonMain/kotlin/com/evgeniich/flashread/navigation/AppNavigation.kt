package com.evgeniich.flashread.navigation

fun MutableList<AppRoute>.navigateToTopLevel(route: AppRoute) {
    require(route.isTopLevel) { "Expected a top-level route, was $route" }
    if (singleOrNull() == route) return
    clear()
    add(route)
}

fun MutableList<AppRoute>.pushIfNeeded(route: AppRoute) {
    if (lastOrNull() != route) {
        add(route)
    }
}

fun MutableList<AppRoute>.openReaderFromLibrary() {
    if (firstOrNull() != AppRoute.Library) {
        clear()
        add(AppRoute.Library)
    } else if (size > 1) {
        subList(1, size).clear()
    }
    pushIfNeeded(AppRoute.Reader)
}

fun MutableList<AppRoute>.popBack(): Boolean {
    if (size <= 1) return false
    removeLastOrNull()
    return true
}
