package com.shelflife.ai.data.repository

import com.shelflife.ai.data.local.PantryDao
import com.shelflife.ai.data.local.toDomain
import com.shelflife.ai.data.local.toDomainList
import com.shelflife.ai.data.local.toEntity
import com.shelflife.ai.data.local.toEntityList
import com.shelflife.ai.di.IoDispatcher
import com.shelflife.ai.domain.model.Category
import com.shelflife.ai.domain.model.PantryItem
import com.shelflife.ai.domain.repository.PantryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PantryRepositoryImpl @Inject constructor(
    private val pantryDao: PantryDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PantryRepository {

    override fun getAllItems(): Flow<List<PantryItem>> =
        pantryDao.getAllItems()
            .map { it.toDomainList() }
            .flowOn(ioDispatcher)

    override fun getItemsByUrgencyThreshold(thresholdTimestamp: Long): Flow<List<PantryItem>> =
        pantryDao.getItemsByUrgencyThreshold(thresholdTimestamp)
            .map { it.toDomainList() }
            .flowOn(ioDispatcher)

    override suspend fun getItemsExpiringBeforeSync(thresholdTimestamp: Long): List<PantryItem> =
        withContext(ioDispatcher) {
            pantryDao.getItemsExpiringBeforeSync(thresholdTimestamp).toDomainList()
        }

    override suspend fun getItemsExpiringWithinSync(thresholdTimestamp: Long): List<PantryItem> =
        getItemsExpiringBeforeSync(thresholdTimestamp)

    override fun getItemsByCategory(category: Category): Flow<List<PantryItem>> =
        pantryDao.getItemsByCategory(category.name)
            .map { it.toDomainList() }
            .flowOn(ioDispatcher)

    override fun searchItems(query: String): Flow<List<PantryItem>> =
        pantryDao.searchItems(query)
            .map { it.toDomainList() }
            .flowOn(ioDispatcher)

    override suspend fun getItemById(id: Long): PantryItem? =
        withContext(ioDispatcher) {
            pantryDao.getItemById(id)?.toDomain()
        }

    override fun observeItemById(id: Long): Flow<PantryItem?> =
        pantryDao.getItemByIdFlow(id)
            .map { it?.toDomain() }
            .flowOn(ioDispatcher)

    override suspend fun getItemByBarcode(barcode: String): PantryItem? =
        withContext(ioDispatcher) {
            pantryDao.getItemByBarcode(barcode)?.toDomain()
        }

    override suspend fun insertItem(item: PantryItem): Long =
        withContext(ioDispatcher) {
            pantryDao.insertItem(item.toEntity())
        }

    override suspend fun insertItems(items: List<PantryItem>): List<Long> =
        withContext(ioDispatcher) {
            pantryDao.insertItems(items.toEntityList())
        }

    override suspend fun updateItem(item: PantryItem) =
        withContext(ioDispatcher) {
            pantryDao.updateItem(item.toEntity())
        }

    override suspend fun updateQuantity(id: Long, quantity: Int) =
        withContext(ioDispatcher) {
            pantryDao.updateQuantity(id, quantity)
        }

    override suspend fun deleteItem(item: PantryItem) =
        withContext(ioDispatcher) {
            pantryDao.deleteItem(item.toEntity())
        }

    override suspend fun deleteItemById(id: Long) =
        withContext(ioDispatcher) {
            pantryDao.deleteItemById(id)
        }

    suspend fun deleteExpiredItems(currentTimestamp: Long): Int =
        withContext(ioDispatcher) {
            pantryDao.deleteExpiredItems(currentTimestamp)
        }

    override suspend fun deleteAllItems(): Int =
        withContext(ioDispatcher) {
            pantryDao.deleteAllItems()
        }
}
