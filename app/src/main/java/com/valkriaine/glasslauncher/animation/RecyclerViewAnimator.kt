package com.valkriaine.glasslauncher.animation

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.facebook.rebound.SimpleSpringListener
import com.facebook.rebound.Spring
import com.facebook.rebound.SpringConfig
import com.facebook.rebound.SpringSystem

class RecyclerViewAnimator(private val mRecyclerView: RecyclerView) {
    private val mHeight: Int = mRecyclerView.resources.displayMetrics.heightPixels
    private val mSpringSystem: SpringSystem = SpringSystem.create()
    private var mFirstViewInit = true
    private var mLastPosition = -1
    private var mStartDelay: Int
    fun onCreateViewHolder(item: View) {
        /**
         * mFirstViewInit is used because we only want to show animation once at initialization.
         * (onCreateViewHolder can be called after if you use multiple view types).
         */
        if (mFirstViewInit) {
            slideInBottom(
                item,
                mStartDelay,
                INIT_TENSION,
                INIT_FRICTION
            )
            mStartDelay += 70
        }
    }

    fun onBindViewHolder(item: View, position: Int) {
        /**
         * After init, animate once item by item when user scroll down.
         */
        if (!mFirstViewInit && position > mLastPosition) {
            slideInBottom(
                item,
                0,
                SCROLL_TENSION,
                SCROLL_FRICTION
            )
            mLastPosition = position
        }
    }

    private fun slideInBottom(
        item: View,
        delay: Int,
        tension: Int,
        friction: Int
    ) { // Move item far outside the RecyclerView
        item.translationY = mHeight.toFloat()
        val startAnimation = Runnable {
            val config = SpringConfig(tension.toDouble(), friction.toDouble())
            val spring = mSpringSystem.createSpring()
            spring.springConfig = config
            spring.addListener(object : SimpleSpringListener() {
                override fun onSpringUpdate(spring: Spring) {
                    /**
                     * Decrease translationY until 0.
                     */
                    /**
                     * Decrease translationY until 0.
                     */
                    /**
                     * Decrease translationY until 0.
                     */
                    /**
                     * Decrease translationY until 0.
                     */
                    val `val` = (mHeight - spring.currentValue).toFloat()
                    item.translationY = `val`
                }

                override fun onSpringEndStateChange(spring: Spring) {
                    mFirstViewInit = false
                }
            })
            // Set the spring in motion; moving from 0 to height
            spring.endValue = mHeight.toDouble()
        }
        mRecyclerView.postDelayed(startAnimation, delay.toLong())
    }

    companion object {
        /**
         * Initial delay before to show items - in ms
         */
        private const val INIT_DELAY = 100
        /**
         * Initial entrance tension parameter.
         * See https://facebook.github.io/rebound/
         */
        private const val INIT_TENSION = 250
        /**
         * Initial entrance friction parameter.
         */
        private const val INIT_FRICTION = 25
        /**
         * Scroll entrance animation tension parameter.
         */
        private const val SCROLL_TENSION = 250
        /**
         * Scroll entrance animation friction parameter.
         */
        private const val SCROLL_FRICTION = 25
    }

    init {
        // Use height of RecyclerView to slide-in items from bottom.
        mStartDelay =
            INIT_DELAY
    }
}
