package com.evgeniich.flashread.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppNavigationMotionTest {

    @Test
    fun instantNavContentTransform_skipsEnterExitAndSizeAnimation() {
        val transform = instantNavContentTransform()
        assertEquals(EnterTransition.None, transform.targetContentEnter)
        assertEquals(ExitTransition.None, transform.initialContentExit)
        assertNull(transform.sizeTransform)
    }
}
