package com.moviepilot.app.data.model

/**
 * 资源搜索筛选条件
 *
 * 设计原则：全部筛选在客户端完成，不依赖服务器 PWA 前端。
 * 早期版本的筛选面板由服务器 Web 页面渲染，服务器升级后弹窗遮罩
 * 样式与脚本版本错配，导致只显示灰色遮罩、筛选面板无法展开。
 * 改为原生 Compose 渲染后，筛选能力完全由 App 控制，不再受
 * 服务器前端版本变动影响。
 *
 * 所有维度均为「空集合 = 不限制」，多个维度之间是 AND 关系，
 * 同一维度内多选是 OR 关系。
 */
data class ResourceFilter(
    // 分站
    val sites: Set<String> = emptySet(),
    // 分辨率 (如 2160p / 1080p / 4k)
    val resolutions: Set<String> = emptySet(),
    // 视频编码 (如 HEVC / x265 10bit)
    val encodes: Set<String> = emptySet(),
    // 资源类型 (如 WEB-DL / UHD BluRay)
    val resourceTypes: Set<String> = emptySet(),
    // 特效 (如 DV / HDR10 DoVi)
    val effects: Set<String> = emptySet(),
    // 制作组 (如 UBWEB / HDS)
    val teams: Set<String> = emptySet(),
    // 分类 (电影 / 电视剧 / 未知)
    val categories: Set<String> = emptySet(),

    // 体积区间（字节）
    val minSize: Long = 0L,
    val maxSize: Long = Long.MAX_VALUE,

    // 做种数下限
    val minSeeders: Int = 0,

    // 免费种 / 促销种
    val onlyFree: Boolean = false,
    val onlyPromotion: Boolean = false,

    // 排序方式
    val sortBy: ResourceSort = ResourceSort.DEFAULT
) {
    /** 当前是否有任何生效的筛选条件 */
    val hasActiveFilter: Boolean
        get() = sites.isNotEmpty() ||
            resolutions.isNotEmpty() ||
            encodes.isNotEmpty() ||
            resourceTypes.isNotEmpty() ||
            effects.isNotEmpty() ||
            teams.isNotEmpty() ||
            categories.isNotEmpty() ||
            minSize > 0L ||
            maxSize != Long.MAX_VALUE ||
            minSeeders > 0 ||
            onlyFree ||
            onlyPromotion ||
            sortBy != ResourceSort.DEFAULT

    /** 生效条件的数量，用于在筛选按钮上显示角标 */
    val activeCount: Int
        get() {
            var n = 0
            if (sites.isNotEmpty()) n++
            if (resolutions.isNotEmpty()) n++
            if (encodes.isNotEmpty()) n++
            if (resourceTypes.isNotEmpty()) n++
            if (effects.isNotEmpty()) n++
            if (teams.isNotEmpty()) n++
            if (categories.isNotEmpty()) n++
            if (minSize > 0L || maxSize != Long.MAX_VALUE) n++
            if (minSeeders > 0) n++
            if (onlyFree) n++
            if (onlyPromotion) n++
            return n
        }

    companion object {
        val EMPTY = ResourceFilter()
    }
}

enum class ResourceSort(val label: String) {
    DEFAULT("综合排序"),
    SIZE_DESC("体积从大到小"),
    SIZE_ASC("体积从小到大"),
    SEEDERS_DESC("做种数从多到少"),
    TIME_DESC("发布时间最新")
}

/**
 * 资源筛选维度定义
 */
enum class ResourceFilterDimension(
    val label: String,
    val maxVisible: Int = 12
) {
    SITE("站点"),
    RESOLUTION("分辨率"),
    ENCODE("视频编码"),
    RESOURCE_TYPE("资源类型"),
    EFFECT("特效"),
    TEAM("制作组"),
    CATEGORY("分类")
}

/**
 * 客户端筛选执行器
 */
object ResourceFilterEngine {

