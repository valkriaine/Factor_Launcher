package com.valkriaine.glasslauncher

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.AsyncTask
import android.util.Base64
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.qhutch.elevationimageview.ElevationImageView
import com.valkriaine.glasslauncher.animation.RecyclerViewAnimator
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur
import java.io.ByteArrayOutputStream


enum class TileType{
    NORMAL, WIDGET, GALLERY, FOLDER
}

class ViewModel (context: Context, pm : PackageManager, s: SharedPreferences) {


    val tileType = object : TypeToken<ArrayList<LiveTile>>() {}.type!!
    val appType = object : TypeToken<ArrayList<AppInfo>>() {}.type!!
    private val appKey = "APPLICATIONLIST"
    private val tileKey = "LIVETILELIST"
    var allApps = ArrayList<AppInfo>()
    private val sharedPreferences = s
    private val editor = sharedPreferences.edit()
    private val gson = Gson()
    var apps = ArrayList<AppInfo>()
    var tiles = ArrayList<LiveTile>()
    val appsAdapter = AppsAdapter(context, R.layout.item)
    val recyclerViewAdapter = RecyclerViewAdapter(context)
    val packageManager = pm
    private val tilesLoader = LoadTiles()
    private val appsLoader = LoadApps()
    private lateinit var tilesSaver : SaveTiles
    //todo: save changes to appList in the background
    private lateinit var appsSaver : SaveApps

