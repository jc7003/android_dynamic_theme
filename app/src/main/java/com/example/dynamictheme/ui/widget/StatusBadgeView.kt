package com.example.dynamictheme.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.withStyledAttributes
import com.example.dynamictheme.R
import kotlin.math.min

/**
 * Example custom view drawn with Paint (nothing in Material styles it for us).
 *
 * Colors and radius come from styleable attrs whose defaults are theme attrs
 * (`Widget.App.StatusBadge` → `?attr/colorSecondary` → `@color/token_secondary`), so the
 * JSON theme applies with zero theme code here: a theme change recreates the screen,
 * which constructs this view again with the overridden token values.
 */
class StatusBadgeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.statusBadgeStyle,
) : View(context, attrs, defStyleAttr) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 13f, resources.displayMetrics)
    }
    private val rect = RectF()
    private val padH = dp(12f)
    private val padV = dp(6f)
    private var cornerRadius = 0f

    var text: String = ""
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    init {
        // Resolve once at construction. Never cache these in a static/companion field:
        // a theme change recreates the screen, which re-runs this with the new tokens.
        context.withStyledAttributes(attrs, R.styleable.StatusBadgeView, defStyleAttr, R.style.Widget_App_StatusBadge) {
            bgPaint.color = getColor(R.styleable.StatusBadgeView_statusBadgeColor, Color.GRAY)
            textPaint.color = getColor(R.styleable.StatusBadgeView_statusBadgeTextColor, Color.WHITE)
            cornerRadius = getDimension(R.styleable.StatusBadgeView_statusBadgeCornerRadius, 0f)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val fm = textPaint.fontMetrics
        val w = (textPaint.measureText(text) + 2 * padH).toInt() + paddingLeft + paddingRight
        val h = (fm.descent - fm.ascent + 2 * padV).toInt() + paddingTop + paddingBottom
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize(h, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        rect.set(
            paddingLeft.toFloat(), paddingTop.toFloat(),
            (width - paddingRight).toFloat(), (height - paddingBottom).toFloat(),
        )
        val r = min(cornerRadius, rect.height() / 2)
        canvas.drawRoundRect(rect, r, r, bgPaint)
        val fm = textPaint.fontMetrics
        canvas.drawText(text, rect.centerX(), rect.centerY() - (fm.ascent + fm.descent) / 2, textPaint)
    }

    private fun dp(v: Float) = v * resources.displayMetrics.density
}
