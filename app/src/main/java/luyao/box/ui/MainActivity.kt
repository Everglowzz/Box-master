package luyao.box.ui

import android.Manifest
import android.content.pm.PackageManager
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.title_layout.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import luyao.box.*
import luyao.box.about.AboutActivity
import luyao.box.adapter.MainAdapter
import luyao.box.adapter.SpaceItemDecoration
import luyao.box.common.base.BaseActivity
import luyao.box.common.util.AppUtils
import luyao.box.ui.setting.SettingActivity
import java.io.File
import java.util.*


class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
    private val mainAdapter by lazy { MainAdapter() }

    override fun getLayoutResId() = R.layout.activity_main

    override fun initView() {
        setSupportActionBar(mToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
//        val toggle = ActionBarDrawerToggle(
//            this, drawer_layout, mToolbar,
//            R.string.navigation_drawer_open,
//            R.string.navigation_drawer_close
//        )
//        drawer_layout.addDrawerListener(toggle)
//        toggle.syncState()

//        initNavView()
//        checkPermissions()
        initRecycleView()
    }

    private fun initNavView() {
        nav_view.setNavigationItemSelectedListener(this)
        val versionTv = nav_view.getHeaderView(0).findViewById<TextView>(R.id.versionNameTv)
        versionTv.text = String.format("V %s", AppUtils.getAppVersionName(this, packageName))
    }

    private fun initRecycleView() {
        mainRecycleView.run {
            layoutManager = GridLayoutManager(this@MainActivity, 3)
            addItemDecoration(SpaceItemDecoration(dp2px(5)))
            adapter = mainAdapter
        }

        mainAdapter.setOnItemClickListener { _, _, position ->
            if (position == mainAdapter.data.size - 1) {
                toast("敬请期待 !")
            } else
                startActivity(mainAdapter.data[position].clazz)
        }
    }

    override fun initData() {
        mainAdapter.setNewData(MAIN_LIST)
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if ((isExit ==false)) {
                Toast.makeText(this, "再点击一次退出应用", Toast.LENGTH_SHORT).show()
                isExit = true
                val tExit = Timer()
                tExit.schedule(object : TimerTask() {
                    override fun run() {
                        isExit = false
                    }
                }, 2000)
            } else {
                finish()
            }
        }
        return false
    }

    private var isExit: Boolean? = false

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(SettingActivity::class.java)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_setting -> {
                startActivity(SettingActivity::class.java)
            }

            R.id.nav_about -> {
                startActivity(AboutActivity::class.java)
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            createBaseFile()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            MaterialDialog(this).show {
                title(R.string.permission_get)
                message(R.string.permission_note)
                positiveButton { ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), 1001) }
            }
        } else createBaseFile()
    }

    private fun createBaseFile() {
        CoroutineScope(Dispatchers.IO).launch {
            val baseFolder = File(BASE_PATH)
            if (!baseFolder.exists()) baseFolder.mkdirs()

            val apkFolder = File(APK_PATH)
            if (!apkFolder.exists()) apkFolder.mkdirs()
        }
    }
}
