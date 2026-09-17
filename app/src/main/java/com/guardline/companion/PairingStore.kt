package com.guardline.companion

import android.content.Context

object PairingStore {
    private const val PREFS = "guardline_prefs"
    private const val KEY_PAIR_CODE = "pair_code"

    fun savePairCode(context: Context, code: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_PAIR_CODE, code).apply()
    }

    fun getPairCode(context: Context): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PAIR_CODE, null)
    }
}
