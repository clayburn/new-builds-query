package me.clayjohnson.newbuildsquery

import java.net.URLEncoder

fun String.urlEncode(): String = URLEncoder.encode(this, "utf-8")