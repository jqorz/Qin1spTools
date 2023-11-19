package com.lollipop.qin1sptools.menu

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.viewbinding.ViewBinding
import com.lollipop.qin1sptools.R
import com.lollipop.qin1sptools.databinding.ActivityGridMenuBinding
import com.lollipop.qin1sptools.databinding.ItemGridMenuBinding
import com.lollipop.qin1sptools.event.KeyEvent
import com.lollipop.qin1sptools.utils.lazyBind
import com.lollipop.qin1sptools.utils.withThis
import com.lollipop.qin1sptools.view.NineGridsChild
import com.lollipop.qin1sptools.view.NineGridsLayout
import kotlin.math.min

class GridMenuManagerV1(
    activity: Activity,
    gridItemListProvider: () -> List<GridItem>,
    onGridItemClickCallback: (item: GridItem, index: Int) -> Unit,
    onGridItemInfoClickCallback: (item: GridItem?, index: Int) -> Unit
) : GridMenu(activity, gridItemListProvider, onGridItemClickCallback, onGridItemInfoClickCallback) {

    companion object {
        private const val DEFAULT_ITEM_POSITION = -1
        private val ITEM_ROW_EACH_PAGE = 3
        private val ITEM_COLUME_EACH_PAGE = 3
        private val ITEM_COUNT_EACH_PAGE = ITEM_ROW_EACH_PAGE * ITEM_COLUME_EACH_PAGE
    }

    val binding: ActivityGridMenuBinding by activity.lazyBind()

    private val viewRecycler = ViewRecycler()

    // from 1 to 9, not from 0
    private var selectedItemNum = DEFAULT_ITEM_POSITION
        set(value) {
            field = value
            onSelectedItemChanged(value)
        }

    private var pageIndex = 0

    private val pageCount: Int
        get() {
            return Math.ceil(gridItemList.size / ITEM_COUNT_EACH_PAGE.toDouble()).toInt()
        }

    private val currentPageShowCount: Int
        get() {
            if (pageIndex == pageCount - 1) {
                return gridItemList.size - pageIndex * ITEM_COUNT_EACH_PAGE
            } else {
                return ITEM_COUNT_EACH_PAGE
            }
        }

    override val rootView: ViewBinding
        get() {
            return binding
        }

    override fun onCreate() {
        initView()
    }

    private fun initView() {
        notifyDataSetChanged()
    }

    private fun onPageChanged() {
        binding.indicatorView.onPageChanged(pageIndex, pageCount)
    }

    override fun notifyDataSetChanged() {
        pageIndex = 0
        updateGridPage()
    }

    private fun updateGridPage() {
        val itemSize = gridItemList.size
        val gridLayout = binding.gridLayout
        if (itemSize == 0) {
            viewRecycler.recycle(gridLayout)
            return
        }
        bindGridItem(gridLayout, gridItemList, pageIndex * ITEM_COUNT_EACH_PAGE)
        onPageChanged()
    }

    private fun bindGridItem(pageView: NineGridsLayout, itemList: List<GridItem>, offset: Int) {
        val itemCount = min(min(offset + ITEM_COUNT_EACH_PAGE, itemList.size) - offset, ITEM_COUNT_EACH_PAGE)
        val space = activity.resources.getDimensionPixelSize(R.dimen.grid_menu_space)
        pageView.setPadding(space, space, space, space)
        pageView.childSpace = space
        viewRecycler.recycle(pageView)
        for (index in 0 until itemCount) {
            val itemHolder = viewRecycler.find {
                GridHolder(pageView.context)
            }
            itemHolder.bind(itemList[index + offset])
            pageView.addView(itemHolder)
        }
        pageView.notifyChildIndexChanged()
    }

    override fun resetCurrentSelected() {
        selectedItemNum = DEFAULT_ITEM_POSITION
        binding.gridLayout.resetSelectedFlag()
    }

    override fun getCurrentSelected(): GridItem {
        val itemIndex = getItemIndexByPosition(selectedItemNum)
        return gridItemList.get(itemIndex)
    }

    override fun onKeyUp(event: KeyEvent, repeatCount: Int): Boolean {
        when (event) {
            KeyEvent.CENTER, KeyEvent.CALL -> {
                val position = if (selectedItemNum == DEFAULT_ITEM_POSITION) {
                    5
                } else {
                    selectedItemNum
                }
                onNumberClick(position)
            }

            KeyEvent.LEFT -> {
                onLeftClick()
            }

            KeyEvent.UP -> {
                onUpClick()
            }

            KeyEvent.RIGHT -> {
                onRightClick()
            }

            KeyEvent.DOWN -> {
                onDownClick()
            }

            KeyEvent.KEY_1 -> {
                onNumberClick(1)
            }

            KeyEvent.KEY_2 -> {
                onNumberClick(2)
            }

            KeyEvent.KEY_3 -> {
                onNumberClick(3)
            }

            KeyEvent.KEY_4 -> {
                onNumberClick(4)
            }

            KeyEvent.KEY_5 -> {
                onNumberClick(5)
            }

            KeyEvent.KEY_6 -> {
                onNumberClick(6)
            }

            KeyEvent.KEY_7 -> {
                onNumberClick(7)
            }

            KeyEvent.KEY_8 -> {
                onNumberClick(8)
            }

            KeyEvent.KEY_9 -> {
                onNumberClick(9)
            }

            KeyEvent.KEY_STAR -> {
                switchToLastPage()
            }

            KeyEvent.KEY_0 -> {
                onInfoClick()
            }

            KeyEvent.KEY_POUND -> {
                switchToNextPage()
            }

            else -> {
                return false
            }
        }
        return true
    }


    private fun onLeftClick() {
        if (selectedItemNum > 1) {
            selectedItemNum--
            refreshViewChildIndex()
        }
    }

    private fun onUpClick() {
        if (selectedItemNum > ITEM_COLUME_EACH_PAGE) {
            selectedItemNum = selectedItemNum - ITEM_COLUME_EACH_PAGE
            refreshViewChildIndex()
        }
    }

    private fun onRightClick() {
        if (selectedItemNum < currentPageShowCount) {
            selectedItemNum++
            refreshViewChildIndex()
        }
    }

    private fun onDownClick() {
        if (selectedItemNum <= currentPageShowCount - ITEM_COLUME_EACH_PAGE) {
            selectedItemNum = selectedItemNum + ITEM_COLUME_EACH_PAGE
            refreshViewChildIndex()
        }
    }

    private fun switchToNextPage() {
        if (pageIndex < pageCount - 1) {
            pageIndex++
            updateGridPage()
            if (currentPageShowCount > 6) {
                selectedItemNum = 5
            } else {
                selectedItemNum = 1
            }
            refreshViewChildIndex()
        }
    }

    private fun switchToLastPage() {
        if (pageIndex > 0) {
            pageIndex--
            updateGridPage()
            selectedItemNum = 5
            refreshViewChildIndex()
        }
    }

    private fun View.resetSelectedFlag() {
        this.let {
            if (it is NineGridsLayout) {
                it.selectedChildIndex = DEFAULT_ITEM_POSITION
            }
        }
    }

    private fun refreshViewChildIndex() {
        binding.gridLayout.let {
            it.selectedChildIndex = selectedItemNum - 1
        }
    }

    private fun onSelectedItemChanged(position: Int) {
        val itemIndex = getItemIndexByPosition(position)
        if (itemIndex < 0) {
            activity.setTitle(R.string.app_name)
        } else {
            activity.title = gridItemList[itemIndex].label
        }
    }

    private fun onNumberClick(position: Int) {
        val pageView = binding.gridLayout
        val itemIndex = getItemIndexByPosition(position)
        if (itemIndex < 0) {
            return
        }
        if (pageView.selectedChildIndex >= 0 && pageView.selectedChildIndex == position - 1) {
            onGridItemClick(gridItemList[itemIndex], itemIndex)
        } else {
            selectedItemNum = position
            pageView.selectedChildIndex = position - 1
        }
    }

    private fun onInfoClick() {
        val pageView = binding.gridLayout
        if (selectedItemNum < 0) {
            onGridItemInfoClick(null, -1)
            return
        }
        val itemIndex = getItemIndexByPosition(selectedItemNum)
        if (itemIndex < 0) {
            onGridItemInfoClick(null, -1)
            return
        }
        if (pageView.selectedChildIndex >= 0 && pageView.selectedChildIndex == selectedItemNum - 1) {
            onGridItemInfoClick(gridItemList[itemIndex], itemIndex)
        } else {
            onGridItemInfoClick(null, -1)
        }
    }

    protected fun getSelectedItem(): GridItem? {
        val pageView = binding.gridLayout
        val position = selectedItemNum
        val itemIndex = getItemIndexByPosition(position)
        if (itemIndex < 0) {
            return null
        }
        if (pageView.selectedChildIndex >= 0 && pageView.selectedChildIndex == position - 1) {
            return gridItemList[itemIndex]
        }
        return null
    }

    private fun getItemIndexByPosition(position: Int): Int {
        if (position < 1) {
            return -1
        }
        val pageView = binding.gridLayout
        if (pageView.childCount < position) {
            return -1
        }
        val dataPageIndex = pageIndex
        val itemIndex = dataPageIndex * ITEM_COUNT_EACH_PAGE + position - 1
        if (itemIndex < 0 || itemIndex >= gridItemList.size) {
            return -1
        }
        return itemIndex
    }

    override fun onDestroy() {
        viewRecycler.destroy()
    }

    private class GridHolder(context: Context) : FrameLayout(context), NineGridsChild {

        private val binding: ItemGridMenuBinding by withThis(true)

        init {
            binding.shapeGroup.setRoundShapeDp(10)
        }

        @SuppressLint("SetTextI18n")
        override fun setGridIndex(index: Int) {
            binding.positionView.text = "${index + 1}"
        }

        override fun setSelected(isSelected: Boolean, selectedScale: Float): Boolean {
            binding.selectedFrameView.isShow = isSelected
            return true
        }

        fun bind(item: GridItem) {
            binding.iconView.setImageDrawable(item.icon)
        }

    }

    private class ViewRecycler {
        val viewPool = ArrayList<View>()

        fun recycle(viewGroup: ViewGroup) {
            // 暂时放弃回收View
            val childCount = viewGroup.childCount
            for (index in 0 until childCount) {
                viewPool.add(viewGroup.getChildAt(index))
            }
            viewGroup.removeAllViewsInLayout()
        }

        inline fun <reified T : View> find(viewCreate: () -> T): T {
            for (view in viewPool) {
                if (view is T) {
                    viewPool.remove(view)
                    return view
                }
            }
            return viewCreate()
        }

        fun destroy() {
            viewPool.clear()
        }

    }

}