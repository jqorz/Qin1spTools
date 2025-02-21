package com.lollipop.qin1sptools.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import com.lollipop.qin1sptools.R
import com.lollipop.qin1sptools.activity.base.SimpleListActivity
import com.lollipop.qin1sptools.event.KeyEvent
import com.lollipop.qin1sptools.guide.Guide
import com.lollipop.qin1sptools.utils.ActionIndexer
import com.lollipop.qin1sptools.utils.FeatureIcon
import com.lollipop.qin1sptools.utils.doAsync
import com.lollipop.qin1sptools.utils.onUI
import java.io.File
import java.text.Collator
import java.util.LinkedList
import java.util.Locale
import java.util.regex.Pattern


/**
 * 文件选择的Activity
 * 可以按照要求选中一个文件夹或者某个文件
 * @author Lollipop
 * @date 2021/07/23
 */
class FileChooseActivity : SimpleListActivity() {

    companion object {
        private const val KEY_ROOT_DIR = "KEY_ROOT_DIR"
        private const val KEY_FILE_FILTER = "KEY_FILE_FILTER"
        private const val KEY_CHOOSE_FILE = "KEY_CHOOSE_FILE"

        private const val REQUEST_CODE = 0xF11E

        private const val KEY_SELECTED_FILE = "KEY_SELECTED_FILE"

        fun isResult(requestCode: Int): Boolean {
            return REQUEST_CODE == requestCode
        }

        fun start(
            activity: Activity,
            requestCode: Int = REQUEST_CODE,
            rootDir: String = "",
            filter: String = "",
            chooseFile: Boolean = false
        ) {
            activity.startActivityForResult(
                Intent(activity, FileChooseActivity::class.java).apply {
                    putExtra(KEY_FILE_FILTER, filter)
                    putExtra(KEY_ROOT_DIR, rootDir)
                    putExtra(KEY_CHOOSE_FILE, chooseFile)
                },
                requestCode
            )
        }

        fun getResultFile(resultCode: Int, intent: Intent?): File? {
            if (resultCode != RESULT_OK || intent == null) {
                return null
            }
            val file = intent.getSerializableExtra(KEY_SELECTED_FILE) ?: return null
            if (file is File) {
                return file
            }
            return null
        }

        private fun setResultFile(activity: Activity, file: File) {
            activity.setResult(RESULT_OK, Intent().apply {
                putExtra(KEY_SELECTED_FILE, file)
            })
        }

    }

    override val baseFeatureIconArray = arrayOf(
        FeatureIcon.OK,
        FeatureIcon.SELECT,
        FeatureIcon.BACK
    )

    private val breadCrumbs = LinkedList<DirInfo>()

    private val currentFiles = ArrayList<String>()

    private val actionIndexer = ActionIndexer()

    private val fileFilter by lazy {
        intent.getStringExtra(KEY_FILE_FILTER)
    }

    private val chooseFile by lazy {
        intent.getBooleanExtra(KEY_CHOOSE_FILE, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initData()
    }

    private fun initData() {
        //获取存储状态
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            setError(getString(R.string.external_storage_not_found))
            return
        }
        val rootDirPath = intent.getStringExtra(KEY_ROOT_DIR) ?: ""
        val root = if (rootDirPath.isEmpty()) {
            Environment.getExternalStorageDirectory()
        } else {
            File(rootDirPath)
        }
        //获取根目录
        next(root)
    }

