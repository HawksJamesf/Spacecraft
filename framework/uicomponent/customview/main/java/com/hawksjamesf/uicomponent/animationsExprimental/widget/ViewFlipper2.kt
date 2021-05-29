package com.hawksjamesf.uicomponent.animationsExprimental.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.animation.Animation
import android.widget.ViewFlipper
import androidx.annotation.Px
import com.hawksjamesf.common.util.ConvertUtil

/**
 * Copyright ® $ 2017
 * All right reserved.
 *
 * @author: hawks.jamesf
 * @since: Apr/06/2020  Mon
 */
class ViewFlipper2 : ViewFlipper {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    fun showAnimation(adapter: AnimatorListenerAdapter? = null) {
        buildAnimation(0f, object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                postDelayed({
                    showNext()
                    startFlipping()
                }, 500)

            }
        })
    }

    fun hideAnimation(adapter: AnimatorListenerAdapter? = null) {
        buildAnimation(ConvertUtil.dp2px(-244f).toFloat(), object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                postDelayed({
                    stopFlipping()
                }, 500)
            }
        })
    }

    override fun setInAnimation(inAnimation: Animation?) {
        super.setInAnimation(inAnimation)
        inAnimation?.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                Log.d("cjf", "hideAnimation: ${displayedChild}")
                if (displayedChild == childCount - 1) {
                    hideAnimation()
                }
            }

            override fun onAnimationStart(animation: Animation?) {
            }
        })
    }


    var animator: ObjectAnimator? = null
    private fun buildAnimation(@Px value: Float, adapter: AnimatorListenerAdapter?) {
        if (animator?.isRunning == true) {
            animator?.cancel()
            animator?.removeAllListeners()
        }
        animator = ObjectAnimator.ofFloat(this, "translationX", value)
        animator?.duration = 1000L
        if (adapter != null) {
            animator?.addListener(adapter)
        }
        animator?.start()
    }
}