package com.valkriaine.glasslauncher

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur



enum class TileType{
    NORMAL, WIDGET, GALLERY
}

class ViewModel (context: Context, pm : PackageManager) {


    private val appKey = "APPLICATIONLIST"
    private val tileKey = "LIVETILELIST"
    var allApps = ArrayList<AppInfo>()
    private var apps = ArrayList<AppInfo>()
    var tiles = ArrayList<LiveTile>()
    val appsAdapter = AppsAdapter(context, R.layout.item)
    val recyclerViewAdapter = RecyclerViewAdapter(context)
    val packageManager = pm
    private val tilesLoader = LoadTiles()
    private val appsLoader = LoadApps()

    init {
        appsLoader.execute()
        tilesLoader.execute()
        apps = allApps
    }






    fun ArrayList<AppInfo>.sort() {
    forEach { app ->
        if (app.label?.get(0)?.isUpperCase()!!)
            app.label = app.label!![0].toUpperCase() + app.label!!.substring(1)
    }
    sortWith(Comparator { app1, app2 ->
        app2.label?.let { app1.label?.compareTo(it) }!!
    })
}


    fun notifyAppListDataChange()
    {
        appsAdapter.notifyDataSetChanged()
        //add animation notifyChange
    }

    fun moveTile(from : Int, to: Int)
    {
        recyclerViewAdapter.moveItem(from, to)
        recyclerViewAdapter.notifyItemMoved(from, to)
    }

    fun launchApp(position: Int) : Intent? = packageManager.getLaunchIntentForPackage(apps[position].name!!)

    fun addToTiles(position: Int)
    {
        recyclerViewAdapter.add(allApps[position].toTile())
    }
    fun checkIfTileExists(appInfo: AppInfo) : Boolean
    {
        for (tile in tiles)
        {
            if (tile.name == appInfo.name)
                return true
        }
        return false
    }

    fun removeFromTiles(position: Int)
    {
        for (tile in tiles)
        {
            if (tile.name == allApps[position].name) {
                recyclerViewAdapter.remove(tile)
                break
            }

        }
    }






