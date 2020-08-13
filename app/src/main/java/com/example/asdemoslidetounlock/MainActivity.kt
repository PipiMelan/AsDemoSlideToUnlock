package com.example.asdemoslidetounlock

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import androidx.core.graphics.contains
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.StringBuilder
import kotlin.collections.contains as contains

class MainActivity : AppCompatActivity() {
    //Use array to save those objects of nine 'dot' to traverse in sliding
    /**
     * private val dots = arrayOf(dot1,dot2,dot3,dot4,dot5,dot6,dot7,dot8,dot9)
     *
     * The order of object building :
     *     constructor
     * -> 'init' code block,the building of attributes
     * -> onCreate(setContentView(R.layout.activity_main))
     * ->
     */


    //1.
//    private var dots :Array<ImageView>? = null

    //2.懒加载
    private val dots : Array<ImageView> by lazy {
        arrayOf(sDot1,sDot2,sDot3,sDot4,sDot5,sDot6,sDot7,sDot8,sDot9)
    }

    // Save the lighted dots
    private val allSelectedView = mutableListOf<ImageView>()

    //Save all the tag values of lines
    private val allLineTags = arrayOf(
        12,23,45,56,67,78,89,    /** All the horizontal lines*/
        14,25,36,47,58,69,       /** All the vertical lines*/
        24,35,57,68,15,26,48,59  /** All the diagonal lines*/
    )

    // Record the last lighted dot object
    private var lastSelectedView: ImageView? = null

    //图片或者视频的请求码
    private val REQUEST_IMAGE_CODE = 123
    private val REQUEST_VIDEO_CODE = 456


    // Record the track when sliding
    private val password = StringBuilder()
    //Record the original password
    private var orgPassword :String? = null
    //Record the password set first time
    private var firstPassword :String? = null

    /********************************************/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        SharedPreferenceUtil.getInstance(this).getPassword().also {
            if(it == null){
                mALert.text = "请设置密码图案"
            }else{
                mALert.text = "请解锁"
                orgPassword = it
            }
        }

        //Add click event to mHeader
        mHeader.setOnClickListener{
            //Get images from camera
            Intent().apply{
                action = Intent.ACTION_PICK
                type = "image/*"

                startActivityForResult(this,REQUEST_IMAGE_CODE)
            }
        }
        //获取头像
        File(filesDir,"header.jpg").also {
            if(it.exists()){
                BitmapFactory.decodeFile(it.path).also {image ->
                    mHeader.setImageBitmap(image)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            REQUEST_IMAGE_CODE -> {
                //Image InputStream OutputStream Writer Reader
                //Judging whether user cancel this operation
                if(resultCode == Activity.RESULT_CANCELED){
                    // Get the Image
                    data?.data.let{it ->
                        if (it != null) {
                            contentResolver.openInputStream(it).use{
                                // Bitmap
                                BitmapFactory.decodeStream(it).also {image ->
                                    //
                                    mHeader.setImageBitmap(image)
                                    //缓存图片
                                    val file = File(filesDir,"header.jpg")
                                    FileOutputStream(file).also {fos->
                                        image.compress(Bitmap.CompressFormat.JPEG,50,fos)
                                    }
//                                    FileOutputStream(filesDir)
//                                    image.compress(Bitmap.CompressFormat.JPEG,50,)
                                    Log.v("pxd",filesDir.absolutePath)
                                }
                            }
                        }
                    }
                }
            }
            REQUEST_VIDEO_CODE -> {

            }
        }

    }
    /*******************************************/





    // 监听触摸事件
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        //将触摸点的坐标转换到mContainer上
        val location = convertTouchLocationToContainerLocation(event!!)

        // 判断是否在操作区域内
        if (!(location.x >= 0 && location.x <= mContainer.width) || !(location.y >= 0 && location.y <= mContainer.height)){
            // 如果不在操作区域内就直接返回不执行下面的操作
            return true
        }
        when(event?.action){
            MotionEvent.ACTION_DOWN ->{
                findViewContainsPoint(location).also {
                   highlightView(it)
                }
            }
            MotionEvent.ACTION_MOVE->{
                findViewContainsPoint(location).also {
                    highlightView(it)
                }

            }
            MotionEvent.ACTION_UP->{
                //Judge whether this is the first time to set password
                if(orgPassword == null){
                    //Whether this is the first time to set password
                    if(firstPassword == null){
                        //记录第一次密码
                        firstPassword = password.toString()
                        //提示输入第二次密码
                        mALert.text = "请确认密码图案"
                    }else{
                       passwordCompare(firstPassword!!,password.toString())
                    }
                }else{
                    passwordCompare(firstPassword!!,password.toString())
                }
                restoreOperation()
            }
        }
        return true
    }

