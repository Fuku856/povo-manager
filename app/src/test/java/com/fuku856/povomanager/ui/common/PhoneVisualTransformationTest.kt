package com.fuku856.povomanager.ui.common

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneVisualTransformationTest {

    private val transformation = PhoneNumberVisualTransformation()

    private fun transformed(input: String): String =
        transformation.filter(AnnotatedString(input)).text.text

    // --- 表示整形 ---

    @Test
    fun mobile11Digits_formatsAs3_4_4() {
        assertEquals("080-1234-5678", transformed("08012345678"))
        assertEquals("090-1234-5678", transformed("09012345678"))
        assertEquals("070-1234-5678", transformed("07012345678"))
        assertEquals("050-1234-5678", transformed("05012345678"))
    }

    @Test
    fun landline10Digits_formatsAs2_4_4() {
        assertEquals("03-1234-5678", transformed("0312345678"))
        assertEquals("06-1234-5678", transformed("0612345678"))
    }

    @Test
    fun partialInput_insertsHyphensProgressively() {
        assertEquals("080", transformed("080"))
        assertEquals("080-1", transformed("0801"))
        assertEquals("080-1234", transformed("0801234"))
        assertEquals("080-1234-5", transformed("08012345"))
        // 固定電話プレフィックス確定前は 2-4-4 区切り
        assertEquals("03", transformed("03"))
        assertEquals("03-1", transformed("031"))
    }

    @Test
    fun empty_returnsEmpty() {
        assertEquals("", transformed(""))
    }

    // --- formatPhoneNumber(表示側)との一致 ---

    @Test
    fun formatPhoneNumber_matchesTransformation_forCompleteNumbers() {
        assertEquals("080-1234-5678", formatPhoneNumber("08012345678"))
        assertEquals("03-1234-5678", formatPhoneNumber("0312345678"))
        // 10〜11桁以外はそのまま
        assertEquals("080", formatPhoneNumber("080"))
    }

    // --- OffsetMapping の整合性 ---

    @Test
    fun offsetMapping_isConsistentAtBoundaries() {
        val result = transformation.filter(AnnotatedString("08012345678"))
        val mapping = result.offsetMapping
        // "080-1234-5678" : 原文0..11 → 変換後の境界でハイフン分ずれる
        assertEquals(0, mapping.originalToTransformed(0))
        assertEquals(4, mapping.originalToTransformed(3))   // "080-" の後ろ
        assertEquals(9, mapping.originalToTransformed(7))   // "080-1234-" の後ろ
        assertEquals(13, mapping.originalToTransformed(11)) // 末尾(全長)
        // 逆変換は往復で原文オフセットに戻る
        for (o in 0..11) {
            assertEquals(o, mapping.transformedToOriginal(mapping.originalToTransformed(o)))
        }
    }

    @Test
    fun offsetMapping_staysInBounds_forPartialInput() {
        val result = transformation.filter(AnnotatedString("080"))
        val mapping = result.offsetMapping
        // ハイフン未表示なので原文=変換後
        assertEquals(3, mapping.originalToTransformed(3))
        assertEquals(3, mapping.transformedToOriginal(3))
    }
}
