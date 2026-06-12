package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.service.PdfTextExtractor
import com.example.viewmodel.MainViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Hello Nova", appName)
  }

  @Test
  fun `pdf text extractor returns error safely for invalid uri`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val invalidUri = Uri.parse("content://corrupted/document.pdf")
    val result = PdfTextExtractor.extract(context, invalidUri)
    assertNotNull(result)
  }

  @Test
  fun `main view model starts with idle vision status`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = MainViewModel(application)
    
    // Assert defaults
    assertEquals("ONLINE", viewModel.visionEngineStatus.value)
    assertEquals("None", viewModel.lastAnalyzedAsset.value)
    assertEquals(0L, viewModel.visionAnalysisTimeMs.value)
    assertEquals("IDLE", viewModel.multimodalSessionStatus.value)
    assertNull(viewModel.selectedImageUri.value)
    assertNull(viewModel.selectedPdfUri.value)
  }

  @Test
  fun `main view model processes image selection correctly`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = MainViewModel(application)
    val testUri = Uri.parse("content://media/external/images/media/42")

    viewModel.processImageSelection(testUri)

    assertEquals(testUri, viewModel.selectedImageUri.value)
    assertNull(viewModel.selectedPdfUri.value)
    assertEquals("ACTIVE_IMAGE", viewModel.multimodalSessionStatus.value)
    assertEquals("42", viewModel.lastAnalyzedAsset.value)
  }

  @Test
  fun `main view model purges multimodal context successfully`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = MainViewModel(application)
    val testUri = Uri.parse("content://media/external/images/media/42")

    viewModel.processImageSelection(testUri)
    viewModel.clearActiveMultimodalAsset()

    assertNull(viewModel.selectedImageUri.value)
    assertNull(viewModel.selectedPdfUri.value)
    assertEquals("IDLE", viewModel.multimodalSessionStatus.value)
    assertEquals("None", viewModel.lastAnalyzedAsset.value)
  }
}
