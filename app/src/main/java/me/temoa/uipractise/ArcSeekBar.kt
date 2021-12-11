package me.temoa.uipractise

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Region
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.properties.Delegates


/**
 * Created by lai
 * on 2021/12/7
 */
class ArcSeekBar @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet?,
  defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

  private val DEFAULT_WIDTH = 220.dp
  private val DEFAULT_ARC_STROKE_WIDTH = 32.dp
  private val TOUCH_SLOP = ViewConfiguration.get(context).scaledTouchSlop

  private var mRadius: Float = 0f

  private var mDownX: Float = 0f
  private var mDownY: Float = 0f
  private var mPointX: Float = 0f
  private var mPointY: Float = 0f
  private var mCenterX: Float = 0f
  private var mCenterY: Float = 0f
  private var mHeight: Float = 0f
  private var mWidth: Float = 0f

  private var mSliderX: Float = Float.MAX_VALUE
  private var mSliderY: Float = Float.MAX_VALUE
  private val mSliderOffset: Float = 4.dp
  private var mSliderRadius: Float = DEFAULT_ARC_STROKE_WIDTH / 2f + mSliderOffset
  private val mSliderPaint: Paint by lazy(LazyThreadSafetyMode.NONE) {
    Paint().apply {
      style = Paint.Style.FILL
      strokeCap = Paint.Cap.ROUND
      color = Color.DKGRAY
      isAntiAlias = true
    }
  }

  private var mArcRectF: RectF = RectF()
  private var mArcStrokeWidth: Float = DEFAULT_ARC_STROKE_WIDTH
  private val mArcPaint: Paint by lazy(LazyThreadSafetyMode.NONE) {
    Paint().apply {
      strokeWidth = mArcStrokeWidth
      style = Paint.Style.STROKE
      strokeCap = Paint.Cap.ROUND
      color = Color.LTGRAY
      isAntiAlias = true
    }
  }
  private val mArcProgressPaint: Paint by lazy(LazyThreadSafetyMode.NONE) {
    Paint().apply {
      strokeWidth = mArcStrokeWidth
      style = Paint.Style.STROKE
      strokeCap = Paint.Cap.ROUND
      color = Color.DKGRAY
      isAntiAlias = true
    }
  }
  private val mArcPaint2: Paint by lazy(LazyThreadSafetyMode.NONE) {
    Paint().apply {
      strokeWidth = mArcStrokeWidth * 2
      style = Paint.Style.STROKE
      strokeCap = Paint.Cap.ROUND
      color = Color.RED
      isAntiAlias = true
    }
  }

  private val mSeekPath = Path()
  private val mBorderPath = Path()
  private val mArcRegion = Region()

  private var mCanGoDrag = true
  private var mIsMoved = false

  private var mDragAngle: Float = 45f
  private var mStartAngle: Float by Delegates.observable(45f) { _, _, newValue ->
    mDragAngle = newValue
  }
  private var mSweepAngle: Float = 270f
  private var mMin: Int = 0
  private var mMax: Int = 100
  private var progress: Int = mMin

  val builder: Builder = Builder(this)

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    setSize(
      when (MeasureSpec.getMode(widthMeasureSpec)) {
        MeasureSpec.EXACTLY -> {
          MeasureSpec.getSize(widthMeasureSpec).toFloat()
        }
        else -> {
          DEFAULT_WIDTH
        }
      }
    )
    setMeasuredDimension(mWidth.toInt(), mHeight.toInt())
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    setSize(w.toFloat())
    setArcRectF()

    mSeekPath.reset()
    val offset = mArcStrokeWidth / 2
    val arcRecF = RectF(offset, offset, mWidth - offset, mHeight - offset)
    mSeekPath.addArc(arcRecF, mStartAngle + 90f, mSweepAngle)
    mArcPaint2.getFillPath(mSeekPath, mBorderPath)
    mBorderPath.close()
    mArcRegion.setPath(mBorderPath, Region(0, 0, w, h))
  }

  private fun setSize(width: Float) {
    mWidth = width
    mHeight = mWidth
    mCenterX = mWidth / 2f
    mCenterY = mHeight / 2f
    mRadius = (mWidth - mArcStrokeWidth) / 2f
  }

  @SuppressLint("DrawAllocation")
  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    val startAngle = mStartAngle + 90f
    canvas.drawArc(mArcRectF, startAngle, mSweepAngle, false, mArcPaint)
    if (mDragAngle - this.mStartAngle < 0) {
      mDragAngle = this.mStartAngle
    }
    canvas.drawArc(mArcRectF, startAngle, mDragAngle - this.mStartAngle, false, mArcProgressPaint)

    canvas.save()
    canvas.translate(mCenterX, mCenterY)
    canvas.scale(-1f, 1f)
    if (mSliderX != Float.MAX_VALUE || mSliderY != Float.MAX_VALUE) {
      canvas.drawCircle(mSliderX, mSliderY, mSliderRadius, mSliderPaint)
    }
    canvas.restore()
  }

  private fun setArcRectF() {
    val offset = mArcStrokeWidth / 2 + mSliderOffset
    mArcRectF.set(offset, offset, mWidth - offset, mHeight - offset)
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(event: MotionEvent): Boolean {
    when (event.action) {
      MotionEvent.ACTION_DOWN -> {
        mDownX = event.x
        mDownY = event.y
        mCanGoDrag = isTouchInArcRegion(event.x, event.y)
        if (mCanGoDrag) {
          mOnProgressChangeListener?.onStartTrackingTouch(this)
        }
      }
      MotionEvent.ACTION_MOVE -> {
        val downX = event.x
        val downY = event.y
        val moveX = abs(mDownX - downX)
        val moveY = abs(mDownY - downY)
        if (mCanGoDrag) {
          if (moveX > TOUCH_SLOP || moveY > TOUCH_SLOP) {
            mIsMoved = true
            convertCoordinates(downX, downY)
          }
          mCanGoDrag = isTouchInArcRegion(downX, downY)
        }
      }
      MotionEvent.ACTION_UP -> {
        if (!mIsMoved) {
          val downX = event.x
          val downY = event.y
          convertCoordinates(downX, downY)
        }
        mCanGoDrag = true
        mIsMoved = false
        mOnProgressChangeListener?.onStopTrackingTouch(this)
      }
      MotionEvent.ACTION_CANCEL -> {
        mCanGoDrag = true
        mIsMoved = false
        mOnProgressChangeListener?.onStopTrackingTouch(this)
      }
    }
    return true
  }

  private fun isTouchInArcRegion(downX: Float, downY: Float): Boolean {
    return mArcRegion.contains(downX.toInt(), downY.toInt())
  }

  private fun convertCoordinates(downX: Float, downY: Float) {
    mPointY = downY - mCenterY
    mPointX = -(downX - mCenterX)

    var del = 0f
    var isReverse = false
    if (mPointX > 0f && mPointY > 0f) {
      // 1
      del = 0f
      isReverse = false
    } else if (mPointX > 0f && mPointY < 0f) {
      // 4
      del = 90f
      isReverse = true
    } else if (mPointX < 0f && mPointY < 0f) {
      // 3
      del = 180f
      isReverse = false
    } else if (mPointX < 0f && mPointY > 0f) {
      // 2
      del = 270f
      isReverse = true
    }

    val v = if (isReverse) {
      abs(mPointX) / sqrt(mPointX * mPointX + mPointY * mPointY)
    } else {
      abs(mPointY) / sqrt(mPointX * mPointX + mPointY * mPointY)
    }
    val tmp = acos(v)
    val angle = Math.toDegrees(tmp.toDouble()) + del
    setAngle(angle, true)
  }

  private fun setAngle(angle: Double, isUser: Boolean) {
    var temp = angle
    if (temp < mStartAngle) {
      temp = mStartAngle.toDouble()
    }

    val maxAngle = mStartAngle + mSweepAngle
    if (temp > maxAngle) {
      temp = maxAngle.toDouble()
    }

    val offset = mRadius - mSliderOffset
    mSliderX = (sin(Math.toRadians(temp)) * offset).toFloat()
    mSliderY = (cos(Math.toRadians(temp)) * offset).toFloat()
    mDragAngle = temp.toFloat()
    postInvalidate()

    progress = ((temp - mStartAngle) / mSweepAngle * (mMax - mMin)).toInt() + mMin
    mOnProgressChangeListener?.onProgressChanged(this, progress, isUser)
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private var mOnProgressChangeListener: OnProgressChangeListener? = null

  fun setOnProgressChangeListener(onProgressChangeListener: OnProgressChangeListener?) {
    mOnProgressChangeListener = onProgressChangeListener
  }

  interface OnProgressChangeListener {

    fun onProgressChanged(seekBar: ArcSeekBar, progress: Int, isUser: Boolean)

    fun onStartTrackingTouch(seekBar: ArcSeekBar)

    fun onStopTrackingTouch(seekBar: ArcSeekBar)
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

  class Builder(private val arcSeekBar: ArcSeekBar) {

    private var startAngle: Float = 45f
    private var sweepAngle: Float = 270f
    private var min: Int = 0
    private var max: Int = 100

    fun setStartAngle(value: Float): Builder {
      startAngle = value
      return this
    }

    fun setSweepAngle(value: Float): Builder {
      sweepAngle = value
      return this
    }

    fun setMin(value: Int): Builder {
      min = value
      return this
    }

    fun setMax(value: Int): Builder {
      max = value
      return this
    }

    fun build() {
      arcSeekBar.reBuild(startAngle, sweepAngle, min, max)
    }
  }

  fun reBuild(startAngle: Float, sweepAngle: Float, min: Int, max: Int) {
    mStartAngle = startAngle
    mSweepAngle = sweepAngle
    mMin = min
    mMax = max
    reset()
    postInvalidate()
  }

  private fun reset() {
    mDownX = 0f
    mDownY = 0f
    mPointX = 0f
    mPointY = 0f
    mSliderX = Float.MAX_VALUE
    mSliderY = Float.MAX_VALUE
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

  fun setProgress(value: Int) {
    post {
      var temp = value
      if (temp > mMax) {
        temp = mMax
      }
      if (temp < mMin) {
        temp = mMin
      }
      val angle = mSweepAngle * (temp - mMin) / (mMax - mMin) + mStartAngle
      setAngle(angle.toDouble(), false)
    }
  }

  fun getProgress(): Int {
    return progress
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

  override fun onSaveInstanceState(): Parcelable {
    super.onSaveInstanceState()
    return Bundle().apply {
      putParcelable("superState", super.onSaveInstanceState())
      putInt(KEY_PROGRESS, progress)
    }
  }

  override fun onRestoreInstanceState(state: Parcelable) {
    var state2: Parcelable? = null
    if (state is Bundle) {
      progress = state.getInt(KEY_PROGRESS)
      state2 = state.getParcelable("superState")
    }
    setProgress(progress)
    super.onRestoreInstanceState(state2)
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private val Int.dp: Float
    get() {
      return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), Resources.getSystem().displayMetrics)
    }

  companion object {
    private const val KEY_PROGRESS = "KEY_PROGRESS"
  }
}