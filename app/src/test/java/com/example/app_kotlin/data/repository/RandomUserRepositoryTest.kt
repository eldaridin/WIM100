package com.example.app_kotlin.data.repository

import com.example.app_kotlin.data.remote.RandomName
import com.example.app_kotlin.data.remote.RandomUser
import com.example.app_kotlin.data.remote.RandomUserApi
import com.example.app_kotlin.data.remote.RandomUserResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RandomUserRepositoryTest {

    private val api = mockk<RandomUserApi>()
    private val repo = RandomUserRepository(api)

    @Test
    fun `nombreFicticio une nombre y apellido de la API externa`() = runTest {
        coEvery { api.generar(any(), any(), any()) } returns
            RandomUserResponse(listOf(RandomUser(RandomName(first = "Lucía", last = "Gómez"))))

        assertEquals("Lucía Gómez", repo.nombreFicticio())
    }
}
