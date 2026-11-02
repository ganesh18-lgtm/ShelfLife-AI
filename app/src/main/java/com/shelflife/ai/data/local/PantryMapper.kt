package com.shelflife.ai.data.local

import com.shelflife.ai.domain.model.Category
import com.shelflife.ai.domain.model.PantryItem

/**
 * Converts a database [PantryItemEntity] to a pure domain [PantryItem].
 */
fun PantryItemEntity.toDomain(): PantryItem {
    val domainCategory = Category.fromString(category)

    return PantryItem(
        id = id,
        name = name,
        barcode = barcode,
        expiryTimestamp = expiryTimestamp,
        category = domainCategory,
        quantity = quantity,
        addedTimestamp = addedTimestamp,
        imageUri = imageUri,
        mfgDateTimestamp = mfgDateTimestamp,
        expiryMode = expiryMode
    )
}

/**
 * Converts a domain [PantryItem] to a database [PantryItemEntity].
 */
fun PantryItem.toEntity(): PantryItemEntity = PantryItemEntity(
    id = id,
    name = name,
    barcode = barcode,
    expiryTimestamp = expiryTimestamp,
    category = category.name,
    quantity = quantity,
    addedTimestamp = addedTimestamp,
    imageUri = imageUri,
    mfgDateTimestamp = mfgDateTimestamp,
    expiryMode = expiryMode
)

/**
 * Converts a list of [PantryItemEntity] to a list of domain [PantryItem].
 */
fun List<PantryItemEntity>.toDomainList(): List<PantryItem> = map { it.toDomain() }

/**
 * Converts a list of domain [PantryItem] to a list of [PantryItemEntity].
 */
fun List<PantryItem>.toEntityList(): List<PantryItemEntity> = map { it.toEntity() }
