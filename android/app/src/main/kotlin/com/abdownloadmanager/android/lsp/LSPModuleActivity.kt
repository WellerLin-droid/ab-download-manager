package com.abdownloadmanager.android.lsp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.abdownloadmanager.android.BuildConfig
import com.abdownloadmanager.android.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * LSPosed 模块设置入口 Activity
 *
 * 通过 de.robv.android.xposed.category.MODULE_SETTINGS 被 LSPosed 框架发现。
 * 完全使用 MIUIX 组件库构建，保持原插件版 UI 风格。
 */
class LSPModuleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiuixTheme {
                MainScreen()
            }
        }
    }
}

@Composable
private fun MainScreen() {
    val scope = rememberCoroutineScope()
    // 仅用于 Toast 展示，不做资源读取（不会触发 warning）
    val context = LocalContext.current

    // 设置状态
    var unlockFocus by remember { mutableStateOf(true) }
    var rootManagerIndex by remember { mutableIntStateOf(0) }
    var showRestartDialog by remember { mutableStateOf(false) }

    // 预取所有字符串资源（避免 composition 中 query LocalContext）
    val checkingLspStatus = stringResource(R.string.lsp_status_checking)
    val checkingRootStatus = stringResource(R.string.lsp_root_checking)
    val activatedStr = stringResource(R.string.lsp_status_activated)
    val rootGrantedStr = stringResource(R.string.lsp_root_granted)
    val rootDeniedStr = stringResource(R.string.lsp_root_denied)
    val toastRestartSuccess = stringResource(R.string.lsp_toast_restart_success)
    val toastRestartFailed = stringResource(R.string.lsp_toast_restart_failed)

    var lspStatus by remember { mutableStateOf(checkingLspStatus) }
    var rootStatus by remember { mutableStateOf(checkingRootStatus) }

    // 检测 LSP 模块状态和 Root 权限
    remember {
        scope.launch {
            lspStatus = activatedStr
            rootStatus = withContext(Dispatchers.IO) {
                try {
                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo root_ok"))
                    val exitCode = process.waitFor()
                    if (exitCode == 0) rootGrantedStr else rootDeniedStr
                } catch (e: Exception) {
                    rootDeniedStr
                }
            }
        }
    }

    val rootManagerOptions = listOf("KSU", "Magisk", "APatch")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.lsp_module_name),
                actions = {
                    IconButton(
                        onClick = { showRestartDialog = true }
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Refresh,
                            contentDescription = stringResource(R.string.lsp_action_restart_systemui),
                        )
                    }
                }
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // ===== 功能设置 =====
            SmallTitle(text = stringResource(R.string.lsp_category_features))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                SwitchPreference(
                    checked = unlockFocus,
                    onCheckedChange = { unlockFocus = it },
                    title = stringResource(R.string.lsp_unlock_focus),
                    summary = stringResource(R.string.lsp_unlock_focus_summary),
                )
                OverlayDropdownPreference(
                    items = rootManagerOptions,
                    selectedIndex = rootManagerIndex,
                    title = stringResource(R.string.lsp_root_manager),
                    summary = stringResource(R.string.lsp_root_manager_summary),
                    onSelectedIndexChange = { rootManagerIndex = it },
                )
            }

            // ===== 状态信息 =====
            SmallTitle(
                modifier = Modifier.padding(top = 12.dp),
                text = stringResource(R.string.lsp_category_status)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                StatusRow(
                    label = stringResource(R.string.lsp_status_label),
                    value = lspStatus,
                )
                StatusRow(
                    label = stringResource(R.string.lsp_root_label),
                    value = rootStatus,
                )
                StatusRow(
                    label = stringResource(R.string.lsp_version_label),
                    value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                )
            }
        }

        // 重启确认对话框
        OverlayDialog(
            show = showRestartDialog,
            title = stringResource(R.string.lsp_dialog_restart_title),
            summary = stringResource(R.string.lsp_dialog_restart_message),
            onDismissRequest = { showRestartDialog = false },
            content = {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(
                        text = stringResource(R.string.lsp_dialog_cancel),
                        onClick = { showRestartDialog = false },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(20.dp))
                    TextButton(
                        text = stringResource(R.string.lsp_dialog_confirm),
                        onClick = {
                            showRestartDialog = false
                            scope.launch(Dispatchers.IO) {
                                try {
                                    Runtime.getRuntime().exec(arrayOf("su", "-c", "pkill -f com.android.systemui"))
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, toastRestartSuccess, Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, toastRestartFailed, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            },
        )
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    ArrowPreference(
        title = label,
        summary = value,
        enabled = false,
    )
}
