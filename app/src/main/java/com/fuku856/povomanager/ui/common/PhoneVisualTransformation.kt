package com.fuku856.povomanager.ui.common

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * 電話番号入力欄で、保存値(数字のみ)を変えずに表示だけ日本の番号形式
 * (080-1234-5678 / 03-1234-5678)へ整形する VisualTransformation。
 *
 * 区切り規則は [phoneGroupSizes] / [phoneHyphenBoundaries] を共有し、表示
 * (ホーム等の [formatPhoneNumber])と一致させる。入力途中の桁数でも段階的に
 * ハイフンを挿入する。
 */
class PhoneNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        // 表示されるハイフンの挿入位置(原文インデックス)。末尾の区切りは含まれない。
        val boundaries = phoneHyphenBoundaries(digits)

        val formatted = buildString {
            for (i in digits.indices) {
                if (i in boundaries) append('-')
                append(digits[i])
            }
        }

        val offsetMapping = object : OffsetMapping {
            // 原文オフセットの手前に挿入されたハイフン数を加算する。
            override fun originalToTransformed(offset: Int): Int =
                offset + boundaries.count { it <= offset }

            // 変換後オフセットの手前にあるハイフン数を差し引く。
            // k 番目のハイフンの変換後インデックスは boundaries[k] + k。
            override fun transformedToOriginal(offset: Int): Int {
                var hyphensBefore = 0
                boundaries.forEachIndexed { k, b ->
                    if (b + k < offset) hyphensBefore++
                }
                return offset - hyphensBefore
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}
