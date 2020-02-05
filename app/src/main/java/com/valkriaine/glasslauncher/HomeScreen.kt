package com.valkriaine.glasslauncher

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.*
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.*
import android.widget.AdapterView.AdapterContextMenuInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager
import jp.wasabeef.recyclerview.adapters.ScaleInAnimationAdapter
import jp.wasabeef.recyclerview.animators.ScaleInBottomAnimator
import kotlinx.android.synthetic.main.activity_home_screen.*
import no.danielzeller.blurbehindlib.BlurBehindLayout
import no.danielzeller.blurbehindlib.UpdateMode


class HomeScreen : AppCompatActivity() {


    private val key = "APPTILELISTPREFERENCE"
    private val timeUnit : Long = 0.000001.toLong()
    private var xOffset : Float = 0f
    private lateinit var wm : WallpaperManager
    lateinit var pager : HomePager
    lateinit var pagerAdapter: SectionsPagerAdapter
    private lateinit var blurry : BlurBehindLayout
    private lateinit var dim : GridLayout
    private lateinit var appGrid : GridView
    lateinit var arrowButton : ImageButton
    lateinit var blurFrame : ConstraintLayout
    private lateinit var widgetManager: AppWidgetManager
    private lateinit var broadcastReceiver: BroadcastReceiver
    private lateinit var sharedPreferences: SharedPreferences
    private val itemTouchHelper by lazy{
        val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(UP or DOWN or START or END, 0) {

            override fun onMove(recyclerView: RecyclerView,
                                viewHolder: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                viewModel.moveTile(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int)
            {
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.alpha = 0.5f
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)

                viewHolder.itemView.alpha = 1.0f
            }
        }
        ItemTouchHelper(simpleItemTouchCallback)
    }

