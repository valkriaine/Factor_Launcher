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

    /**
     * Initial delay before to show items - in ms
     */
    private val delay = 50
    /**
     * Initial entrance tension parameter.
     * See https://facebook.github.io/rebound/
     */
    private val tension = 250
    /**
     * Initial entrance friction parameter.
     */
    private val friction = 25

    fun onCreateViewHolder(item: View) {
        /**
         * mFirstViewInit is used because we only want to show animation once at initialization.
         * (onCreateViewHolder can be called after if you use multiple view types).
         */
        if (mFirstViewInit) {
            slideInBottom(
                item,
                mStartDelay
            )
            this.mStartDelay += 70
        }
    }

    fun onBindViewHolder(item: View, position: Int) {
        /**
         * After init, animate once item by item when user scroll down.
         */
        if (!mFirstViewInit && position > mLastPosition) {
            slideInBottom(
                item,
                0

            )
            mLastPosition = position
        }
    }

    private fun slideInBottom(
        item: View,
        delay: Int
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

    init {
        // Use height of RecyclerView to slide-in items from bottom.
        mStartDelay =
            delay
    }
}
