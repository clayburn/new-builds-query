package me.clayjohnson.newbuildsquery

import java.time.Instant
import java.time.temporal.ChronoUnit

fun Instant.daysAgo(days: Int): Instant = this.minus(days.toLong(), ChronoUnit.DAYS)