    private fun passwordCompare(first:String,second:String){
        //确认密码
        if (first == second){
            // 两次密码一致
            mALert.text = "密码设置成功"
            // 保存密码
            SharedPreferenceUtil.getInstance(this).savePassword(first)
        }else{
            mALert.text = "请重新设置"
            firstPassword = null
        }
    }

    // Lightening Operation
    private fun highlightView(v:ImageView?){
        if (v != null && v.visibility == View.INVISIBLE) {
            //Judge whether dot is the first dot
            if(lastSelectedView == null) {
                // Just lighten this dot and save it
                highlightDot(v)
            }else{
                //Certain dot has been lightened
                //Get the tag value between last dot and current dot
                val previous = (lastSelectedView?.tag as String ).toInt()
                val current = (v.tag as String).toInt()
                val lineTag = if (previous > current) current*10+previous else previous*10+current
                
                //Judge whether this line exists
                if (allLineTags.contains(lineTag)){
                    //Lighten this dot
                   highlightDot(v)
                    //Lighten this line
                    mContainer.findViewWithTag<ImageView>(lineTag.toString()).apply {
                        visibility = View.VISIBLE
                        allSelectedView.add(this)
                    }
                }
            }
        }
    }

    private fun highlightDot(v:ImageView){
        // Lighten  the dots
        v.visibility = View.VISIBLE
        allSelectedView.add(v)
        password.append(v.tag)

        //
        lastSelectedView = v;
    }

    //restore operation
    private fun restoreOperation(){
        // Traverse the array of lighted dots
        for (item in allSelectedView){
            item.visibility = View.INVISIBLE
        }

        // Clear them
        allSelectedView.clear()
        lastSelectedView = null

        Log.v("dxj",password.toString())
        password.clear()
    }


    //2.Use lazy loading to get the height of title bar and status bar
    private val barHeight:Int by lazy{
        // Get the screen size
        val display = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(display)

        //get the operation size
        val drawingRect = Rect()
        window.findViewById<ViewGroup>(Window.ID_ANDROID_CONTENT).getDrawingRect(drawingRect)

        display.heightPixels - drawingRect.height()
    }

    //1.Get the height of the title bar and status bar(标题栏和状态栏)
   /** private fun barHeight1(): Int{
        // Get the screen size
        val display = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(display)

        //get the operation size
        val drawingRect = Rect()
        window.findViewById<ViewGroup>(Window.ID_ANDROID_CONTENT).getDrawingRect(drawingRect)

        return display.heightPixels - drawingRect.height()
    }*/

    // 将触摸点的坐标转换为容器的坐标
    // Converts the coordinates of the touch point to the coordinates of the container
    private fun convertTouchLocationToContainerLocation(event: MotionEvent):Point{
        return Point().apply{
        //x = 触摸点x - 容器的x
            x = (event.x - mContainer.x).toInt()
        // y = 触摸点y - 容器的y - 状态栏高度
            y = (event.y - mContainer.y - barHeight).toInt()
        }
    }

    // Get the dot view of current touch point
    private fun findViewContainsPoint(point:Point):ImageView?{
        // Traverse all dots judging whether contains certain point
        for(dotView in dots){
            getRectForView(dotView).also {
                if(it.contains(point.x,point.y)) {
                    return dotView
                }
            }
        }
        return null
     }

    //Get the corresponding rect in view
    private fun getRectForView(v:View) = Rect(v.left,v.top,v.right,v.bottom)
}