    private fun next(dir: File) {
        if (!dir.canRead() || !dir.canWrite()) {
            if (breadCrumbs.isEmpty()) {
                setError(getString(R.string.external_storage_not_found))
                return
            }
            setError(getString(R.string.folder_unavailable))
            return
        }
        actionIndexer.newAction()
        clearError()
        currentFiles.clear()
        startLoading()
        updateData()
        doAsync {
            val nowAction = actionIndexer.now
            val pattern: Pattern? = fileFilter?.let {
                if (it.isEmpty()) {
                    null
                } else {
                    Pattern.compile(it)
                }
            }
            val childrenFileList = ArrayList<File>()
            if (dir.isDirectory) {
                dir.listFiles()?.forEach {
                    var needAdd = false
                    if (it.isDirectory) {
                        needAdd = true
                    } else if (pattern != null) {
                        if (pattern.matcher(it.name).matches()) {
                            needAdd = true
                        }
                    } else {
                        needAdd = true
                    }
                    if (needAdd) {
                        childrenFileList.add(it)
                    }
                }
            }
            childrenFileList.sortWith(compareByDescending<File> { it.isDirectory }.thenComparator { a, b ->
                Collator.getInstance(Locale.CHINA).compare(a.name, b.name)
            })
            val childrenList = childrenFileList.map { (if (it.isDirectory) " > " else "") + it.name }
            if (actionIndexer.active(nowAction)) {
                breadCrumbs.addLast(DirInfo(dir, 0, childrenList, childrenFileList))
                currentFiles.addAll(childrenList)
                onUI {
                    updateData()
                    endLoading()
                    updateTitle()
                }
            }
        }
    }

    private fun back(): Boolean {
        if (breadCrumbs.isEmpty()) {
            return false
        }
        breadCrumbs.removeLast()
        if (breadCrumbs.isEmpty()) {
            return false
        }
        actionIndexer.newAction()
        val last = breadCrumbs.last
        currentFiles.clear()
        currentFiles.addAll(last.children)
        updateData()
        selectedTo(last.selectedPosition)
        updateTitle()
        return true
    }

    private fun updateData() {
        setData(currentFiles)
    }

    override fun onKeyUp(event: KeyEvent, repeatCount: Int): Boolean {
        if (isLoading && event == KeyEvent.BACK) {
            back()
            return true
        }
        when (event) {
            KeyEvent.LEFT -> {
                back()
                return true
            }

            KeyEvent.RIGHT -> {
                return toNextDir()
            }

            else -> {

            }
        }
        return super.onKeyUp(event, repeatCount)
    }

    override fun onCenterFeatureButtonClick(): Boolean {
        if (toNextDir()) {
            return true
        }
        return super.onCenterFeatureButtonClick()
    }

    private fun toNextDir(): Boolean {
        val index = selectedIndex
        if (index >= 0 && index < currentFiles.size) {
            val last = breadCrumbs.last ?: return true
            val childrenFile = last.childrenFile
            if (childrenFile.size == currentFiles.size) {
                val file = childrenFile[index]
                if (!file.isDirectory) {
                    if (chooseFile) {
                        choose()
                    }
                    return true
                }
                next(file)
                last.selectedPosition = index
                return true
            }
        }
        return false
    }

    private fun updateTitle() {
        if (breadCrumbs.isEmpty()) {
            setTitle(R.string.add_from_file)
            return
        }
        val last = breadCrumbs.last
        if (last == breadCrumbs.first) {
            setTitle(R.string.add_from_file)
            return
        }
        title = last.file.name
    }

    override fun onRightFeatureButtonClick(): Boolean {
        return back() || super.onRightFeatureButtonClick()
    }

    override fun onLeftFeatureButtonClick(): Boolean {
        choose()
        return true
    }

    private fun choose() {
        if (breadCrumbs.isEmpty()) {
            return
        }
        val last = breadCrumbs.last
        val index = selectedIndex
        if (last.childrenFile.size <= index) {
            return
        }
        val file: File
        if (chooseFile) {
            file = last.childrenFile[index]
            if (file.isDirectory) {
                toNextDir()
                return
            }
        } else {
            file = last.file
        }
        setResultFile(this, file)
        finish()
        return
    }

    override fun buildGuide(builder: Guide.Builder) {
        builder.next(KeyEvent.UP, R.string.guide_file_up)
            .next(KeyEvent.DOWN, R.string.guide_file_down)
            .next(KeyEvent.CENTER, R.string.guide_file_center)
            .next(KeyEvent.OPTION, R.string.guide_file_option)
            .next(KeyEvent.BACK, R.string.guide_file_back)
        super.buildGuide(builder)
    }

    private class DirInfo(
        val file: File,
        var selectedPosition: Int = 0,
        val children: List<String>,
        val childrenFile: List<File>
    )

}