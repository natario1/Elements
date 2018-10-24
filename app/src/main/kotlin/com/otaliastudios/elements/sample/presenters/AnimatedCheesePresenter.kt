package com.otaliastudios.elements.sample.presenters

import android.content.Context
import android.view.View
import android.view.ViewPropertyAnimator
import android.widget.TextView
import com.otaliastudios.elements.AnimationType
import com.otaliastudios.elements.Presenter
import com.otaliastudios.elements.extensions.SimplePresenter
import com.otaliastudios.elements.sample.R
import java.util.*

class AnimatedCheesePresenter(context: Context) : SimplePresenter<String>(context, R.layout.item_cheese, 0, { view, item ->
    (view as TextView).text = item
}) {
    override fun animates(animation: AnimationType, holder: Holder): Boolean {
        return animation.isAdd() || animation.isRemove()
    }

    private val random = Random()

    override fun onPreAnimate(animation: AnimationType, holder: Holder, view: View) {
        super.onPreAnimate(animation, holder, view)
        if (animation.isAdd()) {
            view.translationX = (random.nextFloat() * 2F - 1F) * view.width
            view.translationY = (random.nextFloat() * 2F - 1F) * view.width
        } else if (animation.isRemove()) {
            view.translationX = 0F
            view.translationY = 0F
        }
    }

    override fun onAnimate(animation: AnimationType, holder: Holder, animator: ViewPropertyAnimator) {
        super.onAnimate(animation, holder, animator)
        animator.setDuration(300L)
        if (animation.isAdd()) {
            animator.translationX(0F).translationY(0F)
        } else if (animation.isRemove()) {
            val translationX = (random.nextFloat() * 2F - 1F) * holder.itemView.width
            val translationY = (random.nextFloat() * 2F - 1F) * holder.itemView.width
            animator.translationX(translationX).translationY(translationY)
        }
    }

    override fun onPostAnimate(animation: AnimationType, holder: Holder, view: View) {
        super.onPostAnimate(animation, holder, view)
        view.translationX = 0F
        view.translationY = 0F
    }
}