package dev.eastar.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import dev.eastar.calendar.CalendarConst.Companion.WEEK_COUNT
import dev.eastar.calendar.tools.DayDrawerImpl
import dev.eastar.calendar.tools.MonthDrawerImpl
import dev.eastar.calendar.tools.WeekDrawerImpl
import java.util.*

//달력부분
class CalendarMonthView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {
    companion object {
        private const val ONE_DAY = 86400000L
        private var RECT = Rect()
    }

    private var monthWidth: Int = 0
    private var monthHeight: Int = 0
    private var weekWidth: Int = 0
    private var weekHeight: Int = 0
    private var dayWidth: Int = 0
    private var dayHeight: Int = 0
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        monthWidth = w
        monthHeight = h
        weekWidth = Math.round((w / Calendar.DAY_OF_WEEK).toDouble()).toInt()
        weekHeight = CalendarConst.WEEK_HEIGHT
        dayWidth = Math.round((w / Calendar.DAY_OF_WEEK).toDouble()).toInt()
        dayHeight = Math.round((h / WEEK_COUNT).toDouble()).toInt()
    }

    private var mDaySelected: Long = 0//달력에서 선택한날
    private var dayFirst: Long = 0//보여지는 시작일
    private var displayMonth: Long = 0//보여지고있는월

    private var monthDrawer: MonthDrawer = MonthDrawerImpl()
    public fun setMonthDrawer(monthDrawer: MonthDrawer) {
        this.monthDrawer = monthDrawer
    }

    private var weekDrawer: WeekDrawer? = WeekDrawerImpl()
    public fun setWeekDrawer(weekDrawer: WeekDrawer?) {
        this.weekDrawer = weekDrawer
    }

    private var dayDrawer: DayDrawer = DayDrawerImpl()
    public fun setDayDrawer(dayDrawer: DayDrawer) {
        this.dayDrawer = dayDrawer
    }

    /**
     * @param displayMonth 선택월
     * @param daySelected 선택일
     */
    fun setDisplayMonth(displayMonth: Long) {
        this.displayMonth = displayMonth
        dayFirst = CalendarUtil.getFirstWeek(displayMonth)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        CalendarObservable.addObserver(observer)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        CalendarObservable.deleteObserver(observer)
    }

    val observer = Observer { _: Observable, o: Any ->
        if (o is Long) {
            mDaySelected = CalendarUtil.getSmartSelectedDay(displayMonth, o)
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val col = Calendar.DAY_OF_WEEK
        val row = WEEK_COUNT
        val dayCount = col * row

        //month
        RECT.set(0, 0, monthWidth, monthHeight)
        monthDrawer.draw(canvas, RECT, dayFirst, row, col)

        RECT.set(0, 0, monthWidth, weekHeight)
        weekDrawer?.draw(canvas, RECT)

        //week
        for (i in 0 until col) {
            val x = weekWidth * i
            canvas.save()
            canvas.translate(x.toFloat(), 0f)

            val dayOfWeek = (CalendarConst.firstDayOfWeek - Calendar.SUNDAY + i) % Calendar.DAY_OF_WEEK + Calendar.SUNDAY
            RECT.set(0, 0, weekWidth, weekHeight)
            weekDrawer?.draw(canvas, RECT, dayOfWeek)
            canvas.restore()
        }

        canvas.translate(0f, weekHeight.toFloat())

        //day
        for (i in 0 until dayCount) {
            val day = dayFirst + i * ONE_DAY
            val x = dayWidth * (i % col)
            val y = Math.round((dayHeight * (i / col)).toDouble()).toInt()
            canvas.save()

            canvas.translate(x.toFloat(), y.toFloat())
            RECT.set(0, 0, dayWidth, dayHeight)
            dayDrawer.draw(canvas, RECT, day, displayMonth, mDaySelected)

            canvas.restore()
        }
    }

    private fun hitTest(e: MotionEvent) {
        val xAxle = (e.x / dayWidth).toInt()
        val yAxle = (e.y / dayHeight).toInt()
        val index = xAxle + yAxle * Calendar.DAY_OF_WEEK
        CalendarObservable.notifySelectedDay(dayFirst + ONE_DAY * index.toLong())
    }

    //------------------------------------------------------------------------------------------
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return detector.onTouchEvent(event)
    }

    private val detector = GestureDetector(getContext(), object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            hitTest(e)
            return false
        }
    })
}