    companion object
    {
        lateinit var background : ImageView
        lateinit var tileList : RecyclerView
        lateinit var blur : ConstraintLayout
        lateinit var viewModel : ViewModel
    }
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_screen)

        sharedPreferences = getSharedPreferences(key, Context.MODE_PRIVATE)
        viewModel = ViewModel(this, packageManager)


        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when(intent.action) {
                    Intent.ACTION_PACKAGE_ADDED -> {
                        val appAdded = context.packageManager.getApplicationInfo(intent.dataString!!.substring(8), 0)
                        val appIntentAdded = context.packageManager.getLaunchIntentForPackage(appAdded.packageName)
                        if(appIntentAdded != null && appAdded.packageName != BuildConfig.APPLICATION_ID) {
                            viewModel.allApps.apply {
                                add(AppInfo(
                                    appAdded.loadLabel(context.packageManager).toString(),
                                    appAdded.loadIcon(context.packageManager),
                                    appAdded.packageName
                                ))
                                sort()
                            }
                        }
                        viewModel.notifyAppListDataChange()
                    }

                    Intent.ACTION_PACKAGE_REMOVED -> {
                        viewModel.allApps.removeByPackage(
                            intent.dataString!!.substring(8)
                        )
                        viewModel.tiles.removeByPackageName(
                            intent.dataString!!.substring(8)
                        )
                        viewModel.notifyAppListDataChange()
                    }
                }
            }
        }.also { broadcastReceiver = it }, IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addDataScheme("package")
            }
        )

        widgetManager = AppWidgetManager.getInstance(this)



        linkComponents()
        setUpPager()
        setUpAppList()
        setUpTileList()
        hideNavigationBar()


        itemTouchHelper.attachToRecyclerView(tileList)
        tileList.itemAnimator = ScaleInBottomAnimator()



    }
    fun ArrayList<AppInfo>.removeByPackage(packageName : String)
    {
        var target : AppInfo? = null
        forEach { app ->
            if (app.name == packageName) target = app
        }
        target?.let { app -> remove(app) } ?: Log.e("removeByPackage", "Not found $packageName in ArrayList<App>")
    }
    fun ArrayList<LiveTile>.removeByPackageName(packageName : String)
    {
        var target : AppInfo? = null
        forEach { app ->
            if (app.name == packageName) target = app
        }
        target?.let { app -> remove(app) } ?: Log.e("removeByPackage", "Not found $packageName in ArrayList<App>")
    }
    override fun onPause()
    {
        super.onPause()
        blurry.disable()
    }
    override fun onResume()
    {
        super.onResume()
        blurry.enable()
        blurry.updateForMilliSeconds(timeUnit)
    }
    override fun onBackPressed()
    {
        if (pager.currentItem == 1)
            pager.setCurrentItem(0, true)
    }
    private fun hideNavigationBar()
    {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
    }
    fun switchpage()
    {
        pager.setCurrentItem(1, true)
    }
    private fun linkComponents()
    {
        this.wm = WallpaperManager.getInstance(applicationContext)
        dim = findViewById(R.id.dim)
        background = findViewById(R.id.image)
        background.setImageDrawable(wm.drawable)
        appGrid = list
        arrowButton = arrowbutton
        tileList = tilelist
        blurFrame = blurframe
        dim.alpha = 0f
        blurry = backgroundblur
        blurry.viewBehind = background
        blur = blurback

    }
    private fun setUpPager()
    {
        pagerAdapter = SectionsPagerAdapter()
        pager = view_pager
        pager.adapter = pagerAdapter
        pager.setAllowedSwipeDirection(SwipeDirection.Right)
        pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageScrollStateChanged(state: Int) {
                if (pager.currentItem == 0) {
                    blurFrame.translationZ = -200f
                    blurry.disable()
                }
                if (pager.currentItem == 1) {
                    blurry.updateMode = UpdateMode.MANUALLY
                }
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                xOffset = (position + positionOffset) / (pagerAdapter.count - 1)
                blurry.enable()
                blurry.updateMode = UpdateMode.ON_SCROLL
                dim.alpha = xOffset/1.5f
                blurry.blurRadius = xOffset * 50
                arrowButton.rotation = 180 + 180*xOffset

            }

            override fun onPageSelected(position: Int) {
                if (position == 0) {
                    pager.setAllowedSwipeDirection(SwipeDirection.Right)
                    arrowButton.rotation = 180f
                } else {
                    pager.setAllowedSwipeDirection(SwipeDirection.Left)
                    arrowButton.rotation = 0f
                }
            }
        })
    }
    private fun setUpAppList()
    {
        appGrid.adapter = viewModel.appsAdapter
        appGrid.setOnItemClickListener{ _, _, position, _ ->
            startActivity(viewModel.launchApp(position))
        }
        registerForContextMenu(appGrid)
        appGrid.setOnCreateContextMenuListener { menu, v, menuInfo ->
            super.onCreateContextMenu(menu, v, menuInfo)
            val info = menuInfo as AdapterContextMenuInfo
            val pos = info.position

            if (!viewModel.checkIfTileExists(viewModel.allApps[pos]))
            menuInflater.inflate(R.menu.applist_menu_add, menu)
            else
                menuInflater.inflate(R.menu.applist_menu_remove, menu)

        }
    }
    private fun setUpTileList()
    {
        tileList.adapter = ScaleInAnimationAdapter(viewModel.recyclerViewAdapter). apply{
            // Change the durations.
            setDuration(350)
            setFirstOnly(true)
        }
        tileList.layoutManager = ChipsLayoutManager.newBuilder(this)
            .setOrientation(ChipsLayoutManager.HORIZONTAL)
            .setChildGravity(Gravity.CENTER)
            .setRowStrategy(ChipsLayoutManager.STRATEGY_DEFAULT)
            .setMaxViewsInRow(2)
            .setScrollingEnabled(true)
            .build()
    }
    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as AdapterContextMenuInfo
        val pos = info.position
        when (item.itemId)
        {
            R.id.addToStart ->
            {
                pager.setCurrentItem(0, true)
                viewModel.addToTiles(pos)
            }
            R.id.uninstall ->
            {
                //launcher uninstall intent
            }
            R.id.info ->
            {
                //launcher info intent
            }
            R.id.removeFromStart ->
            {
                viewModel.removeFromTiles(pos)
            }
        }

        return super.onContextItemSelected(item)
    }
    class SectionsPagerAdapter : PagerAdapter()
    {
        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            var resId = 0
            when (position) {
                0 -> resId = R.id.livetilehost
                1 -> resId = R.id.appdrawer
            }
            return container.findViewById(resId)
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return if (position == 0)
                "Home"
            else
                "Apps"
        }
        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view == `object`
        }
        override fun getCount(): Int {
            return 2
        }
    }

}
enum class SwipeDirection
{
    All, Left, Right, None
}
class HomePager(context: Context?, attrs: AttributeSet?) : ViewPager(context!!, attrs)
{
    private var initialXValue = 0f
    private var direction: SwipeDirection

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (isSwipeAllowed(event)) {
            super.onTouchEvent(event)
        } else false
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return if (isSwipeAllowed(event)) {
            super.onInterceptTouchEvent(event)
        } else false
    }

    private fun isSwipeAllowed(event: MotionEvent): Boolean {
        if (direction == SwipeDirection.All) return true
        if (direction == SwipeDirection.None) //disable any swipe
            return false
        if (event.action == MotionEvent.ACTION_DOWN) {
            initialXValue = event.x
            return true
        }
        if (event.action == MotionEvent.ACTION_MOVE) {
            try {
                val diffX = event.x - initialXValue
                if (diffX > 0 && direction == SwipeDirection.Right) { // swipe from left to right detected
                    return false
                } else if (diffX < 0 && direction == SwipeDirection.Left) { // swipe from right to left detected
                    return false
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        }
        return true
    }

    fun setAllowedSwipeDirection(direction: SwipeDirection) {
        this.direction = direction
    }

    init {
        direction = SwipeDirection.All
    }
}


