    /** 从搜索结果中提取某个维度的全部可选值（按出现频次降序） */
    fun extractOptions(
        results: List<TorrentSearchResult>,
        dimension: ResourceFilterDimension
    ): List<String> = when (dimension) {
        ResourceFilterDimension.SITE ->
            results.mapNotNull { it.torrentInfo.siteName.takeIf { s -> s.isNotBlank() } }
        ResourceFilterDimension.RESOLUTION ->
            results.mapNotNull { it.metaInfo?.resourcePix?.takeIf { s -> s.isNotBlank() } }
        ResourceFilterDimension.ENCODE ->
            results.mapNotNull { it.metaInfo?.videoEncode?.takeIf { s -> s.isNotBlank() } }
        ResourceFilterDimension.RESOURCE_TYPE ->
            results.mapNotNull { it.metaInfo?.resourceType?.takeIf { s -> s.isNotBlank() } }
        ResourceFilterDimension.EFFECT ->
            results.mapNotNull { it.metaInfo?.resourceEffect?.takeIf { s -> s.isNotBlank() } }
        ResourceFilterDimension.TEAM ->
            results.mapNotNull { it.metaInfo?.resourceTeam?.takeIf { s -> s.isNotBlank() } }
        ResourceFilterDimension.CATEGORY ->
            results.mapNotNull { it.torrentInfo.category?.takeIf { s -> s.isNotBlank() } }
    }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .map { it.key }

    /** 应用筛选 + 排序 */
    fun apply(
        results: List<TorrentSearchResult>,
        filter: ResourceFilter
    ): List<TorrentSearchResult> {
        val filtered = results.filter { item ->
            matches(item, filter)
        }
        return when (filter.sortBy) {
            ResourceSort.DEFAULT -> filtered
            ResourceSort.SIZE_DESC -> filtered.sortedByDescending { it.torrentInfo.size }
            ResourceSort.SIZE_ASC -> filtered.sortedBy { it.torrentInfo.size }
            ResourceSort.SEEDERS_DESC -> filtered.sortedByDescending { it.torrentInfo.seeders }
            ResourceSort.TIME_DESC -> filtered.sortedByDescending { it.torrentInfo.pubDate ?: "" }
        }
    }

    private fun matches(item: TorrentSearchResult, filter: ResourceFilter): Boolean {
        val meta = item.metaInfo
        val info = item.torrentInfo

        if (filter.sites.isNotEmpty() && info.siteName !in filter.sites) return false
        if (filter.categories.isNotEmpty() && (info.category ?: "") !in filter.categories) return false
        if (filter.resolutions.isNotEmpty() && (meta?.resourcePix ?: "") !in filter.resolutions) return false
        if (filter.encodes.isNotEmpty() && (meta?.videoEncode ?: "") !in filter.encodes) return false
        if (filter.resourceTypes.isNotEmpty() && (meta?.resourceType ?: "") !in filter.resourceTypes) return false
        if (filter.effects.isNotEmpty() && (meta?.resourceEffect ?: "") !in filter.effects) return false
        if (filter.teams.isNotEmpty() && (meta?.resourceTeam ?: "") !in filter.teams) return false

        if (info.size < filter.minSize) return false
        if (info.size > filter.maxSize) return false
        if (info.seeders < filter.minSeeders) return false

        if (filter.onlyFree && !isFree(info)) return false
        if (filter.onlyPromotion && !isPromotion(info)) return false

        return true
    }

    /** 免费种：促销比例或_freeDate 标识 */
    private fun isFree(info: TorrentInfoDetail): Boolean {
        val vf = info.volumeFactor
        if (!vf.isNullOrBlank() && vf != "100%" && vf.contains("%")) return true
        if (!info.freeDate.isNullOrBlank() && info.freeDate != "3000-12-31") return true
        val diff = info.freeDateDiff
        if (!diff.isNullOrBlank()) {
            // 形如 "3天" / "expired" 等非空描述
            return true
        }
        return false
    }

    /** 促销种：折扣小于 1 */
    private fun isPromotion(info: TorrentInfoDetail): Boolean {
        val d = info.downloadVolumeFactor ?: return false
        return d < 1f
    }

    /** 体积预设选项（字节） */
    val SIZE_PRESETS: List<Pair<String, Long>> = listOf(
        "不限" to Long.MAX_VALUE,
        "< 1GB" to (1L * 1024 * 1024 * 1024),
        "1-5GB" to (5L * 1024 * 1024 * 1024),
        "5-20GB" to (20L * 1024 * 1024 * 1024),
        "20-50GB" to (50L * 1024 * 1024 * 1024),
        "> 50GB" to Long.MAX_VALUE
    )
}
