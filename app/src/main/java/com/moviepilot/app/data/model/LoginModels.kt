package com.moviepilot.app.data.model

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

/**
 * POST api/v1/login/access-token 的响应。
 *
 * v2 / v3 都会返回 access_token；其余字段用来还原网页版的登录态，
 * 尤其是 super_user —— 网页版路由 `/` 会按它决定进 /dashboard 还是 /apps。
 */
data class LoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String? = null,
    @SerializedName("super_user") val superUser: Boolean? = null,
    @SerializedName("user_id") val userId: Int? = null,
    @SerializedName("user_name") val userName: String? = null,
    @SerializedName("avatar") val avatar: String? = null,
    @SerializedName("level") val level: Int? = null,
    @SerializedName("permissions") val permissions: JsonObject? = null
)

/**
 * 网页版 Pinia 里 `user` store 的持久化结构：
 *
 * ```json
 * { "superUser": true, "userID": 1, "userName": "admin",
 *   "avatar": "...", "level": 2, "permissions": {...}, "wizard": false }
 * ```
 *
 * 必须和 `auth` 一起写进 localStorage，否则网页版冷启动时
 * `superUser` 还是默认的 false，根路由会把超管也甩到「应用中心 /apps」，
 * 表现就是「只能看到订阅相关入口、没有真正的主页」。
 */
object WebUserSeed {

    private fun build(
        superUser: Boolean,
        userId: Int,
        userName: String,
        avatar: String,
        level: Int,
        permissions: JsonObject?
    ): String {
        val o = JsonObject()
        o.addProperty("superUser", superUser)
        o.addProperty("userID", userId)
        o.addProperty("userName", userName)
        o.addProperty("avatar", avatar)
        o.addProperty("level", level)
        if (permissions != null) o.add("permissions", permissions)
        o.addProperty("wizard", false)
        return o.toString()
    }

    /** 登录响应里字段最全（含 level / permissions），优先用它 */
    fun fromLogin(response: LoginResponse): String? {
        if (response.accessToken.isBlank()) return null
        return build(
            superUser = response.superUser == true,
            userId = response.userId ?: -1,
            userName = response.userName.orEmpty(),
            avatar = response.avatar.orEmpty(),
            level = response.level ?: 1,
            permissions = response.permissions
        )
    }

    /**
     * GET api/v1/user/current 的响应。
     * v3 会包一层 `{success, data}`，v2 直接返回对象，两种都兼容。
     */
    fun fromCurrentUser(root: JsonObject?): String? {
        if (root == null) return null
        val data = runCatching {
            if (root.has("data") && root.get("data").isJsonObject) root.getAsJsonObject("data") else root
        }.getOrNull() ?: return null

        fun str(key: String): String = runCatching {
            if (data.has(key) && !data.get(key).isJsonNull) data.get(key).asString else ""
        }.getOrDefault("")

        fun int(key: String, fallback: Int): Int = runCatching {
            if (data.has(key) && !data.get(key).isJsonNull) data.get(key).asInt else fallback
        }.getOrDefault(fallback)

        fun bool(key: String): Boolean = runCatching {
            if (data.has(key) && !data.get(key).isJsonNull) data.get(key).asBoolean else false
        }.getOrDefault(false)

        val permissions = runCatching {
            if (data.has("permissions") && data.get("permissions").isJsonObject) {
                data.getAsJsonObject("permissions")
            } else {
                null
            }
        }.getOrNull()

        return build(
            superUser = bool("is_superuser"),
            userId = int("id", -1),
            userName = str("name"),
            avatar = str("avatar"),
            level = int("level", 1),
            permissions = permissions
        )
    }
}
