package com.goodusestudios.shell

import com.goodusestudios.shell.ui.NavigationMode
import com.goodusestudios.shell.ui.navigationModeForWidth
import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveShellTest {
    @Test fun compactWindowUsesBottomBar() =
        assertEquals(NavigationMode.BottomBar, navigationModeForWidth(599))

    @Test fun mediumWindowUsesRail() =
        assertEquals(NavigationMode.Rail, navigationModeForWidth(600))

    @Test fun expandedWindowUsesSidebar() =
        assertEquals(NavigationMode.Sidebar, navigationModeForWidth(840))
}