    init {
        appsLoader.execute()
        tilesLoader.execute()
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
    }
    fun getBitmapFromString(stringPicture: String): Bitmap? {
        val decodedString = Base64.decode(stringPicture, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
    }

    fun moveTile(from : Int, to: Int)
    {
        recyclerViewAdapter.moveItem(from, to)
        recyclerViewAdapter.notifyItemMoved(from, to)
    }
    fun launchApp(position: Int) : Intent? = packageManager.getLaunchIntentForPackage(apps[position].name!!)

    fun addToTiles(position: Int)
    {
        recyclerViewAdapter.add(apps[position].toTile())
        tilesSaver = SaveTiles()
        tilesSaver.execute()
    }
    fun removeFromTiles(position: Int)
    {
        for (tile in tiles)
        {
            if (tile.name == apps[position].name) {
                recyclerViewAdapter.remove(tile)
                break
            }
        }
        tilesSaver = SaveTiles()
        tilesSaver.execute()
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
    fun uninstallApp(position: Int) : Intent?
    {
        return Intent(Intent.ACTION_DELETE, Uri.parse("package:" + apps[position].name))
    }



    inner class RecyclerViewAdapter (private val context: Context) : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>()
    {
        var onItemClick: ((LiveTile) -> Unit)? = null
        var onItemLongClick : ((LiveTile) -> Unit)? = null
        private val animator =
            RecyclerViewAnimator(HomeScreen.binding.tileList)

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
        {
            private var size = 1
            private val icon : ElevationImageView = itemView.findViewById(R.id.tileIcon) as ElevationImageView
            private val label: TextView = itemView.findViewById(R.id.tileLabel) as TextView
            private val inner : ConstraintLayout = itemView.findViewById(R.id.inner) as ConstraintLayout
            private val outer : ConstraintLayout = itemView.findViewById(R.id.tileLayout) as ConstraintLayout
            private val blur : BlurView = itemView.findViewById(R.id.trans) as BlurView
            private val color : FrameLayout = itemView.findViewById(R.id.color) as FrameLayout

            init {
                itemView.setOnClickListener {
                    onItemClick?.invoke(tiles[adapterPosition])
                }
                itemView.setOnLongClickListener {
                    onItemLongClick?.invoke(tiles[adapterPosition])
                    itemView.isLongClickable
                }

                itemView.setOnLongClickListener{
                    val popup = PopupMenu(context, itemView)
                    popup.inflate(R.menu.tilelist_menu)
                    popup.menu.add("Remove from Start")
                    popup.menu.add("Edit")
                    when (size) {
                        0 -> {
                            popup.menu.add("Make wide")
                            popup.menu.add("Make large")
                        }
                        1 -> {
                            popup.menu.add("Make small")
                            popup.menu.add("Make large")
                        }
                        else -> {
                            popup.menu.add("Make small")
                            popup.menu.add("Make wide")
                        }
                    }
                    popup.setOnMenuItemClickListener { item ->
                        if (item.title == "Remove from Start") {
                            recyclerViewAdapter.remove(tiles[adapterPosition])
                        }
                        if (item.title == "Edit") {
                            //todo: edit mode
                        }
                        if (item.title == "Make wide") {
                            tiles[adapterPosition].size = 1
                            recyclerViewAdapter.notifyItemChanged(adapterPosition)
                        }
                        if (item.title == "Make large") {
                            tiles[adapterPosition].size = 2
                            recyclerViewAdapter.notifyItemChanged(adapterPosition)
                        }
                        if (item.title == "Make small") {
                            tiles[adapterPosition].size = 0
                            recyclerViewAdapter.notifyItemChanged(adapterPosition)
                        }

                        tilesSaver = SaveTiles()
                        tilesSaver.execute()
                        true
                    }
                    popup.show()
                    true
                }

            }

            fun bindItem(liveTile: LiveTile?)
            {
                size = liveTile!!.size
                icon.setImageBitmap(getBitmapFromString(liveTile.icon!!))
                label.text = liveTile.rename

                when (liveTile.size) {
                    0 -> {
                        outer.minWidth = HomeScreen.binding.tileList.width / 2
                        outer.maxWidth = outer.minWidth
                        outer.maxHeight = outer.minWidth
                        outer.minHeight = outer.maxHeight
                        inner.minHeight = outer.minHeight - 19
                        inner.minWidth = inner.minHeight
                        inner.maxHeight = inner.minHeight
                        inner.maxWidth = inner.minWidth
                    }
                    1 -> {
                        outer.minWidth = HomeScreen.binding.tileList.width
                        outer.maxWidth = outer.minWidth
                        outer.maxHeight = outer.minWidth/2
                        outer.minHeight = outer.maxHeight
                        inner.minHeight = outer.minHeight - 19
                        inner.minWidth = outer.minWidth - 19
                        inner.maxHeight = inner.minHeight
                        inner.maxWidth = inner.minWidth
                    }
                    2 -> {
                        outer.minWidth = HomeScreen.binding.tileList.width
                        outer.maxWidth = outer.minWidth
                        outer.maxHeight = outer.minWidth
                        outer.minHeight = outer.maxHeight
                        inner.minHeight = outer.minHeight - 19
                        inner.minWidth = outer.minWidth - 19
                        inner.maxHeight = inner.minHeight
                        inner.maxWidth = inner.minWidth
                    }
                }
                blur.setupWith(HomeScreen.binding.blurBackground)
                    .setBlurAlgorithm(RenderScriptBlur(context))
                    .setBlurRadius(18F)
                    .setHasFixedTransformationMatrix(false)

                color.setBackgroundColor(liveTile.color)
                color.alpha = 0.15F

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
            val layout = LayoutInflater.from(context).inflate(R.layout.tile, parent, false)

            animator.onCreateViewHolder(layout)
            return ViewHolder(layout)
        }
        override fun getItemViewType(position: Int): Int = tiles[position].size
        override fun getItemCount(): Int = tiles.size
        override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads : MutableList<Any>){
            animator.onBindViewHolder(holder.itemView, position)
            if (payloads.isEmpty()) {
                holder.bindItem((tiles[position]))
            }
            else
            {
                if (payloads.any{it is NotificationChanged})
                {
                    //todo: handle notification change here
                }
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bindItem((tiles[position]))
        }
    }
    inner class AppsAdapter internal constructor(context: Context, private val resource: Int) : ArrayAdapter<AppsAdapter.ItemHolder>(context, resource)
    {
        override fun getCount(): Int {
            return apps.size
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var cv = convertView

            val holder: ItemHolder
            if (cv == null) {
                cv = LayoutInflater.from(context).inflate(resource, null)
                holder = ItemHolder()
                holder.name = cv!!.findViewById(R.id.label) as TextView
                holder.icon = cv.findViewById(R.id.icon) as ImageView
                cv.tag = holder
            } else {
                holder = cv.tag as ItemHolder
            }

            holder.name!!.text = apps[position].rename
            holder.icon!!.setImageBitmap(getBitmapFromString(apps[position].reIcon!!))
            holder.icon!!.setBackgroundColor(apps[position].color)

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
            val json : ArrayList<LiveTile>? = gson.fromJson(sharedPreferences.getString(tileKey, ""), tileType)
            if (json != null)
                tiles = json
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
            val json : ArrayList<AppInfo>? = gson.fromJson(sharedPreferences.getString(appKey, ""), appType)
            if (json != null)
                apps = json
            try {
                if (apps.size == 0) {
                    val i = Intent(Intent.ACTION_MAIN, null)
                    i.addCategory(Intent.CATEGORY_LAUNCHER)
                    val availableApps = packageManager.queryIntentActivities(i, 0)
                    for (ri in availableApps) {
                        if (ri.activityInfo.packageName != BuildConfig.APPLICATION_ID)
                        allApps.add(AppInfo(ri.loadLabel(packageManager).toString(),ri.activityInfo.loadIcon(packageManager).toBitmap(),ri.activityInfo.packageName))
                    }
                    allApps.sort()
                    apps = allApps
                }
            } catch (ex: Exception) {
                Log.e("Error loadApps", ex.message.toString() + " loadApps")
            }
            return allApps.size
        }

        override fun onPostExecute(result: Int?) {
            super.onPostExecute(result)
            notifyAppListDataChange()
            apps.sort()
        }

    }
    @SuppressLint("StaticFieldLeak")
    inner class SaveTiles : AsyncTask<String, Int, Int>()
    {

        override fun doInBackground(vararg params: String?): Int
        {
            editor.putString(tileKey,gson.toJson(tiles))
            editor.apply()
            return tiles.size
        }
    }
    @SuppressLint("StaticFieldLeak")
    inner class SaveApps : AsyncTask<String, Int, Int>()
    {
        override fun doInBackground(vararg params: String?): Int
        {
            editor.putString(appKey,gson.toJson(apps))
            editor.apply()
            return apps.size
        }
    }
}

class NotificationChanged

open class AppInfo (label : String, icon : Bitmap, packageName : String) : Comparable<AppInfo>
{
    var label: String? = label
    var rename: String? = label
    val name: String? = packageName
    val icon: String? = getStringFromBitmap(icon)
    var reIcon = this.icon
    var color : Int = 0
    var isHidden = false

    private fun createPaletteSync(bitmap: Bitmap): Palette = Palette.from(bitmap).generate()

    private fun getStringFromBitmap(bitmapPicture: Bitmap): String? {
        val quality = 100
        val encodedImage: String
        val byteArrayBitmapStream = ByteArrayOutputStream()
        bitmapPicture.compress(Bitmap.CompressFormat.PNG, quality, byteArrayBitmapStream)
        val b: ByteArray = byteArrayBitmapStream.toByteArray()
        encodedImage = Base64.encodeToString(b, Base64.DEFAULT)
        return encodedImage
    }

    private fun getBitmapFromString(stringPicture: String): Bitmap? {
        val decodedString = Base64.decode(stringPicture, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
    }

    fun toTile() : LiveTile
    {
        return LiveTile(label!!,getBitmapFromString(this.icon!!)!!,name!!)
    }

    init {
        color = createPaletteSync(icon).getVibrantColor(Color.WHITE)
    }
    override fun compareTo(other: AppInfo): Int = this.rename.toString().compareTo(other.rename.toString())
}
class LiveTile(label : String, icon : Bitmap, packageName : String) : AppInfo(label, icon, packageName)
{
    var size = 0
    var type = TileType.NORMAL
}







