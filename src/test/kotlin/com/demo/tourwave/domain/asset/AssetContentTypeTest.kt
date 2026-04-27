package com.demo.tourwave.domain.asset

import com.demo.tourwave.domain.common.DomainException
import com.demo.tourwave.domain.common.ErrorCode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AssetContentTypeTest {
    // === 허용 타입 5개 ===

    @Test
    fun `image_jpeg is allowed`() {
        val result = AssetContentType.fromString("image/jpeg")
        assertEquals(AssetContentType.IMAGE_JPEG, result)
    }

    @Test
    fun `image_png is allowed`() {
        val result = AssetContentType.fromString("image/png")
        assertEquals(AssetContentType.IMAGE_PNG, result)
    }

    @Test
    fun `image_webp is allowed`() {
        val result = AssetContentType.fromString("image/webp")
        assertEquals(AssetContentType.IMAGE_WEBP, result)
    }

    @Test
    fun `image_gif is allowed`() {
        val result = AssetContentType.fromString("image/gif")
        assertEquals(AssetContentType.IMAGE_GIF, result)
    }

    @Test
    fun `application_pdf is allowed`() {
        val result = AssetContentType.fromString("application/pdf")
        assertEquals(AssetContentType.APPLICATION_PDF, result)
    }

    // === 거부 케이스 ===

    @Test
    fun `text_html is rejected with 422`() {
        val ex =
            assertFailsWith<DomainException> {
                AssetContentType.fromString("text/html")
            }
        assertEquals(ErrorCode.ASSET_UNSUPPORTED_CONTENT_TYPE, ex.errorCode)
        assertEquals(422, ex.status)
    }

    @Test
    fun `application_x_msdownload is rejected with 422`() {
        val ex =
            assertFailsWith<DomainException> {
                AssetContentType.fromString("application/x-msdownload")
            }
        assertEquals(ErrorCode.ASSET_UNSUPPORTED_CONTENT_TYPE, ex.errorCode)
        assertEquals(422, ex.status)
    }

    @Test
    fun `blank string is rejected with 422`() {
        val ex =
            assertFailsWith<DomainException> {
                AssetContentType.fromString("   ")
            }
        assertEquals(ErrorCode.ASSET_UNSUPPORTED_CONTENT_TYPE, ex.errorCode)
        assertEquals(422, ex.status)
    }

    @Test
    fun `arbitrary unknown type is rejected with 422`() {
        val ex =
            assertFailsWith<DomainException> {
                AssetContentType.fromString("application/octet-stream")
            }
        assertEquals(ErrorCode.ASSET_UNSUPPORTED_CONTENT_TYPE, ex.errorCode)
        assertEquals(422, ex.status)
    }

    @Test
    fun `uppercase input is normalized and accepted`() {
        val result = AssetContentType.fromString("IMAGE/JPEG")
        assertEquals(AssetContentType.IMAGE_JPEG, result)
    }

    @Test
    fun `mixed case input is normalized and accepted`() {
        val result = AssetContentType.fromString("Application/PDF")
        assertEquals(AssetContentType.APPLICATION_PDF, result)
    }

    @Test
    fun `mimeType property returns correct mime string`() {
        assertEquals("image/jpeg", AssetContentType.IMAGE_JPEG.mimeType)
        assertEquals("image/png", AssetContentType.IMAGE_PNG.mimeType)
        assertEquals("image/webp", AssetContentType.IMAGE_WEBP.mimeType)
        assertEquals("image/gif", AssetContentType.IMAGE_GIF.mimeType)
        assertEquals("application/pdf", AssetContentType.APPLICATION_PDF.mimeType)
    }
}
