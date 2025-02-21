package com.lollipop.qin1sptools.activity.games

import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.lollipop.qin1sptools.R
import com.lollipop.qin1sptools.activity.FileChooseActivity
import com.lollipop.qin1sptools.activity.base.GridMenuActivity
import com.lollipop.qin1sptools.dialog.MessageDialog
import com.lollipop.qin1sptools.event.KeyEvent
import com.lollipop.qin1sptools.guide.Guide
import com.lollipop.qin1sptools.menu.GridMenu
import com.lollipop.qin1sptools.utils.FeatureIcon
import com.lollipop.qin1sptools.utils.delay
import com.lollipop.qin1sptools.utils.doAsync
import com.lollipop.qin1sptools.utils.onUI
import com.lollipop.qin1sptools.utils.requestStoragePermissions
import ru.playsoftware.j2meloader.applist.AppItem
import ru.playsoftware.j2meloader.appsdb.AppRepository
import ru.playsoftware.j2meloader.util.AppUtils
import ru.playsoftware.j2meloader.util.JarConverter
import java.io.File
import javax.microedition.util.ContextHolder

class J2meActivity : GridMenuActivity() {

    companion object {
        private const val JAR_FILTER = ".*\\.[jJ][aA][rRdD]\$"

        private const val OPTION_ID_FILE = 0
        private const val OPTION_ID_PRESET = 1
    }

    override val baseFeatureIconArray = arrayOf(
        FeatureIcon.OPTION,
        FeatureIcon.NONE,
        FeatureIcon.BACK
    )

    private val gameList = ArrayList<AppItem>()

    private val appRepository by lazy {
        AppRepository(this)
    }

    private val converter by lazy {
        JarConverter(applicationInfo.dataDir)
    }

    private var deleteDialog: MessageDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initData()

        requestStoragePermissions()

