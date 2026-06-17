package com.abdownloadmanager.android.lsp

import android.os.Bundle
import android.util.Log
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface

/**
 * AB Download Manager LSPosed 模块入口
 *
 * 最低支持 libxposed API v101，目标 API v102
 * Hook 范围:
 *   - com.android.systemui: 移除焦点通知白名单（参考 HyperFocus 精简方案）
 */
class ABDManagerXposedModule : XposedModule() {

    companion object {
        private const val TAG = "ABDManagerLSP"
    }

    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        Log.i(TAG, "模块已加载，框架: ${getFrameworkName()}")
    }

    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        val classLoader = param.getDefaultClassLoader()

        when (param.packageName) {
            "com.android.systemui" -> {
                Log.i(TAG, "SystemUI 包加载，注册焦点通知白名单移除 Hook")
                registerSystemUIHooks(classLoader)
                hookPluginLoader(classLoader)
            }
        }
    }

    // ==================== SystemUI Hook: 移除焦点通知白名单 ====================

    private fun registerSystemUIHooks(classLoader: ClassLoader) {
        hookAllCanShowFocus(classLoader)
        hookAllCanCustomFocus(classLoader)
        hookAuthManagerCallback(classLoader)
        Log.i(TAG, "SystemUI 焦点通知白名单 Hook 注册完成")
    }

    private fun hookPluginLoader(classLoader: ClassLoader) {
        runCatching {
            val factoryClass = Class.forName(
                "com.android.systemui.shared.plugins.PluginInstance\$PluginFactory",
                false,
                classLoader
            )
            val createPluginContextMethod = factoryClass.declaredMethods.first {
                it.name == "createPluginContext"
            }
            hook(createPluginContextMethod).intercept { callback ->
                val result = callback.proceed()
                runCatching {
                    val wrapper = result as? android.content.ContextWrapper
                    if (wrapper != null) {
                        val pluginClassLoader = wrapper.classLoader
                        val factory = callback.thisObject
                        val componentNameField = factory.javaClass.declaredFields.find {
                            it.type == android.content.ComponentName::class.java
                        }
                        if (componentNameField != null) {
                            componentNameField.isAccessible = true
                            val componentName = componentNameField.get(factory) as? android.content.ComponentName
                            val className = componentName?.className
                            if (className == "miui.systemui.notification.NotificationStatPluginImpl" ||
                                className == "miui.systemui.notification.FocusNotificationPluginImpl") {
                                Log.i(TAG, "焦点通知插件加载: $className，使用 Plugin ClassLoader 注册 Hook")
                                registerSystemUIHooks(pluginClassLoader)
                            }
                        }
                    }
                }.onFailure {
                    Log.w(TAG, "Plugin 加载监听处理失败: ${it.message}")
                }
                result
            }
            Log.i(TAG, "PluginLoader Hook 注册成功")
        }.onFailure {
            Log.w(TAG, "PluginInstance\$PluginFactory 不存在（可能非 HyperOS）: ${it.message}")
        }
    }

    private fun hookAllCanShowFocus(classLoader: ClassLoader) {
        runCatching {
            val clazz = Class.forName(
                "miui.systemui.notification.NotificationSettingsManager",
                false,
                classLoader
            )
            val methods = findMethodsInHierarchy(clazz, "canShowFocus")
            if (methods.isEmpty()) {
                Log.w(TAG, "canShowFocus 方法未找到（沿继承链搜索）")
                return
            }
            methods.forEach { method ->
                hook(method).intercept {
                    Log.d(TAG, "canShowFocus(${method.parameterTypes.joinToString { it.simpleName }}) → 强制返回 true")
                    true
                }
            }
            Log.i(TAG, "Hook 成功: canShowFocus (${methods.size} 个重载，来自 ${methods.map { it.declaringClass.simpleName }.distinct()}) → 始终返回 true")
        }.onFailure {
            Log.w(TAG, "canShowFocus Hook 失败: ${it.message}")
        }
    }

    private fun hookAllCanCustomFocus(classLoader: ClassLoader) {
        runCatching {
            val clazz = Class.forName(
                "miui.systemui.notification.NotificationSettingsManager",
                false,
                classLoader
            )
            val methods = findMethodsInHierarchy(clazz, "canCustomFocus")
            if (methods.isEmpty()) {
                Log.w(TAG, "canCustomFocus 方法未找到（沿继承链搜索）")
                return
            }
            methods.forEach { method ->
                hook(method).intercept {
                    Log.d(TAG, "canCustomFocus(${method.parameterTypes.joinToString { it.simpleName }}) → 强制返回 true")
                    true
                }
            }
            Log.i(TAG, "Hook 成功: canCustomFocus (${methods.size} 个重载，来自 ${methods.map { it.declaringClass.simpleName }.distinct()}) → 始终返回 true")
        }.onFailure {
            Log.w(TAG, "canCustomFocus Hook 失败 (可能系统版本不支持): ${it.message}")
        }
    }

    private fun hookAuthManagerCallback(classLoader: ClassLoader) {
        runCatching {
            val authClass = Class.forName(
                "miui.systemui.notification.auth.AuthManager\$AuthServiceCallback\$onAuthResult\$1",
                false,
                classLoader
            )
            val methods = findMethodsInHierarchy(authClass, "invokeSuspend")
            if (methods.isEmpty()) {
                Log.w(TAG, "AuthManager invokeSuspend 方法未找到（非 HyperOS 3.0+，跳过）")
                return
            }
            val method = methods.first()
            hook(method).intercept { callback ->
                val obj = callback.thisObject
                if (obj != null) {
                    val bundleField = obj.javaClass.declaredFields.find { it.name == "\$authBundle" }
                    if (bundleField != null) {
                        bundleField.isAccessible = true
                        val bundle = bundleField.get(obj) as? Bundle
                        if (bundle != null) {
                            val oldCode = bundle.getInt("result_code", -1)
                            bundle.putInt("result_code", 0)
                            Log.d(TAG, "invokeSuspend: result_code $oldCode → 0（鉴权通过）")
                        }
                    }
                }
                getInvoker(method)
                    .setType(XposedInterface.Invoker.Type.ORIGIN)
                    .invoke(obj, *callback.args.toTypedArray())
            }
            Log.i(TAG, "Hook 成功: AuthManager.invokeSuspend → result_code = 0")
        }.onFailure {
            Log.w(TAG, "AuthManager invokeSuspend Hook 失败（可能不是 HyperOS 3.0+）: ${it.message}")
        }
    }

    private fun findMethodsInHierarchy(clazz: Class<*>, methodName: String): List<java.lang.reflect.Method> {
        val methods = mutableListOf<java.lang.reflect.Method>()
        var c: Class<*>? = clazz
        while (c != null) {
            c.declaredMethods.filter { it.name == methodName }.forEach { methods.add(it) }
            c = c.superclass
        }
        return methods
    }
}
