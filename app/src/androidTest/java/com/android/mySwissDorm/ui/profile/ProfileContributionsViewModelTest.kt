package com.android.mySwissDorm.ui.profile

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileContributionsViewModelTest {

  @Test
  fun initialState_isLoadingTrue_andEmpty() {
    val vm = ProfileContributionsViewModel()
    val ui = vm.ui.value
    assertTrue(ui.isLoading)
    assertTrue(ui.items.isEmpty())
    assertEquals(null, ui.error)
  }

  @Test
  fun setFromExternal_setsItems_andStopsLoading() {
    val vm = ProfileContributionsViewModel()
    val data =
        listOf(
            Contribution("Listing l1", "Nice room near EPFL"),
            Contribution("Request r1", "Student interested in a room"))
    vm.setFromExternal(data)
    val ui = vm.ui.value
    assertFalse(ui.isLoading)
    assertEquals(2, ui.items.size)
    assertEquals("Listing l1", ui.items.first().title)
    assertEquals(null, ui.error)
  }

  @Test
  fun load_setsLoadingFlag_immediately_withoutCrashing() {
    val vm = ProfileContributionsViewModel()
    // First call flips loading=true synchronously before the coroutine starts.
    vm.load()
    assertTrue(vm.ui.value.isLoading)
  }

  @Test
  fun fakeRepository_fetchesTwoContributions() = runBlocking {
    // Covers lines inside FakeContributionsRepository without using Main dispatcher
    val repo = FakeContributionsRepository()
    val items = repo.fetchMyContributions()
    assertEquals(2, items.size)
    assertEquals("Listing l1", items[0].title)
    assertEquals("Request r1", items[1].title)
  }
}