    inner class RecyclerViewAdapter (private val context: Context) : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>()
    {
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
        {
            private val icon : ImageView = itemView.findViewById(R.id.tileicon)
            private val label: TextView = itemView.findViewById(R.id.tilelabel)
            private val inner : ConstraintLayout = itemView.findViewById(R.id.inner)
            private val outer : ConstraintLayout = itemView.findViewById(R.id.tilelayout)
            private val blur : BlurView = itemView.findViewById(R.id.trans)

            fun bindItem(liveTile: LiveTile?)
            {
                icon.setImageDrawable(liveTile?.icon)
                label.text = liveTile?.rename

                when (liveTile?.size) {
                    0 -> {
                        outer.minWidth = HomeScreen.tileList.width / 2
                        outer.maxWidth = outer.minWidth
                        outer.maxHeight = outer.minWidth
                        outer.minHeight = outer.maxHeight
                        inner.minHeight = outer.minHeight - 19
                        inner.minWidth = inner.minHeight
                        inner.maxHeight = inner.minHeight
                        inner.maxWidth = inner.minWidth
                    }
                    1 -> {
                        outer.minWidth = HomeScreen.tileList.width
                        outer.maxWidth = outer.minWidth
                        outer.maxHeight = outer.minWidth/2
                        outer.minHeight = outer.maxHeight
                        inner.minHeight = outer.minHeight - 19
                        inner.minWidth = outer.minWidth - 19
                        inner.maxHeight = inner.minHeight
                        inner.maxWidth = inner.minWidth
                    }
                    2 -> {
                        outer.minWidth = HomeScreen.tileList.width
                        outer.maxWidth = outer.minWidth
                        outer.maxHeight = outer.minWidth
                        outer.minHeight = outer.maxHeight
                        inner.minHeight = outer.minHeight - 19
                        inner.minWidth = outer.minWidth - 19
                        inner.maxHeight = inner.minHeight
                        inner.maxWidth = inner.minWidth
                    }
                }
                blur.setupWith(HomeScreen.blur)
                    .setBlurAlgorithm(RenderScriptBlur(context))
                    .setBlurRadius(18F)
                    .setHasFixedTransformationMatrix(false)
            }
        }
        fun moveItem(from: Int, to: Int)
        {
            val fromTile = tiles[from]
            tiles.removeAt(from)
            tiles.add(to, fromTile)
        }
        fun add(tile: LiveTile)
        {
            tiles.add(tile)
            notifyItemInserted(tiles.size)
        }
        fun remove(tile: LiveTile)
        {
            val pos = tiles.indexOf(tile)
            tiles.remove(tile)
            notifyItemRemoved(pos)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
        {
            return when (viewType) {
                0 -> ViewHolder(LayoutInflater.from(context).inflate(R.layout.tile, parent, false))
                1 -> ViewHolder(LayoutInflater.from(context).inflate(R.layout.tile_wide, parent, false))
                else -> ViewHolder(LayoutInflater.from(context).inflate(R.layout.tile_large, parent, false))
            }
        }
        //more on this, add widget type
        override fun getItemViewType(position: Int): Int = tiles[position].size
        override fun getItemCount(): Int = tiles.size
        override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bindItem((tiles[position]))
    }
    inner class AppsAdapter internal constructor(context: Context, private val resource: Int) : ArrayAdapter<AppsAdapter.ItemHolder>(context, resource)
    {


        override fun getCount(): Int {
            return allApps.size
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var cv = convertView

            val holder: ItemHolder
            if (cv == null) {
                cv = LayoutInflater.from(context).inflate(resource, null)
                holder = ItemHolder()
                holder.name = cv!!.findViewById(R.id.label)
                holder.icon = cv.findViewById(R.id.icon)
                cv.tag = holder
            } else {
                holder = cv.tag as ItemHolder
            }

            holder.name!!.text = allApps[position].rename
            holder.icon!!.setImageDrawable(allApps[position].icon)
            holder.icon!!.setBackgroundColor(allApps[position].color)

            return cv
        }


        inner class ItemHolder {
            var name: TextView? = null
            var icon: ImageView? = null
        }

    }



    @SuppressLint("StaticFieldLeak")
    inner class LoadTiles : AsyncTask<String, Int, Int>()
    {

        override fun doInBackground(vararg params: String?): Int
        {
            //load tiles here

            return tiles.size
        }

        override fun onPostExecute(result: Int?) {
            super.onPostExecute(result)
            recyclerViewAdapter.notifyDataSetChanged()
        }

    }
    @SuppressLint("StaticFieldLeak")
    inner class LoadApps : AsyncTask<String, Int, Int>()
    {
        override fun doInBackground(vararg params: String?): Int
        {
            try {
                if (allApps.size == 0) {
                    val i = Intent(Intent.ACTION_MAIN, null)
                    i.addCategory(Intent.CATEGORY_LAUNCHER)
                    val availableApps = packageManager.queryIntentActivities(i, 0)
                    for (ri in availableApps) {
                        if (ri.activityInfo.packageName != BuildConfig.APPLICATION_ID)
                        allApps.add(AppInfo(ri.loadLabel(packageManager).toString(),ri.activityInfo.loadIcon(packageManager),ri.activityInfo.packageName))
                    }

                    allApps.sort()
                }
            } catch (ex: Exception) {
                Log.e("Error loadApps", ex.message.toString() + " loadApps")
            }
            return allApps.size
        }

        override fun onPostExecute(result: Int?) {
            super.onPostExecute(result)
            notifyAppListDataChange()
            allApps.sort()
        }

    }
}





open class AppInfo (label : String, icon : Drawable, packageName : String) : Comparable<AppInfo>
{
    var label: String? = label
    var rename: String? = label
    val name: String? = packageName
    var icon: Drawable? = icon
    var color : Int = 0
    var isHidden = false

    private fun createPaletteSync(bitmap: Bitmap): Palette = Palette.from(bitmap).generate()

    fun toTile() : LiveTile
    {
        return LiveTile(label!!,icon!!,name!!)
    }

    init {
        color = createPaletteSync(icon.toBitmap(100,100, Bitmap.Config.ARGB_8888)).getVibrantColor(Color.WHITE)
    }

    override fun compareTo(other: AppInfo): Int = this.rename.toString().compareTo(other.rename.toString())
}
class LiveTile(label : String, icon : Drawable, packageName : String) : AppInfo(label, icon, packageName)
{
    var size = 0
    var type = TileType.NORMAL
}






