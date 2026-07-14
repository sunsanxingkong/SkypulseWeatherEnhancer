package com.skypulse.enhancer.ui

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("skypulse_hook_prefs", MODE_PRIVATE)

        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        // 标题
        root.addView(TextView(this).apply {
            text = "南风天气增强模块\nv3.2.27"
            textSize = 20f
            setPadding(0, 0, 0, 32)
        })

        // 开关1: 会员破解
        root.addView(createSwitchRow("破解会员", "hook_premium", true))

        // 说明
        root.addView(TextView(this).apply {
            text = "启用后自动解锁全部会员功能"
            textSize = 13f
            setPadding(60, 0, 0, 24)
        })

        // 开关2: 随机设备ID
        root.addView(createSwitchRow("随机设备ID", "hook_device_id", true))

        root.addView(TextView(this).apply {
            text = "每次启动使用随机设备标识"
            textSize = 13f
            setPadding(60, 0, 0, 24)
        })

        // 提示
        root.addView(TextView(this).apply {
            text = "\n⚠️ 设置后请重启南风天气应用生效"
            textSize = 13f
        })

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun createSwitchRow(label: String, key: String, default: Boolean): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 12, 0, 4)
        }
        row.addView(TextView(this).apply {
            text = label
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val sw = Switch(this).apply {
            isChecked = prefs.getBoolean(key, default)
            setOnCheckedChangeListener { _, v ->
                prefs.edit().putBoolean(key, v).apply()
            }
        }
        row.addView(sw)
        return row
    }
}