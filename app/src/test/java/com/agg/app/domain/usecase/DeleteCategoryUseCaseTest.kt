package com.agg.app.domain.usecase

import com.agg.app.testutil.InMemoryCategoryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DeleteCategoryUseCaseTest {

    private val repository = InMemoryCategoryRepository()
    private val deleteCategory = DeleteCategoryUseCase(repository)

    @Test
    fun `custom category without transactions is deleted`() = runTest {
        val id = repository.custom.id

        assertEquals(DeleteCategoryResult.Deleted, deleteCategory(id))
        assertNull(repository.getCategory(id))
    }

    @Test
    fun `default category is never deleted`() = runTest {
        val id = repository.default.id

        assertEquals(DeleteCategoryResult.IsDefault, deleteCategory(id))
        assertNotNull(repository.getCategory(id))
    }

    @Test
    fun `category with transactions is kept`() = runTest {
        val id = repository.custom.id
        repository.inUse += id

        assertEquals(DeleteCategoryResult.InUse, deleteCategory(id))
        assertNotNull(repository.getCategory(id))
    }

    @Test
    fun `unknown category reports not found`() = runTest {
        assertEquals(DeleteCategoryResult.NotFound, deleteCategory(999))
    }
}
