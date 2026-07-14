package com.skypulse.enhancer.hook

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

/**
 * 南风天气 v3.2.27 Hook 入口
 *
 * 混淆类映射:
 *   m4.f    = 会员管理 ViewModel
 *   m4.f$b  = 设备ID工具类
 *   m4.a    = 状态枚举(f11340a=SUCCESS)
 */
class MainHook : XposedModule() {

    companion object {
        private const val TAG = "SkyEnhancer"
        private const val TARGET = "com.skypulse.weather"
        private const val PREFS = "skypulse_hook_prefs"
        private const val KEY_PREMIUM = "hook_premium"
        private const val KEY_DEVICE = "hook_device_id"

        private val CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray()
        private var cachedId: String? = null

        @Synchronized
        fun randomId(): String {
            if (cachedId == null) cachedId = (1..8).map { CHARS.random() }.joinToString("")
            return cachedId!!
        }
    }

    private var done = false

    override fun onModuleLoaded(p: ModuleLoadedParam) {
        log(Log.INFO, TAG, "南风天气增强模块加载 [API $apiVersion]")
    }

    override fun onPackageLoaded(p: PackageLoadedParam) {
        if (p.packageName != TARGET) return
        log(Log.INFO, TAG, "南风天气 v3.2.27 进程加载")
    }

    override fun onPackageReady(p: PackageReadyParam) {
        if (p.packageName != TARGET || !p.isFirstPackage || done) return
        done = true
        val cl = p.classLoader

        log(Log.INFO, TAG, "===== Hook开始 =====")

        try { hookPremium(cl) } catch (e: Exception) { log(Log.ERROR, TAG, "会员Hook失败", e) }
        try { hookDeviceId(cl) } catch (e: Exception) { log(Log.ERROR, TAG, "设备IDHook失败", e) }
        try { hookActivation(cl) } catch (e: Exception) { log(Log.ERROR, TAG, "激活码Hook失败", e) }
        try { hookApp(cl) } catch (e: Exception) { log(Log.ERROR, TAG, "AppInit失败", e) }

        log(Log.INFO, TAG, "===== Hook完成 =====")
    }

    // ============ 开关检查 ============
    private fun isOn(ctx: Any?, key: String, def: Boolean): Boolean {
        return try {
            if (ctx is Context) ctx.getSharedPreferences(PREFS, 0).getBoolean(key, def) else def
        } catch (_: Exception) { def }
    }

    private fun getCtx(obj: Any?): Context? {
        return try {
            val f = obj?.javaClass?.getDeclaredField("a")
            f?.isAccessible = true
            f?.get(obj) as? Context
        } catch (_: Exception) { null }
    }

    // ============ 1. 会员破解: m4.f.i() → true ============
    private fun hookPremium(cl: ClassLoader) {
        val cls = Class.forName("m4.f", true, cl)
        for (m in cls.declaredMethods) {
            if (m.name == "i" && m.parameterTypes.isEmpty() &&
                (m.returnType == Boolean::class.javaPrimitiveType || m.returnType == Boolean::class.javaObjectType)) {
                hook(m).intercept { chain ->
                    val ctx = getCtx(chain.thisObject)
                    if (isOn(ctx, KEY_PREMIUM, true)) {
                        log(Log.INFO, TAG, "[会员] m4.f.i() → true")
                        true
                    } else chain.proceed()
                }
                log(Log.INFO, TAG, "[会员] ✅ m4.f.i()")
                return
            }
        }
    }

    // ============ 2. 设备ID随机: m4.f$b.c(Context) + m4.f.f() ============
    private fun hookDeviceId(cl: ClassLoader) {
        // 2a. m4.f$b.c(Context) → 设备ID生成
        try {
            val cls = Class.forName("m4.f\$b", true, cl)
            for (m in cls.declaredMethods) {
                if (m.name == "c" && m.parameterTypes.size == 1 &&
                    m.parameterTypes[0] == Context::class.java) {
                    hook(m).intercept { chain ->
                        val ctx = chain.args[0] as? Context
                        if (isOn(ctx, KEY_DEVICE, true)) {
                            val id = randomId()
                            log(Log.INFO, TAG, "[设备ID] 生成 → $id")
                            id
                        } else chain.proceed()
                    }
                    log(Log.INFO, TAG, "[设备ID] ✅ m4.f\$b.c()")
                    break
                }
            }
        } catch (_: Exception) {}

        // 2b. m4.f.f() → 读取设备ID
        try {
            val cls = Class.forName("m4.f", true, cl)
            for (m in cls.declaredMethods) {
                if (m.name == "f" && m.parameterTypes.isEmpty() &&
                    m.returnType == String::class.java) {
                    hook(m).intercept { chain ->
                        val ctx = getCtx(chain.thisObject)
                        if (isOn(ctx, KEY_DEVICE, true)) {
                            val id = randomId()
                            log(Log.INFO, TAG, "[设备ID] 读取 → $id")
                            id
                        } else chain.proceed()
                    }
                    log(Log.INFO, TAG, "[设备ID] ✅ m4.f.f()")
                    break
                }
            }
        } catch (_: Exception) {}
    }

    // ============ 3. 激活码跳过: m4.f.c(String) → SUCCESS ============
    private fun hookActivation(cl: ClassLoader) {
        val cls = Class.forName("m4.f", true, cl)
        for (m in cls.declaredMethods) {
            if (m.name == "c" && m.parameterTypes.size == 1 &&
                m.parameterTypes[0] == String::class.java) {
                // 取 m4.a 枚举的 SUCCESS 值
                val enumCls = Class.forName("m4.a", true, cl)
                val succ = enumCls.declaredFields.firstOrNull {
                    it.type == enumCls
                }?.also { it.isAccessible = true }?.get(null)
                    ?: return

                hook(m).intercept { chain ->
                    val ctx = getCtx(chain.thisObject)
                    if (isOn(ctx, KEY_PREMIUM, true)) {
                        log(Log.INFO, TAG, "[激活] m4.f.c() → SUCCESS")
                        succ
                    } else chain.proceed()
                }
                log(Log.INFO, TAG, "[激活] ✅ m4.f.c()")
                return
            }
        }
    }

    // ============ 4. App初始化: SkyPulseApp.onCreate() ============
    private fun hookApp(cl: ClassLoader) {
        val cls = Class.forName("com.skypulse.weather.SkyPulseApp", true, cl)
        for (m in cls.declaredMethods) {
            if (m.name == "onCreate" && m.parameterTypes.isEmpty()) {
                hook(m).intercept { chain ->
                    chain.proceed()
                    try {
                        val ctx = chain.thisObject as? Context ?: return@intercept null
                        ctx.getSharedPreferences(PREFS, 0).edit().apply {
                            if (!contains(KEY_PREMIUM)) putBoolean(KEY_PREMIUM, true)
                            if (!contains(KEY_DEVICE)) putBoolean(KEY_DEVICE, true)
                        }.apply()
                        log(Log.INFO, TAG, "[初始化] 默认开关写入")
                    } catch (_: Exception) {}
                    null
                }
                log(Log.INFO, TAG, "[初始化] ✅ SkyPulseApp")
                return
            }
        }
    }
}