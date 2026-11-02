package com.shelflife.ai.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.shelflife.ai.domain.model.Category
import com.shelflife.ai.domain.model.ExpiryUrgency
import com.shelflife.ai.domain.model.PantryItem
import com.shelflife.ai.domain.repository.PantryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExpiryNotificationWorkerTest {

    private lateinit var context: Context
    private lateinit var repository: PantryRepository
    private lateinit var workerParams: WorkerParameters

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = mockk(relaxed = true)
        workerParams = mockk(relaxed = true)
    }

    @Test
    fun doWork_withExpiringItems_queriesRepositoryAndReturnsSuccess() = runBlocking {
        val now = System.currentTimeMillis()
        val expiringItem1 = PantryItem(
            id = 1L,
            name = "Fresh Milk",
            expiryTimestamp = now + (12 * 3600 * 1000L),
            category = Category.DAIRY,
            quantity = 2
        )
        val expiringItem2 = PantryItem(
            id = 2L,
            name = "Greek Yogurt",
            expiryTimestamp = now + (36 * 3600 * 1000L),
            category = Category.DAIRY,
            quantity = 1
        )

        coEvery { repository.getItemsExpiringWithinSync(any()) } returns listOf(expiringItem1, expiringItem2)

        val worker = ExpiryNotificationWorker(context, workerParams, repository)
        val result = worker.doWork()

        assertEquals(Result.success(), result)
        coVerify(atLeast = 1) {
            repository.getItemsExpiringWithinSync(match { it >= now + ExpiryUrgency.FORTY_EIGHT_HOURS_MILLIS })
        }
    }

    @Test
    fun doWork_withNoExpiringItems_returnsSuccessWithoutErrors() = runBlocking {
        coEvery { repository.getItemsExpiringWithinSync(any()) } returns emptyList()

        val worker = ExpiryNotificationWorker(context, workerParams, repository)
        val result = worker.doWork()

        assertEquals(Result.success(), result)
        coVerify(atLeast = 1) { repository.getItemsExpiringWithinSync(any()) }
    }

    @Test
    fun doWork_ignoresOutOfStockItems() = runBlocking {
        val now = System.currentTimeMillis()
        val outOfStockItem = PantryItem(
            id = 3L,
            name = "Empty Bagel",
            expiryTimestamp = now + (10 * 3600 * 1000L),
            category = Category.BAKERY,
            quantity = 0
        )

        coEvery { repository.getItemsExpiringWithinSync(any()) } returns listOf(outOfStockItem)

        val worker = ExpiryNotificationWorker(context, workerParams, repository)
        val result = worker.doWork()

        assertEquals(Result.success(), result)
    }

    @Test
    fun doWork_whenRepositoryThrowsException_retriesWhenAttemptsRemaining() = runBlocking {
        coEvery { repository.getItemsExpiringWithinSync(any()) } throws RuntimeException("Database error")
        coEvery { workerParams.runAttemptCount } returns 0

        val worker = ExpiryNotificationWorker(context, workerParams, repository)
        val result = worker.doWork()

        assertEquals(Result.retry(), result)
    }
}