        val thisIntent = intent
        val uri = thisIntent.data
        if (thisIntent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0
            && savedInstanceState == null && uri != null
        ) {
            decodeByIntent(uri)
        }
    }

    private fun initData() {
        gridItemList.clear()
        notifyDataSetChanged()
        startLoading()
        doAsync(::onError) {
            updateGameList()
            onUI {
                endLoading()
                notifyDataSetChanged()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data?.let { uri ->
            decodeByIntent(uri)
        }
    }

    private fun decodeByIntent(uri: Uri) {
        convertJar(uri)
    }

    private fun updateGameList() {
        val apps = appRepository.all
        AppUtils.updateDb(appRepository, apps)

        val defaultIcon = ContextCompat.getDrawable(this, R.mipmap.ic_launcher_java)!!

        val menuList = ArrayList<GridMenu.GridItem>()
        apps.forEach { app ->
            try {
                menuList.add(
                    GridMenu.GridItem(
                        app.id,
                        Drawable.createFromPath(app.imagePathExt) ?: defaultIcon,
                        app.title
                    )
                )
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        gridItemList.clear()
        gridItemList.addAll(menuList)
        gameList.clear()
        gameList.addAll(apps)
    }

    private fun onError(error: Throwable) {
        error.printStackTrace()
        endLoading()
        gridItemList.clear()
        setTitle(R.string.error)
        notifyDataSetChanged()
    }

    override fun onGridItemClick(item: GridMenu.GridItem, index: Int) {
        val id = item.id
        val gameInfo = gameList.find { it.id == id } ?: return
        updateWindowInsets()
        MicroDisplayActivity.start(
            this,
            gameInfo.title,
            gameInfo.pathExt,
            ContextHolder.getStatusBarSize()
        )
    }

    private fun callDeleteApp(gameInfo: AppItem): Boolean {
        deleteDialog = MessageDialog.build(this) {
            message = getString(R.string.dialog_msg_delete_java_game, gameInfo.title)
            setLeftButton(R.string.delete) {
                deleteApp(gameInfo)
                it.dismiss()
                deleteDialog = null
            }
            setRightButton(R.string.cancel) {
                it.dismiss()
                deleteDialog = null
            }
        }
        deleteDialog?.show()
        return true
    }

    private fun deleteApp(gameInfo: AppItem) {
        startLoading()
        resetCurrentSelected()
        doAsync(::onError) {
            AppUtils.deleteApp(gameInfo)
            appRepository.delete(gameInfo)
            updateGameList()
            onUI {
                endLoading()
                notifyDataSetChanged()
            }
        }
    }

    private fun updateWindowInsets() {
        val rectangle = Rect()
        window.decorView.getWindowVisibleDisplayFrame(rectangle)
        ContextHolder.setStatusBarSize(rectangle.top)
    }

    override fun onGridItemInfoClick(item: GridMenu.GridItem?, index: Int) {
        item ?: return
        val id = item.id
        val gameInfo = gameList.find { it.id == id } ?: return
        callDeleteApp(gameInfo)
    }

    override fun onLeftFeatureButtonClick(): Boolean {
        startFileChoose()
        /*OptionDialog.build(this) {
            setTitle(R.string.menu)
            dataList.clear()
            add(OptionDialog.Item(getString(R.string.add_from_preset), OPTION_ID_PRESET))
            add(OptionDialog.Item(getString(R.string.add_from_file), OPTION_ID_FILE))
            addMenuModeOption(this)
            setLeftButton(R.string.ok) {
                if (it is OptionDialog) {
                    val selectedPosition = it.selectedPosition
                    if (selectedPosition in dataList.indices) {
                        val id = dataList[selectedPosition].id
                        when (id) {
                            OPTION_ID_FILE -> {
                                startFileChoose()
                            }

                            OPTION_ID_PRESET -> {
                                startPresetChoose()
                            }

                            else -> {
                                setMenuMode(id)
                                startActivity(Intent(this@J2meActivity, J2meActivity::class.java))
                                finish()
                            }
                        }
                    }
                }
                it.dismiss()
            }
            setRightButton(R.string.cancel) {
                it.dismiss()
            }
        }.show()*/
        return true
    }

    private fun startFileChoose() {
        FileChooseActivity.start(this@J2meActivity, filter = JAR_FILTER, chooseFile = true)
    }

    private fun startPresetChoose() {
        PresetJarActivity.start(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (FileChooseActivity.isResult(requestCode)) {
            val resultFile = FileChooseActivity.getResultFile(resultCode, data)
            if (resultFile != null) {
                convertJar(resultFile)
            }
            return
        } else if (PresetJarActivity.isResult(requestCode)) {
            val resultFile = PresetJarActivity.getResultFile(resultCode, data)
            if (resultFile != null) {
                convertJar(resultFile)
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onStop() {
        resetCurrentSelected()
        super.onStop()
    }

    private fun convertJar(uri: Uri) {
        delay(200) {
            startLoading()
            setFeatureButtons(FeatureIcon.NONE, FeatureIcon.NONE, FeatureIcon.NONE)
            doAsync({
                showToast(R.string.convert_error)
                endLoading()
            }) {
                val gameDir = converter.convert(uri)
                val app = AppUtils.getApp(gameDir)
                appRepository.insert(app)
                updateGameList()
                onUI {
                    endLoading()
                    notifyDataSetChanged()
                    setFeatureButtons()
                }
            }
        }
    }

    private fun convertJar(file: File) {
        convertJar(Uri.fromFile(file))
    }

    override fun buildGuide(builder: Guide.Builder) {
        builder.next(KeyEvent.OPTION, R.string.guide_menu_jar)
            .next(KeyEvent.UP, R.string.guide_browser_direction)
            .next(KeyEvent.KEY_5, R.string.guide_grid_num)
            .next(KeyEvent.KEY_0, R.string.guide_delete_java_app)
            .next(KeyEvent.KEY_STAR, R.string.guide_browser_last_page)
            .next(KeyEvent.KEY_POUND, R.string.guide_browser_next_page)
        super.buildGuide(builder)
    }

}