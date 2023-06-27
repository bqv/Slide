package uz.shift.colorpicker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import ltd.ucode.slide.R

class LineColorPicker(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    /**
     * Return current picker palete
     */
    var colors: IntArray = IntArray(1)
        set(colors: IntArray) {
            // TODO: selected color can be NOT in set of colors
            // FIXME: colors can be null
            field = colors
            if (!containsColor(colors, color)) {
                color = colors[0]
            }
            recalcCellSize()
            invalidate()
        }

    // indicate if nothing selected
    var isColorSelected = false
    private val paint: Paint = Paint()
    private val rect = Rect()

    /**
     * Return currently selected color.
     */
    var color: Int
        private set
    private var onColorChanged: OnColorChangedListener? = null
    private var cellSize = 0
    private var mOrientation = 0
    private var isClick = false
    private var screenW = 0
    private var screenH = 0

    init {
        colors = if (isInEditMode) {
            Palette.DEFAULT
        } else {
            IntArray(1)
        }
    }

    init {
        paint.style = Paint.Style.FILL
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.LineColorPicker, 0, 0)
        try {
            mOrientation = a.getInteger(
                R.styleable.LineColorPicker_android_orientation,
                LinearLayout.HORIZONTAL
            )
            if (!isInEditMode) {
                val colorsArrayResId = a.getResourceId(R.styleable.LineColorPicker_colors, -1)
                if (colorsArrayResId > 0) {
                    colors = context.resources.getIntArray(colorsArrayResId)
                }
            }
            val selected = a.getInteger(R.styleable.LineColorPicker_selectedColorIndex, -1)
            if (selected != -1) {
                val currentColors = colors
                val currentColorsLength = currentColors?.size ?: 0
                if (selected < currentColorsLength) {
                    setSelectedColorPosition(selected)
                }
            }
        } finally {
            a.recycle()
        }
        color = colors!![0]
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mOrientation == LinearLayout.HORIZONTAL) {
            drawHorizontalPicker(canvas)
        } else {
            drawVerticalPicker(canvas)
        }
    }

    private fun drawVerticalPicker(canvas: Canvas) {
        rect.left = 0
        rect.top = 0
        rect.right = canvas.width
        rect.bottom = 0

        // 8%
        val margin = Math.round(canvas.width * 0.08f)
        for (color in colors!!) {
            paint.color = color
            rect.top = rect.bottom
            rect.bottom += cellSize
            if (isColorSelected && color == this.color) {
                rect.left = 0
                rect.right = canvas.width
            } else {
                rect.left = margin
                rect.right = canvas.width - margin
            }
            canvas.drawRect(rect, paint)
        }
    }

    private fun drawHorizontalPicker(canvas: Canvas) {
        rect.left = 0
        rect.top = 0
        rect.right = 0
        rect.bottom = canvas.height

        // 8%
        val margin = Math.round(canvas.height * 0.08f)
        for (color in colors!!) {
            paint.color = color
            rect.left = rect.right
            rect.right += cellSize
            if (isColorSelected && color == this.color) {
                rect.top = 0
                rect.bottom = canvas.height
            } else {
                rect.top = margin
                rect.bottom = canvas.height - margin
            }
            canvas.drawRect(rect, paint)
        }
    }

    private fun onColorChanged(color: Int) {
        if (onColorChanged != null) {
            onColorChanged!!.onColorChanged(color)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val actionId = event.action
        val newColor: Int
        when (actionId) {
            MotionEvent.ACTION_DOWN -> isClick = true
            MotionEvent.ACTION_UP -> {
                newColor = getColorAtXY(event.x, event.y)
                setSelectedColor(newColor)
                if (isClick) {
                    performClick()
                }
            }

            MotionEvent.ACTION_MOVE -> {
                newColor = getColorAtXY(event.x, event.y)
                setSelectedColor(newColor)
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> isClick = false
            else -> {}
        }
        return true
    }

    /**
     * Return color at x,y coordinate of view.
     */
    private fun getColorAtXY(x: Float, y: Float): Int {

        // FIXME: colors.length == 0 -> division by ZERO.s
        if (mOrientation == LinearLayout.HORIZONTAL) {
            var left = 0
            var right = 0
            for (color in colors!!) {
                left = right
                right += cellSize
                if (left <= x && right >= x) {
                    return color
                }
            }
        } else {
            var top = 0
            var bottom = 0
            for (color in colors!!) {
                top = bottom
                bottom += cellSize
                if (y >= top && y <= bottom) {
                    return color
                }
            }
        }
        return color
    }

    override fun onSaveInstanceState(): Parcelable? {
        // begin boilerplate code that allows parent classes to save state
        val superState = super.onSaveInstanceState()
        val ss = SavedState(superState)
        // end
        ss.selectedColor = color
        ss.isColorSelected = isColorSelected
        return ss
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        // begin boilerplate code so parent classes can restore state
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        val ss = state
        super.onRestoreInstanceState(ss.superState)
        // end
        color = ss.selectedColor
        isColorSelected = ss.isColorSelected
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        screenW = w
        screenH = h
        recalcCellSize()
        super.onSizeChanged(w, h, oldw, oldh)
    }
    // @Override
    // protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    // int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
    // int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
    // this.setMeasuredDimension(parentWidth, parentHeight);
    // super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    // }
    /**
     * Set selected color as color value from palette.
     */
    fun setSelectedColor(color: Int) {

        // not from current palette
        if (!containsColor(colors, color)) {
            return
        }

        // do we need to re-draw view?
        if (!isColorSelected || this.color != color) {
            this.color = color
            isColorSelected = true
            invalidate()
            onColorChanged(color)
        }
    }

    /**
     * Set selected color as index from palete
     */
    fun setSelectedColorPosition(position: Int) {
        setSelectedColor(colors!![position])
    }

    private fun recalcCellSize() {
        cellSize = if (mOrientation == LinearLayout.HORIZONTAL) {
            Math.round(screenW / (colors!!.size * 1f))
        } else {
            Math.round(screenH / (colors!!.size * 1f))
        }
    }

    /**
     * Return true if palette contains this color
     */
    private fun containsColor(colors: IntArray?, c: Int): Boolean {
        for (color in colors!!) {
            if (color == c) return true
        }
        return false
    }

    /**
     * Set onColorChanged listener
     *
     * @param l
     */
    fun setOnColorChangedListener(l: OnColorChangedListener?) {
        onColorChanged = l
    }

    internal class SavedState : BaseSavedState {
        var selectedColor = 0
        var isColorSelected = false

        constructor(superState: Parcelable?) : super(superState) {}
        private constructor(`in`: Parcel) : super(`in`) {
            selectedColor = `in`.readInt()
            isColorSelected = `in`.readInt() == 1
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(selectedColor)
            out.writeInt(if (isColorSelected) 1 else 0)
        }

        companion object {
            // required field that makes Parcelables from a Parcel
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState?> = object : Parcelable.Creator<SavedState?> {
                override fun createFromParcel(`in`: Parcel): SavedState? {
                    return SavedState(`in`)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }
}
