package com.abdownloadmanager.updatechecker

import io.github.z4kn4fein.semver.Version
import ir.amirab.util.platform.Arch
import ir.amirab.util.platform.Platform

/**
 * 暂时屏蔽官方更新渠道，始终返回无更新。
 * 后续可替换为自定义更新检查接口的实现。
 */
class NoUpdateChecker(currentVersion: Version) : UpdateChecker(currentVersion) {
    override suspend fun getMyPlatformLatestVersion(): UpdateInfo {
        return UpdateInfo(
            version = currentVersion,
            platform = Platform.getCurrentPlatform(),
            arch = Arch.getCurrentArch(),
            updateSource = listOf(
                UpdateSource.DirectDownloadLink(
                    link = "",
                    name = "",
                    hash = null,
                    installableArch = null,
                )
            ),
            changeLog = ""
        )
    }
}
