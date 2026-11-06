package com.example.util

import java.util.Calendar

data class CalendarWeek(
    val mondayTime: Long,
    val sundayTime: Long,
    val assignedMonth: Int // 0..11
)

object DateUtils {
    fun getCalendarWeeksForYear(year: Int): List<CalendarWeek> {
        val weeks = mutableListOf<CalendarWeek>()
        
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        
        val nextYear = year + 1
        
        while (true) {
            val mondayTime = cal.timeInMillis
            val monthCounts = IntArray(12) { 0 }
            
            val tempCal = Calendar.getInstance().apply { timeInMillis = mondayTime }
            for (i in 0 until 7) {
                val y = tempCal.get(Calendar.YEAR)
                val m = tempCal.get(Calendar.MONTH)
                if (y == year && m in 0..11) {
                    monthCounts[m]++
                }
                tempCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            tempCal.add(Calendar.DAY_OF_YEAR, -1)
            val sundayTime = tempCal.timeInMillis
            
            var assignedMonth = -1
            var maxCount = 0
            for (m in 0..11) {
                if (monthCounts[m] > maxCount) {
                    maxCount = monthCounts[m]
                    assignedMonth = m
                }
            }
            
            if (assignedMonth != -1 && maxCount >= 4) {
                weeks.add(CalendarWeek(mondayTime, sundayTime, assignedMonth))
            }
            
            cal.add(Calendar.DAY_OF_YEAR, 7)
            
            if (cal.get(Calendar.YEAR) >= nextYear) {
                val checkCal = Calendar.getInstance().apply { timeInMillis = cal.timeInMillis }
                var daysInNextYear = 0
                for (i in 0 until 7) {
                    if (checkCal.get(Calendar.YEAR) >= nextYear) {
                        daysInNextYear++
                    }
                    checkCal.add(Calendar.DAY_OF_YEAR, 1)
                }
                if (daysInNextYear >= 4) {
                    break
                }
            }
        }
        
        return weeks
    }
}
