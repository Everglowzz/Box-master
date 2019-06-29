package luyao.box

import android.app.Application
import android.content.Context
import kotlin.properties.Delegates

/**
 * Created by luyao
 * on 2018/12/29 13:33
 */
class App : Application() {

    companion object {
        var CONTEXT: Context by Delegates.notNull()
    }

    override fun onCreate() {
        super.onCreate()
        CONTEXT = applicationContext

    }

}