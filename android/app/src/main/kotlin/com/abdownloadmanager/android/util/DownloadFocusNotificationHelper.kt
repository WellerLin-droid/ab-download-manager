package com.abdownloadmanager.android.util

import android.content.Context
import android.graphics.drawable.Icon
import android.os.Bundle
import org.json.JSONObject

/**
 * 下载进度焦点通知辅助类
 *
 * 纯手动构建 MIUI 澎湃焦点通知参数，无外部依赖。
 *
 * 小岛（未展开）：
 *   左侧：应用图标
 *   右侧：文字进度（如 "50%"）
 *   固定宽度，进度变化不抖动
 *
 * 大岛（展开）：
 *   应用图标标记 + 下载信息 + 进度条
 */
object DownloadFocusNotificationHelper {

    private const val PIC_TICKER = "miui.focus.pic_ticker"
    private const val PIC_MARK = "miui.focus.pic_mark_v2"

    fun buildFocusExtras(
        context: Context,
        speedText: String,
        etaText: String?,
        percent: Int?,
        isFirstNotify: Boolean,
        activeDownloadCount: Int,
    ): Bundle {
        val appIcon = try {
            val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
            Icon.createWithResource(context, appInfo.icon)
        } catch (_: Exception) {
            Icon.createWithResource(context, android.R.drawable.stat_sys_download)
        }

        val picsBundle = Bundle()
        picsBundle.putParcelable(PIC_TICKER, appIcon)
        picsBundle.putParcelable(PIC_MARK, appIcon)

        // miui.focus.pics 在 build() 前通过 addExtras 注入
        val extras = Bundle()
        extras.putBundle("miui.focus.pics", picsBundle)

        // miui.focus.param 在 build() 后通过 extras.putString 注入（参考 123Networkdisk 模式）
        val paramJson = buildParamJson(speedText, etaText, percent, isFirstNotify, activeDownloadCount)
        extras.putString("miui.focus.param", paramJson)

        return extras
    }

    private fun buildParamJson(
        speedText: String,
        etaText: String?,
        percent: Int?,
        isFirstNotify: Boolean,
        activeDownloadCount: Int,
    ): String {
        val safeProgress = percent?.coerceIn(0, 100) ?: 0
        val param = JSONObject()
        val paramV2 = JSONObject()

        paramV2.put("protocol", 1)
        // 小岛显示文字进度，固定宽度
        paramV2.put("ticker", if (safeProgress > 0) "$safeProgress%" else "下载中")
        paramV2.put("tickerPic", PIC_TICKER)
        paramV2.put("updatable", true)
        paramV2.put("timeout", if (isFirstNotify) 30 else 280)
        paramV2.put("enableFloat", isFirstNotify)
        paramV2.put("islandFirstFloat", isFirstNotify)
        paramV2.put("isShowNotification", true)
        paramV2.put("showSmallIcon", true)

        // 大岛展开内容
        val title = if (activeDownloadCount <= 1) "正在下载文件" else "正在下载${activeDownloadCount}个文件"
        val baseInfo = JSONObject()
        baseInfo.put("type", 1)
        baseInfo.put("title", title)
        if (speedText.isNotBlank()) baseInfo.put("content", speedText)
        if (!etaText.isNullOrBlank()) baseInfo.put("subContent", etaText)
        paramV2.put("baseInfo", baseInfo)

        // 进度条
        if (safeProgress > 0) {
            val progressInfo = JSONObject()
            progressInfo.put("progress", safeProgress)
            progressInfo.put("colorProgress", "#34C759")
            if (safeProgress < 100) progressInfo.put("colorProgressEnd", "#30B0C7")
            paramV2.put("progressInfo", progressInfo)
        }

        // 大岛右侧应用图标
        val picInfo = JSONObject()
        picInfo.put("type", 1)
        picInfo.put("pic", PIC_MARK)
        paramV2.put("picInfo", picInfo)

        param.put("param_v2", paramV2)
        param.put("isShowNotification", true)
        return param.toString()
    }
}
