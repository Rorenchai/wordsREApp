package com.example.study

import android.app.Application

/** 应用入口：创建全局依赖容器。 */
class StudyApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
