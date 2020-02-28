package com.valkriaine.glasslauncher

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.AdapterView.AdapterContextMenuInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.databinding.DataBindingUtil
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager
import com.valkriaine.glasslauncher.databinding.ActivityHomeScreenBinding
import jp.wasabeef.recyclerview.animators.ScaleInBottomAnimator
import no.danielzeller.blurbehindlib.UpdateMode


class HomeScreen : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {


    private val key = "APPTILELISTPREFERENCE"
    private val firstLaunch = "isFirstLaunch"
    private lateinit var defaultSharedPreferences: SharedPreferences
    private var isFirstLaunchBlur = true
    private val timeUnit : Long = 0.000001.toLong()
    private var xOffset : Float = 0f
    private lateinit var wm : WallpaperManager
    lateinit var pagerAdapter: SectionsPagerAdapter
    private lateinit var widgetManager: AppWidgetManager
    private lateinit var broadcastReceiver: BroadcastReceiver
    private lateinit var sharedPreferences: SharedPreferences
    private val itemTouchHelper by lazy{
        val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(UP or DOWN or START or END, 2) {

            override fun onMove(recyclerView: RecyclerView,
                                viewHolder: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                viewModel.moveTile(from, to)
                return true
            }

            override fun isItemViewSwipeEnabled(): Boolean {
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
        lateinit var viewModel : ViewModel
        lateinit var binding : ActivityHomeScreenBinding
        var isFirstLaunch : Boolean = true
    }
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this)

        sharedPreferences = getSharedPreferences(key, Context.MODE_PRIVATE)
        isFirstLaunch = sharedPreferences.getBoolean(firstLaunch, true)


        if (isFirstLaunch)
        {
            startActivity(Intent(this, SettingsActivity :: class.java))
            finish()
        }
        else
        {
            binding = DataBindingUtil.setContentView(this, R.layout.activity_home_screen)
            widgetManager = AppWidgetManager.getInstance(this)
            viewModel = ViewModel(this, packageManager, sharedPreferences)

            linkComponents()
            setUpPager()
            setUpAppList()
            setUpTileList()
            hideNavigationBar()
            registerBroadcast()
        }
    }

