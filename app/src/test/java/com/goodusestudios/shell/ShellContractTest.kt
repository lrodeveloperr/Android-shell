package com.goodusestudios.shell

import com.goodusestudios.shell.data.ShellGate
import com.goodusestudios.shell.data.resolveShellGate
import com.goodusestudios.shell.ui.ShellConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellContractTest {
    @Test fun shippedTemplateConfigurationIsInternallyValid() =
        assertTrue(ShellConfig.validationErrors().joinToString(), ShellConfig.validationErrors().isEmpty())

    @Test fun firstRunRequiresFullOnboarding() =
        assertEquals(ShellGate.FullOnboarding, resolveShellGate(false, null, 1))

    @Test fun legalVersionChangeRequiresOnlyLegalUpdate() =
        assertEquals(ShellGate.LegalUpdate, resolveShellGate(true, 1, 2))

    @Test fun currentAcceptanceOpensTheApp() =
        assertEquals(ShellGate.Ready, resolveShellGate(true, 2, 2))
}
