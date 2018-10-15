package catt.custom.view.day15

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log.e
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.annotation.IntDef
import catt.custom.view.R
import java.lang.ref.WeakReference
import java.util.concurrent.Executors

class Day15GestureLockView
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {
    private val _TAG: String = Day15GestureLockView::class.java.simpleName
    private val _linePaint: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
            isDither = true
            strokeWidth = convertPx(TypedValue.COMPLEX_UNIT_DIP, cattStrokeWidth)
            color = successfulColor
            style = Paint.Style.STROKE
        }
    }

    private val _arrowPaint: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
            isDither = true
            strokeWidth = convertPx(TypedValue.COMPLEX_UNIT_DIP, cattStrokeWidth)
            color = successfulColor
            style = Paint.Style.FILL
        }
    }

    private val _pressedPasswordPaint: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
            isDither = true
            strokeWidth = convertPx(TypedValue.COMPLEX_UNIT_DIP, cattStrokeWidth)
            color = pressedColor
            style = Paint.Style.STROKE
        }
    }

    private val _successfulPasswordPaint: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
            isDither = true
            strokeWidth = convertPx(TypedValue.COMPLEX_UNIT_DIP, cattStrokeWidth)
            color = successfulColor
            style = Paint.Style.STROKE
        }
    }

    private val _errorPasswordPaint: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
            isDither = true
            strokeWidth = convertPx(TypedValue.COMPLEX_UNIT_DIP, cattStrokeWidth)
            color = errorColor
            style = Paint.Style.STROKE
        }
    }

    private val _normalPasswordPaint: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
            isDither = true
            strokeWidth = convertPx(TypedValue.COMPLEX_UNIT_DIP, cattStrokeWidth)
            color = normalColor
            style = Paint.Style.STROKE
        }
    }

    private val _computePointRunnable: WeakReference<ComputePointRunnable> by lazy { WeakReference(ComputePointRunnable(this)) }
    private val points: ArrayList<Point> by lazy { ArrayList<Point>(passwordRow * passwordColumn) }
    private val _originalPasswordList: ArrayList<Int> by lazy { ArrayList<Int>(passwordRow * passwordColumn) }
    private val _checkPasswordList: ArrayList<Int> by lazy { ArrayList<Int>(passwordRow * passwordColumn) }
    private val _selectedPointList: ArrayList<Point> by lazy { ArrayList<Point>(passwordRow * passwordColumn) }

    private var passwordRow: Int = 3
    private var passwordColumn: Int = 3
    private var pressedColor: Int = Color.parseColor("#FF3688CF")
    private var successfulColor: Int = Color.parseColor("#FF4147AF")
    private var normalColor: Int = Color.parseColor("#FFD0D0D0")
    private var errorColor: Int = Color.parseColor("#FFEA4A3E")
    private var cattStrokeWidth: Int = 8
    private var passwordMargin: Int = 16
    private var passwordLength: Int = 3
    private var listener: OnGestureLockEventListener? = null

    init {
        context.obtainStyledAttributes(attrs, R.styleable.Day15GestureLockView).apply {
            passwordLength = getInt(R.styleable.Day15GestureLockView_catt15_PasswordLength, passwordLength)
            if (passwordLength < 3) {
                throw IllegalArgumentException("Minimum password length must be greater than or equal to 3")
            }
            passwordRow = getInt(R.styleable.Day15GestureLockView_catt15_PasswordRow, passwordRow)
            passwordColumn = getInt(R.styleable.Day15GestureLockView_catt15_PasswordColumn, passwordColumn)
            errorColor = getColor(R.styleable.Day15GestureLockView_catt15_ErrorColor, errorColor)
            successfulColor = getColor(R.styleable.Day15GestureLockView_catt15_SuccessfulColor, successfulColor)
            pressedColor = getColor(R.styleable.Day15GestureLockView_catt15_PressedColor, pressedColor)
            normalColor = getColor(R.styleable.Day15GestureLockView_catt15_NormalColor, normalColor)
            cattStrokeWidth = getDimensionPixelSize(R.styleable.Day15GestureLockView_catt15_StrokeWidth, cattStrokeWidth)
            passwordMargin = getDimensionPixelSize(R.styleable.Day15GestureLockView_catt15_PasswordMargin, passwordMargin).run { if (this < passwordMargin) return@run passwordMargin else return@run this }
        }.recycle()
    }

    fun setOnUnlockEventListener(listener: OnGestureLockEventListener?) = listener.apply { this@Day15GestureLockView.listener = this@apply }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val size = when (widthSize > heightSize) {
            true -> heightSize
            else -> widthSize
        }
        setMeasuredDimension(size, size)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        e(_TAG, "@@@ onLayout")
        if (_computePointRunnable.get() != null && _computePointRunnable.get()!!.whetherComputeFinished)
            Executors.newSingleThreadExecutor().execute(_computePointRunnable.get())
    }

    @Synchronized
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.apply {
            drawPoint()
            drawLine()
        }
    }

    private fun Canvas.drawPoint() {
        points.forEach {
            val paint = when (it.state) {
                PointState.NORMAL -> _normalPasswordPaint
                PointState.PRESSED -> _pressedPasswordPaint
                PointState.SUCCESS -> _successfulPasswordPaint
                else -> _errorPasswordPaint
            }
            save()
            drawCircle(it._centerX, it._centerY, it._innerCircleRadius, paint)
            restore()
            save()
            drawCircle(it._centerX, it._centerY, it._outerCircleRadius, paint)
            restore()
        }
    }

    private fun Canvas.drawLine() {
        if (_selectedPointList.isNotEmpty()) {
            lastPoint = _selectedPointList[0]
            _selectedPointList.forEach {
                val stateColor = when (it.state) {
                    PointState.NORMAL -> normalColor
                    PointState.PRESSED -> pressedColor
                    PointState.SUCCESS -> successfulColor
                    else -> errorColor
                }
                _linePaint.color = stateColor
                _arrowPaint.color = stateColor
                drawLine(lastPoint!!, it, _linePaint)
                drawArrow(lastPoint!!, it, it._innerCircleRadius, 38, _arrowPaint)
                lastPoint = it
            }

            lastPoint?.apply lasPoint@{
                val point = Point(
                        _outerCircleRadius,
                        _innerCircleRadius,
                        moveX,
                        moveY,
                        0F,
                        0F, 0F, 0F,
                        state
                )

                if (!whetherTouchUp && !isInsideCircle(this@lasPoint, point)) {
                    drawLine(this@lasPoint, point, _linePaint)
                }
            }
        }
    }


    private fun isInsideCircle(start: Point, end: Point): Boolean {
        val dx = end._centerX - start._centerX
        val dy = end._centerY - start._centerY
        return Math.sqrt(Math.pow(Math.abs(dx).toDouble(), 2.0) + Math.pow(Math.abs(dy).toDouble(), 2.0)) < start._innerCircleRadius
    }


    /**
     * 画箭头
     */
    private fun Canvas.drawArrow(start: Point, end: Point, arrowHeight: Float, angle: Int, paint: Paint) {
        val dx = end._centerX - start._centerX
        val dy = end._centerY - start._centerY
        val pointDistance = Math.sqrt(Math.pow(Math.abs(dx).toDouble(), 2.0) + Math.pow(Math.abs(dy).toDouble(), 2.0))
        val sinB = (dx / pointDistance).toFloat()
        val cosB = (dy / pointDistance).toFloat()
        val tanA = Math.tan(Math.toRadians(angle.toDouble())).toFloat()
        val h = (pointDistance - arrowHeight.toDouble() - start._outerCircleRadius).toFloat()
        val l = arrowHeight * tanA
        val a = l * sinB
        val b = l * cosB

        val x0 = h * sinB
        val y0 = h * cosB

        val x1 = start._centerX + (h + arrowHeight) * sinB
        val y1 = start._centerY + (h + arrowHeight) * cosB

        val x2 = start._centerX + x0 - b
        val y2 = start._centerY + y0 + a

        val x3 = start._centerX + x0 + b
        val y3 = start._centerY + y0 - a
        val path = Path()
        path.moveTo(x1, y1)
        path.lineTo(x2, y2)
        path.lineTo(x3, y3)
        path.close()
        drawPath(path, paint)
    }

    private fun Canvas.drawLine(start: Point, end: Point, paint: Paint) {
        val dx = end._centerX - start._centerX
        val dy = end._centerY - start._centerY
        val pointDistance = Math.sqrt(Math.pow(Math.abs(dx).toDouble(), 2.0) + Math.pow(Math.abs(dy).toDouble(), 2.0))
        val rx = (dx / pointDistance /*cosA*/ * end._innerCircleRadius).toFloat()
        val ry = (dy / pointDistance /*cosA*/ * end._innerCircleRadius).toFloat()
        save()
        drawLine(start._centerX + rx, start._centerY + ry, end._centerX - rx, end._centerY - ry, paint)
        restore()
    }

    private var moveX = 0F
    private var moveY = 0F
    private var lastPoint: Point? = null
    private var whetherResetPointCompleted: Boolean = true
    private var whetherNeedSetupOriginalPassword: Boolean = false
    private var whetherTouchUp: Boolean = true
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return ev.run {
            when (action) {
                MotionEvent.ACTION_DOWN /*判断点击事件是否在圆心内*/ -> {
                    whetherTouchUp = false
                    if (!whetherResetPointCompleted) return@run super.onTouchEvent(ev)
                    whetherResetPointCompleted = false
                    resetCheckPassword()
                    moveX = x
                    moveY = y
                    points.forEach {
                        it ?: return@forEach
                        if (!isPressedRange(x, y, it) || it.state != PointState.NORMAL) return@forEach
                        it.state = PointState.PRESSED
                        _selectedPointList.add(it)
                        postInvalidate()
                        whetherNeedSetupOriginalPassword = isNeedSetupOriginalPassword()
                        if (whetherNeedSetupOriginalPassword) _originalPasswordList.add(points.indexOf(it))
                        else _checkPasswordList.add(points.indexOf(it))
                    }
                    return@run true
                }
                MotionEvent.ACTION_MOVE -> {
                    points.forEach {
                        if (isPressedRange(x, y, it) && it.state == PointState.NORMAL) {
                            it.state = PointState.PRESSED
                            _selectedPointList.add(it)
                            if (whetherNeedSetupOriginalPassword) _originalPasswordList.add(points.indexOf(it))
                            else _checkPasswordList.add(points.indexOf(it))
                        }
                    }
                    moveX = x
                    moveY = y
                    postInvalidate()
                    return@run true
                }
                MotionEvent.ACTION_UP -> {
                    whetherTouchUp = true
                    if (whetherNeedSetupOriginalPassword && _originalPasswordList.size >= passwordLength) {
                        resetPointToNormalState()
                        whetherResetPointCompleted = true
                        return@run true
                    }

                    if (whetherNeedSetupOriginalPassword && _originalPasswordList.size < passwordLength) {
                        listener?.apply { onStateFailed(CauseState.STATE_ORIGINAL_PASSWORD_LENGTH_TOO_FWE) }
                        resetOriginalPassword()
                        resetPointToNormalState()
                        whetherResetPointCompleted = true
                        return@run true
                    }

                    if (verifyingPasswords()) {
                        _checkPasswordList.forEach {
                            e(_TAG, "checkPassword = $it")
                            points[it].state = PointState.SUCCESS
                        }
                    } else {
                        _checkPasswordList.forEach {
                            e(_TAG, "checkPassword = $it")
                            points[it].state = PointState.ERROR
                        }
                    }
                    postInvalidate()
                    this@Day15GestureLockView.postDelayed({
                        resetPointToNormalState()
                        whetherResetPointCompleted = true
                    }, 600L)
                    return@run true
                }
            }
            return@run super.onTouchEvent(ev)
        }
    }

    fun resetPoint() {
        resetPointToNormalState()
    }

    private fun isNeedSetupOriginalPassword(): Boolean = _originalPasswordList.isEmpty()

    private fun resetPointToNormalState() {
        points.forEach {
            it.state = PointState.NORMAL
        }
        _selectedPointList.clear()
        postInvalidate()
    }

    private fun resetCheckPassword() {
        _checkPasswordList.clear()
    }

    fun resetOriginalPassword() {
        _originalPasswordList.clear()
    }

    private fun verifyingPasswords(): Boolean {
        if (_originalPasswordList.isEmpty()) {
            listener?.apply { onStateFailed(CauseState.STATE_NOT_SETUP_ORIGINAL_PASSWORD) }
            return false
        }
        if (_originalPasswordList.size != _checkPasswordList.size) {
            listener?.apply { onStateFailed(CauseState.STATE_PASSWORD_VALIDATION_ERROR) }
            return false
        }
        for (i in 0 until _originalPasswordList.size) {
            if (_originalPasswordList[i] != _checkPasswordList[i]) {
                e(_TAG, "#################### _originalPassword[$i] = ${_originalPasswordList[i]!!.toInt()}, _checkPasswordList[$i] = ${_checkPasswordList[i]!!.toInt()}")

                listener?.apply { onStateFailed(CauseState.STATE_PASSWORD_VALIDATION_ERROR) }
                return false
            }
        }
        listener?.apply { onSuccessfulUnlock() }
        return true
    }

    private fun isPressedRange(x: Float, y: Float, point: Point)
            : Boolean = x > point._startWidth && x < point._endWidth && y > point._startHeight && y < point._endHeight

    private fun convertPx(unit: Int, value: Int) = TypedValue.applyDimension(unit, value.toFloat(), resources.displayMetrics)

    private fun computePoint() {
        points.clear()
        val equipartitionWidth = measuredWidth.toFloat() / passwordColumn
        val equipartitionHeight = measuredHeight.toFloat() / passwordRow
        val centerX = equipartitionWidth / 2F
        val centerY = equipartitionHeight / 2F
        for (r in 0 until passwordRow) {
            for (c in 0 until passwordColumn) {
                val startWidth = equipartitionWidth * c
                var startHeight = equipartitionHeight * r

                val radius = when (equipartitionWidth > equipartitionHeight) {
                    true -> centerY
                    false -> centerX
                } - passwordMargin * 2

                points.add(Point(
                        radius,
                        radius * 0.25F,
                        centerX + startWidth,
                        centerY + startHeight,
                        startWidth + passwordMargin,
                        startWidth + equipartitionWidth - passwordMargin,
                        startHeight + passwordMargin,
                        startHeight + equipartitionHeight - passwordMargin
                ))
            }
        }
    }

    @IntDef(PointState.ERROR, PointState.NORMAL, PointState.PRESSED, PointState.SUCCESS)
    @Target(AnnotationTarget.FIELD)
    @Retention(AnnotationRetention.SOURCE)
    private annotation class PointStateClubs

    private object PointState {
        const val NORMAL = 0
        const val PRESSED = 1
        const val SUCCESS = 2
        const val ERROR = 3
    }

    interface OnGestureLockEventListener {
        fun onSuccessfulUnlock()
        fun onStateFailed(@CauseStateClubs causeState: Int)
    }

    @IntDef(CauseState.STATE_NOT_SETUP_ORIGINAL_PASSWORD, CauseState.STATE_PASSWORD_VALIDATION_ERROR)
    @Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.SOURCE)
    private annotation class CauseStateClubs

    object CauseState {
        /**
         * The current password does not match the original password
         */
        const val STATE_PASSWORD_VALIDATION_ERROR = -1
        /**
         * original password not setup
         */
        const val STATE_NOT_SETUP_ORIGINAL_PASSWORD = -2

        /**
         * original password length, not conform to security policy
         */
        const val STATE_ORIGINAL_PASSWORD_LENGTH_TOO_FWE = -3

    }

    private companion object Own {
        private class Point(
                val _outerCircleRadius: Float,
                val _innerCircleRadius: Float,
                val _centerX: Float,
                val _centerY: Float,
                val _startWidth: Float,
                val _endWidth: Float,
                val _startHeight: Float,
                val _endHeight: Float,
                @PointStateClubs var state: Int = PointState.NORMAL)

        private class ComputePointRunnable(val _root: Day15GestureLockView) : Runnable {
            var whetherComputeFinished: Boolean = true
            override fun run() {
                if (whetherComputeFinished) {
                    whetherComputeFinished = false
                    _root.computePoint()
                    _root.postInvalidate()
                }
            }
        }
    }
}