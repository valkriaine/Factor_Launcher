package com.valkriaine.glasslauncher.animation

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import android.widget.EdgeEffect
import androidx.core.widget.NestedScrollView
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.RecyclerView.EdgeEffectFactory.DIRECTION_BOTTOM
import androidx.recyclerview.widget.RecyclerView.EdgeEffectFactory.DIRECTION_TOP
import java.lang.reflect.Field
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.reflect.KMutableProperty0

inline fun <reified T> getField(name: String): Field {
    return T::class.java.getDeclaredField(name).apply {
        isAccessible = true
    }
}


internal object OverScroll {
    private const val OVERSCROLL_DAMP_FACTOR = 0.02f

    /**
     * This curve determines how the effect of scrolling over the limits of the page diminishes
     * as the user pulls further and further from the bounds
     *
     * @param f The percentage of how much the user has overscrolled.
     * @return A transformed percentage based on the influence curve.
     */
    private fun overScrollInfluenceCurve(f: Float): Float {
        var f1 = f
        f1 -= 1.0f
        return f * f * f + 1.0f
    }

    /**
     * @param amount The original amount overscrolled.
     * @param max The maximum amount that the View can overscroll.
     * @return The dampened overscroll amount.
     */
    fun dampedScroll(amount: Float, max: Int): Int {
        if (amount.compareTo(0f) == 0) return 0
        var f = amount / max
        f = f / abs(f) * overScrollInfluenceCurve(abs(f))

        // Clamp this factor, f, to -1 < f < 1
        if (abs(f) >= 1) {
            f /= abs(f)
        }
        return (OVERSCROLL_DAMP_FACTOR * f * max * 6).roundToInt()
    }
}


class SpringScrollView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr)
{

    private val springManager = SpringEdgeEffect.Manager(this)


    private var shouldTranslateSelf = true

    private var isTopFadingEdgeEnabled = true

    init {
        getField<NestedScrollView>("mEdgeGlowTop").set(this, springManager.createEdgeEffect(DIRECTION_BOTTOM, true))
        getField<NestedScrollView>("mEdgeGlowBottom").set(this, springManager.createEdgeEffect(DIRECTION_BOTTOM))
        overScrollMode = View.OVER_SCROLL_ALWAYS
    }

    override fun draw(canvas: Canvas) {
        springManager.withSpring(canvas, shouldTranslateSelf) {
            super.draw(canvas)
            false
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        springManager.withSpring(canvas, !shouldTranslateSelf) {
            super.dispatchDraw(canvas)
            false
        }
    }

    override fun getTopFadingEdgeStrength(): Float {
        return if (isTopFadingEdgeEnabled) super.getTopFadingEdgeStrength() else 0f
    }
}


class KFloatPropertyCompat(private val property: KMutableProperty0<Float>, name: String) : FloatPropertyCompat<Any>(name) {

    override fun getValue(`object`: Any) = property.get()

    override fun setValue(`object`: Any, value: Float) {
        property.set(value)
    }
}


class SpringEdgeEffect(
    context: Context,
    private val getMax: () -> Int,
    private val target: KMutableProperty0<Float>,
    private val activeEdge: KMutableProperty0<SpringEdgeEffect?>,
    private val velocityMultiplier: Float,
    private val reverseAbsorb: Boolean) : EdgeEffect(context)
{


    private val spring = SpringAnimation(this, KFloatPropertyCompat(target, "value"), 0f).apply {
        spring = SpringForce(0f).setStiffness(250f).setDampingRatio(0.8f)
    }
    private var distance = 0f

    override fun draw(canvas: Canvas) = false

    override fun onAbsorb(velocity: Int) {
        if (reverseAbsorb) {
            releaseSpring(-velocityMultiplier * velocity)
        } else {
            releaseSpring(velocityMultiplier * velocity)
        }
    }

    override fun onPull(deltaDistance: Float, displacement: Float) {
        activeEdge.set(this)
        distance += deltaDistance * (velocityMultiplier * 5)
        target.set(OverScroll.dampedScroll(distance * getMax(), getMax()).toFloat())
    }

    override fun onRelease() {
        distance = 0f
        releaseSpring(0f)
    }

    private fun releaseSpring(velocity: Float) {
        spring.setStartVelocity(velocity)
        spring.setStartValue(target.get())
        spring.start()
    }

    class Manager(private val view: View) {

        var shiftX = 0f
            set(value) {
                if (field != value) {
                    field = value
                    view.invalidate()
                }
            }
        var shiftY = 0f
            set(value) {
                if (field != value) {
                    field = value
                    view.invalidate()
                }
            }

        private var activeEdgeY: SpringEdgeEffect? = null
            set(value) {
                if (field != value) {
                    field?.run { value?.distance = distance }
                }
                field = value
            }

        inline fun withSpring(canvas: Canvas, allow: Boolean = true, body: () -> Boolean): Boolean {
            val result: Boolean
            if ((shiftX == 0f && shiftY == 0f) || !allow) {
                result = body()
            } else {
                canvas.translate(shiftX, shiftY)
                result = body()
                canvas.translate(-shiftX, -shiftY)
            }
            return result
        }

        fun createEdgeEffect(direction: Int, reverseAbsorb: Boolean = false): EdgeEffect? {
            return when (direction) {
                DIRECTION_TOP -> SpringEdgeEffect(view.context, view::getHeight, ::shiftY, ::activeEdgeY, 0.4f, reverseAbsorb)
                DIRECTION_BOTTOM -> SpringEdgeEffect(view.context, view::getWidth, ::shiftY, ::activeEdgeY, -0.4f, reverseAbsorb)
                else -> null
            }
        }
    }
}