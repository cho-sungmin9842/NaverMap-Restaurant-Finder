package com.example.navermap

import androidx.core.text.HtmlCompat
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SearchResult(
    @field:Json(name = "items") val items: List<SearchItem>
)

@JsonClass(generateAdapter = true)
data class SearchItem(
    @field:Json(name = "title") val title: String,
    @field:Json(name = "link") val link: String,
    @field:Json(name = "category") val category: String,
    @field:Json(name = "roadAddress") val roadAddress: String,
    @field:Json(name = "mapx") val mapx: Int,
    @field:Json(name = "mapy") val mapy: Int
) {
    // 검색 API 응답의 title에는 <b>강조</b> 같은 HTML 태그와 &amp; 같은 엔티티가 섞여 있으므로 제거/변환한다.
    val plainTitle: String
        get() = HtmlCompat.fromHtml(title, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()
}