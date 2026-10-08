package com.vaultguard.app

import com.vaultguard.app.domain.model.PasswordGeneratorConfig
import com.vaultguard.app.domain.usecase.GeneratePasswordUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PasswordGeneratorTest {

    private lateinit var useCase: GeneratePasswordUseCase

    @Before
    fun setUp() {
        useCase = GeneratePasswordUseCase()
    }

    @Test
    fun testGeneratePasswordLength() {
        val config = PasswordGeneratorConfig(length = 24)
        val password = useCase(config)
        assertEquals(24, password.length)
    }

    @Test
    fun testGeneratePasswordWithUppercaseAndNumbersOnly() {
        val config = PasswordGeneratorConfig(
            length = 20,
            includeUppercase = true,
            includeLowercase = false,
            includeNumbers = true,
            includeSymbols = false
        )
        val password = useCase(config)
        assertEquals(20, password.length)
        assertTrue(password.any { it.isUpperCase() })
        assertTrue(password.any { it.isDigit() })
        assertTrue(password.none { it.isLowerCase() })
    }

    @Test
    fun testCalculateStrength() {
        val emptyStrength = useCase.calculateStrength("")
        assertEquals(0, emptyStrength.score)

        val weakStrength = useCase.calculateStrength("abc")
        assertEquals(0, weakStrength.score)

        val strongStrength = useCase.calculateStrength("K#9xP$2mQ!vL&8wZ")
        assertTrue(strongStrength.score >= 3)
    }
}