    private fun registerBroadcast ()
    {
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when(intent.action) {
                    Intent.ACTION_PACKAGE_ADDED -> {
                        val appAdded = context.packageManager.getApplicationInfo(intent.dataString!!.substring(8), 0)
                        val appIntentAdded = context.packageManager.getLaunchIntentForPackage(appAdded.packageName)
                        if(appIntentAdded != null && appAdded.packageName != BuildConfig.APPLICATION_ID) {
                            viewModel.apps.apply {
                                add(AppInfo(appAdded.loadLabel(context.packageManager).toString(), appAdded.loadIcon(context.packageManager).toBitmap(), appAdded.packageName))
                                sort()
                            }
                        }
                        viewModel.notifyAppListDataChange()
                    }
                    Intent.ACTION_PACKAGE_REMOVED -> {
                        if (!intent.extras?.getBoolean(Intent.EXTRA_REPLACING)!!) {
                            viewModel.apps.removeByPackage(intent.dataString!!.substring(8))
                            viewModel.tiles.removeByPackageName(intent.dataString!!.substring(8))
                        }
                        else
                            viewModel.apps.removeByPackage(intent.dataString!!.substring(8))
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
        if (!isFirstLaunch) {
            binding.backgroundBlur.disable()
            unregisterReceiver(broadcastReceiver)
        }
    }
    override fun onResume()
    {
        super.onResume()
        if (!isFirstLaunch)
        {
            binding.backgroundBlur.enable()
            binding.backgroundBlur.updateForMilliSeconds(timeUnit)
            registerBroadcast()
        }
        else
            startActivity(Intent(this, SettingsActivity :: class.java))

    }
    override fun onBackPressed()
    {
        if (binding.viewPager.currentItem == 1)
            binding.viewPager.setCurrentItem(0, true)
    }
    private fun hideNavigationBar()
    {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
    }
    fun switchpage(view: View)
    {
        binding.viewPager.setCurrentItem(1, true)
    }
    private fun linkComponents()
    {
            this.wm = WallpaperManager.getInstance(applicationContext)
            binding.apply {
                image.setImageDrawable(wm.drawable)
                dim.alpha = 0f
                backgroundBlur.viewBehind = binding.image
            }
    }
    private fun setUpPager()
    {
        pagerAdapter = SectionsPagerAdapter()
        binding.apply {
            viewPager.adapter = pagerAdapter
            viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

                override fun onPageScrollStateChanged(state: Int)
                {
                        if (viewPager.currentItem == 0) {
                            backgroundBlur.disable()
                        }
                        if (viewPager.currentItem == 1) {
                            backgroundBlur.updateMode = UpdateMode.MANUALLY
                        }
                }

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int)
                {
                    if (isFirstLaunchBlur) {
                        backgroundBlur.disable()
                        isFirstLaunchBlur = false
                    }
                    else
                    {
                        xOffset = (position + positionOffset) / (pagerAdapter.count - 1)

                            backgroundBlur.enable()
                            backgroundBlur.updateMode = UpdateMode.ON_SCROLL
                            backgroundBlur.blurRadius = xOffset * 40

                        dim.alpha = xOffset / 1.5f
                        arrowButton.rotation = +180 * xOffset - 180
                    }

                }

                override fun onPageSelected(position: Int)
                {
                    if (position == 0)
                    {
                        arrowButton.rotation = 180f
                    }

                }
            })
        }

    }
    private fun setUpAppList()
    {
        binding.apply {
            list.adapter = viewModel.appsAdapter
            list.setOnItemClickListener{ _, _, position, _ ->
                try {
                    startActivity(viewModel.launchApp(position))
                }
                catch (e : java.lang.Exception)
                {

                }

            }
            registerForContextMenu(list)
            list.setOnCreateContextMenuListener { menu, v, menuInfo ->
                super.onCreateContextMenu(menu, v, menuInfo)
                val info = menuInfo as AdapterContextMenuInfo
                val pos = info.position

                if (!viewModel.checkIfTileExists(viewModel.apps[pos]))
                    menuInflater.inflate(R.menu.applist_menu_add, menu)
                else
                    menuInflater.inflate(R.menu.applist_menu_remove, menu)

            }
        }
    }
    private fun setUpTileList()
    {
        binding.apply {
            tileList.adapter = viewModel.recyclerViewAdapter
            registerForContextMenu(tileList)
            itemTouchHelper.attachToRecyclerView(tileList)
            tileList.itemAnimator = ScaleInBottomAnimator()
        }

        binding.tileList.layoutManager = ChipsLayoutManager.newBuilder(this)
            .setOrientation(ChipsLayoutManager.HORIZONTAL)
            .setChildGravity(Gravity.CENTER)
            .setRowStrategy(ChipsLayoutManager.STRATEGY_DEFAULT)
            .setMaxViewsInRow(2)
            .setScrollingEnabled(true)
            .build()

        viewModel.recyclerViewAdapter.onItemClick = { liveTile -> startActivity(packageManager.getLaunchIntentForPackage(liveTile.name!!)) }
        viewModel.recyclerViewAdapter.onItemLongClick = {
        }


    }
    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as AdapterContextMenuInfo
        val pos = info.position
        when (item.itemId)
        {
            R.id.addToStart ->
            {
                binding.viewPager.setCurrentItem(0, true)
                binding.tileList.scrollToPosition(viewModel.tiles.size - 1)
                viewModel.addToTiles(pos)
            }
            R.id.uninstall ->
            {
               startActivity(viewModel.uninstallApp(pos))
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
                0 -> resId = R.id.liveTileHost
                1 -> resId = R.id.appdrawer
            }
            return container.findViewById(resId)
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view == `object`
        }
        override fun getCount(): Int {
            return 2
        }
    }

    private fun setBackgroundVisibility(visibility : Boolean)
    {
        if (!visibility)
        {
            binding.screen.setBackgroundColor(Color.BLACK)
        }
    }

//todo: more on this. need two separate setting menus
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key.equals("background"))
            setBackgroundVisibility(sharedPreferences!!.getBoolean("background", false))
    }

    override fun onDestroy() {
        super.onDestroy()
        defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

}
class HomePager(context: Context?, attrs: AttributeSet?) : ViewPager(context!!, attrs)
{
    private var initialXValue = 0f
    private var diff = 0f

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

        if (event.action == MotionEvent.ACTION_DOWN)
        {
            initialXValue = event.x
            return true
        }
        if (event.action == MotionEvent.ACTION_MOVE)
        {
            diff = event.x - initialXValue
                if (diff > 0 && HomeScreen.binding.viewPager.currentItem == 0)
                 // swipe from left to right detected
                    return false
                else if (diff < 0 && HomeScreen.binding.viewPager.currentItem == 1)
                // swipe from right to left detected
                    return false
        }
        return true
    }
}





















