package com.agg.app.ui.more

import androidx.lifecycle.SavedStateHandle
import com.agg.app.ui.common.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoreViewModelTest {
    @Test
    fun `defaults to summary with about dialog closed`() {
        val viewModel = MoreViewModel(SavedStateHandle())

        assertEquals(
            UiState.Success(MoreData(selectedSection = MoreSection.SUMMARY, showAbout = false)),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `selects categories and summary`() {
        val viewModel = MoreViewModel(SavedStateHandle())

        viewModel.selectSection(MoreSection.CATEGORIES)
        assertEquals(MoreSection.CATEGORIES, viewModel.data().selectedSection)

        viewModel.selectSection(MoreSection.SUMMARY)
        assertEquals(MoreSection.SUMMARY, viewModel.data().selectedSection)
    }

    @Test
    fun `about dialog can be opened and dismissed`() {
        val viewModel = MoreViewModel(SavedStateHandle())

        viewModel.showAbout()
        assertTrue(viewModel.data().showAbout)

        viewModel.dismissAbout()
        assertFalse(viewModel.data().showAbout)
    }

    @Test
    fun `selection and about dialog restore from saved state`() {
        val savedState = SavedStateHandle()
        val original = MoreViewModel(savedState)
        original.selectSection(MoreSection.CATEGORIES)
        original.showAbout()

        val recreated = MoreViewModel(SavedStateHandle(savedState.keys().associateWith { savedState.get<Any>(it) }))

        assertEquals(MoreSection.CATEGORIES, recreated.data().selectedSection)
        assertTrue(recreated.data().showAbout)
    }

    private fun MoreViewModel.data(): MoreData =
        (uiState.value as UiState.Success).data
}
