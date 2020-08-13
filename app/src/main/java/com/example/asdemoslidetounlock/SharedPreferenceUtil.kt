package com.example.asdemoslidetounlock

import android.content.Context
import java.util.prefs.Preferences

/**
 *@Description
 *@邓兴杰
 *@QQ1793126995
 */
class SharedPreferenceUtil  private constructor() {
    private val FILE_NAME = "password"
    private val KEY = "passwordKey"

    companion object {
        private var mContext: Context? = null
        private var instance: SharedPreferenceUtil? = null

        fun getInstance(context: Context): SharedPreferenceUtil {
            mContext = context
            if (instance == null) {
                synchronized(this) {
                    instance = SharedPreferenceUtil()
                }
            }
            return instance!!
        }
    }

    fun savePassword(pwd: String) {
        val sharedPreferences = mContext?.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

        val edit = sharedPreferences?.edit()

        edit?.putString(KEY, pwd)

//        edit?.commit()
        edit?.apply()
        /*mContext?.getSharedPreferences(FILE_NAME,Context.MODE_PRIVATE).also {
           it?.edit()
       }*/
    }

    fun getPassword(): String? {
        val sharedPreferences = mContext?.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        return sharedPreferences?.getString(KEY,null)
    }
}