package com.otaliastudios.elements

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.view.ViewPropertyAnimator
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator

public class Animator(private val adapter: Adapter) : SimpleItemAnimator() {
    
    private companion object {
        private val defaultInterpolator: TimeInterpolator by lazy { ValueAnimator().interpolator }
    }

    private fun <T> MutableList<T>.forEachReversed(action: (T) -> Unit) {
        for (i in indices.reversed()) {
            action(get(i))
        }
    }

    private fun <T> MutableList<T>.consumeReversed(action: (T) -> Unit) {
        for (i in indices.reversed()) {
            action(get(i))
            removeAt(i)
        }
    }

    private fun Presenter.Holder.presenter(): Presenter<*> {
        return adapter.presenterForType(itemViewType)
    }

    private fun Presenter.Holder.animates(animation: AnimationType): Boolean {
        return presenter().animates(animation, this)
    }

    private fun Presenter.Holder.onPreAnimate(animation: AnimationType) {
        return presenter().onPreAnimate(animation, this, itemView)
    }

    private fun Presenter.Holder.onPostAnimate(animation: AnimationType) {
        return presenter().onPostAnimate(animation, this, itemView)
    }

    private fun Presenter.Holder.onAnimate(animation: AnimationType, animator: ViewPropertyAnimator) {
        return presenter().onAnimate(animation, this, animator)
    }

    private class MoveInfo(var holder: Presenter.Holder,
                           var fromX: Int,
                           var fromY: Int,
                           var toX: Int,
                           var toY: Int)

    private class ChangeInfo(var oldHolder: Presenter.Holder?,
                             var newHolder: Presenter.Holder?, 
                             var fromX: Int = 0, 
                             var fromY: Int = 0, 
                             var toX: Int = 0, 
                             var toY: Int = 0)

    // Pending operations. We will execute them in runPendingAnimations() (or after).
    private val pendingRemovals = mutableListOf<Presenter.Holder>()
    private val pendingAdditions = mutableListOf<Presenter.Holder>()
    private val pendingMoves = mutableListOf<MoveInfo>()
    private val pendingChanges = mutableListOf<ChangeInfo>()

    // Scheduled operations. This means we had some removals and we are waiting for the removal to
    // be completed before running the other kinds of animations.
    private val scheduledAdditions = mutableListOf(mutableListOf<Presenter.Holder>())
    private val scheduledMoves = mutableListOf(mutableListOf<MoveInfo>())
    private val scheduledChanges = mutableListOf(mutableListOf<ChangeInfo>())

    // Operations that are actually being run at the moment.
    private val runningAdditions = mutableListOf<Presenter.Holder>()
    private val runningMoves = mutableListOf<Presenter.Holder>()
    private val runningRemovals = mutableListOf<Presenter.Holder>()
    private val runningChanges = mutableListOf<Presenter.Holder>()

    /**
     * Prepare the holder for a new animation. Apply the time interpolator
     * and end any animations related to it.
     */
    private fun resetAnimation(holder: Presenter.Holder) {
        holder.itemView.animate().interpolator = defaultInterpolator
        endAnimation(holder)
    }

    /**
     * Whether we are running. Depends on our lists.
     */
    override fun isRunning(): Boolean {
        return (!pendingAdditions.isEmpty()
                || !pendingChanges.isEmpty()
                || !pendingMoves.isEmpty()
                || !pendingRemovals.isEmpty()
                || !runningMoves.isEmpty()
                || !runningRemovals.isEmpty()
                || !runningAdditions.isEmpty()
                || !runningChanges.isEmpty()
                || !scheduledMoves.isEmpty()
                || !scheduledAdditions.isEmpty()
                || !scheduledChanges.isEmpty())
    }

    /**
     * Called when this holder has been removed. We ask the presenter
     * to understand whether the item should be animated out or not.
     */
    override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
        if (ElementsLogger.verbose()) {
            ElementsLogger.v("Animator animateRemove called for ${holder.hashCode()}")
        }
        holder as Presenter.Holder
        resetAnimation(holder)
        return if (holder.animates(AnimationType.REMOVE)) {
            resetAnimation(holder)
            holder.onPreAnimate(AnimationType.REMOVE)
            pendingRemovals.add(holder)
            true
        } else {
            dispatchRemoveFinished(holder)
            false
        }
    }

    /**
     * Called when this holder has been added. We ask the presenter
     * to understand whether the item should be animated in or not.
     */
    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
        if (ElementsLogger.verbose()) {
            ElementsLogger.v("Animator animateAdd called for ${holder.hashCode()}")
        }
        holder as Presenter.Holder
        resetAnimation(holder)
        return if (holder.animates(AnimationType.ADD)) {
            holder.onPreAnimate(AnimationType.ADD)
            pendingAdditions.add(holder)
            true
        } else {
            dispatchAddFinished(holder)
            false
        }
    }

    /**
     * TODO ask the presenter.
     */
    @Suppress("NAME_SHADOWING")
    override fun animateMove(holder: RecyclerView.ViewHolder, fromX: Int, fromY: Int,
                             toX: Int, toY: Int): Boolean {
        if (ElementsLogger.verbose()) {
            ElementsLogger.v("Animator animateMove called for ${holder.hashCode()}")
        }
        holder as Presenter.Holder
        val view = holder.itemView
        val fromX = fromX + view.translationX.toInt()
        val fromY = fromY + view.translationY.toInt()
        resetAnimation(holder)
        val deltaX = toX - fromX
        val deltaY = toY - fromY
        if (deltaX == 0 && deltaY == 0) {
            ElementsLogger.w("Animator animateMove: finished because delta is 0.")
            dispatchMoveFinished(holder)
            return false
        } else {
            if (ElementsLogger.verbose()) {
                ElementsLogger.v("Animator animateMove: adding move to pendingMoves. deltaX $deltaX deltaY $deltaY.")
            }
        }
        if (deltaX != 0) view.translationX = (-deltaX).toFloat()
        if (deltaY != 0) view.translationY = (-deltaY).toFloat()
        pendingMoves.add(MoveInfo(holder, fromX, fromY, toX, toY))
        return true
    }

    /**
     * TODO ask the presenter.
     */
    override fun animateChange(oldHolder0: RecyclerView.ViewHolder, newHolder0: RecyclerView.ViewHolder?,
                               fromX: Int, fromY: Int, toX: Int, toY: Int): Boolean {
        if (ElementsLogger.verbose()) {
            ElementsLogger.v("Animator animateChange called for ${oldHolder0.hashCode()} and ${newHolder0?.hashCode()}")
        }
        val oldHolder = oldHolder0 as Presenter.Holder
        val newHolder = newHolder0 as? Presenter.Holder
        if (oldHolder === newHolder) {
            // Don't know how to run change animations when the same view holder is re-used.
            // run a move animation to handle position changes.
            if (ElementsLogger.verbose()) {
                ElementsLogger.v("Animator animateChange: Same holders. xRange $fromX $toX yRange $fromY $toY")
            }
            return animateMove(oldHolder, fromX, fromY, toX, toY)
        } else {
            if (ElementsLogger.verbose()) {
                ElementsLogger.v("Animator animateChange: Different holders. xRange $fromX $toX yRange $fromY $toY")
            }
        }
        val prevTranslationX = oldHolder.itemView.translationX
        val prevTranslationY = oldHolder.itemView.translationY
        val prevAlpha = oldHolder.itemView.alpha
        resetAnimation(oldHolder)
        val deltaX = (toX.toFloat() - fromX.toFloat() - prevTranslationX).toInt()
        val deltaY = (toY.toFloat() - fromY.toFloat() - prevTranslationY).toInt()
        // recover prev translation state after ending animation
        oldHolder.itemView.translationX = prevTranslationX
        oldHolder.itemView.translationY = prevTranslationY
        oldHolder.itemView.alpha = prevAlpha
        if (newHolder != null) {
            // carry over translation values
            resetAnimation(newHolder)
            newHolder.itemView.translationX = (-deltaX).toFloat()
            newHolder.itemView.translationY = (-deltaY).toFloat()
            newHolder.itemView.alpha = 0f
        }
        pendingChanges.add(ChangeInfo(oldHolder, newHolder, fromX, fromY, toX, toY))
        return true
    }

    /**
     * At this point we run all pending animations:
     * 1. execute all pendingRemovals now
     * 2. execute the rest with a 'removeDuration' delay, so the animations do not overlap.
     *    This makes capturing values, translations and offsets much easier.
     *
     * This is the reason why we don't have a batchRemovals array.
     * We execute them instantly.
     */
    override fun runPendingAnimations() {
        val hasRemovals = !pendingRemovals.isEmpty()
        val hasMoves = !pendingMoves.isEmpty()
        val hasChanges = !pendingChanges.isEmpty()
        val hasAdditions = !pendingAdditions.isEmpty()
        if (!hasRemovals && !hasMoves && !hasAdditions && !hasChanges) return

        // First, execute remove animations.
        for (holder in pendingRemovals) {
            if (ElementsLogger.verbose()) {
                ElementsLogger.v("Animator runPendingAnimations: executing removals.")
            }
            executeRemoval(holder)
        }
        pendingRemovals.clear()

        // Next, execute move and change animations at the same time.
        if (hasMoves) {
            if (ElementsLogger.verbose()) {
                ElementsLogger.v("Animator runPendingAnimations: scheduling moves.")
            }
            val moves = mutableListOf<MoveInfo>()
            moves.addAll(pendingMoves)
            scheduledMoves.add(moves)
            pendingMoves.clear()
            val mover = Runnable {
                if (ElementsLogger.verbose()) {
                    ElementsLogger.v("Animator runPendingAnimations: executing moves.")
                }
                moves.forEach { executeMove(it) }
                moves.clear()
                scheduledMoves.remove(moves)
            }
            if (hasRemovals) {
                val view = moves[0].holder.itemView
                ViewCompat.postOnAnimationDelayed(view, mover, removeDuration)
            } else {
                mover.run()
            }
        }
        if (hasChanges) {
            if (ElementsLogger.verbose()) {
                ElementsLogger.v("Animator runPendingAnimations: scheduling changes.")
            }
            val changes = mutableListOf<ChangeInfo>()
            changes.addAll(pendingChanges)
            scheduledChanges.add(changes)
            pendingChanges.clear()
            val changer = Runnable {
                if (ElementsLogger.verbose()) {
                    ElementsLogger.v("Animator runPendingAnimations: executing changes. delayed: $hasRemovals")
                }
                changes.forEach { executeChange(it) }
                changes.clear()
                scheduledChanges.remove(changes)
            }
            if (hasRemovals) {
                val view = changes[0].oldHolder!!.itemView
                ViewCompat.postOnAnimationDelayed(view, changer, removeDuration)
            } else {
                changer.run()
            }
        }

        // Next, execute additions after everything else has finished.
        if (hasAdditions) {
            if (ElementsLogger.verbose()) {
                ElementsLogger.v("Animator runPendingAnimations: scheduling additions.")
            }
            val additions = mutableListOf<Presenter.Holder>()
            additions.addAll(pendingAdditions)
            scheduledAdditions.add(additions)
            pendingAdditions.clear()
            val adder = Runnable {
                if (ElementsLogger.verbose()) {
                    ElementsLogger.v("Animator runPendingAnimations: executing additions.")
                }
                additions.forEach { executeAddition(it) }
                additions.clear()
                scheduledAdditions.remove(additions)
            }
            if (hasRemovals || hasMoves || hasChanges) {
                val removeDuration = if (hasRemovals) removeDuration else 0
                val moveDuration = if (hasMoves) moveDuration else 0
                val changeDuration = if (hasChanges) changeDuration else 0
                val totalDelay = removeDuration + Math.max(moveDuration, changeDuration)
                val view = additions[0].itemView
                ViewCompat.postOnAnimationDelayed(view, adder, totalDelay)
            } else {
                adder.run()
            }
        }
    }

    private fun executeRemoval(holder: Presenter.Holder) {
        val animation = holder.itemView.animate().setDuration(removeDuration)
        runningRemovals.add(holder)
        holder.onAnimate(AnimationType.REMOVE, animation)
        animation.setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animator: Animator) {
                dispatchRemoveStarting(holder)
            }

            override fun onAnimationCancel(animation: Animator?) {
                holder.onPostAnimate(AnimationType.REMOVE)
            }

            override fun onAnimationEnd(animator: Animator) {
                animation.setListener(null)
                holder.onPostAnimate(AnimationType.REMOVE)
                dispatchRemoveFinished(holder)
                runningRemovals.remove(holder)
                maybeDispatchFinished()
            }
        }).start()
    }

    private fun executeAddition(holder: Presenter.Holder) {
        val animation = holder.itemView.animate().setDuration(addDuration)
        runningAdditions.add(holder)
        holder.onAnimate(AnimationType.ADD, animation)
        animation.setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animator: Animator) {
                dispatchAddStarting(holder)
            }

            override fun onAnimationCancel(animator: Animator) {
                holder.onPostAnimate(AnimationType.ADD)
            }

            override fun onAnimationEnd(animator: Animator) {
                animation.setListener(null)
                holder.onPostAnimate(AnimationType.ADD)
                dispatchAddFinished(holder)
                runningAdditions.remove(holder)
                maybeDispatchFinished()
            }
        }).start()
    }

    private fun executeMove(moveInfo: MoveInfo) {
        val holder = moveInfo.holder
        val view = moveInfo.holder.itemView
        val deltaX = moveInfo.toX - moveInfo.fromX
        val deltaY = moveInfo.toY - moveInfo.fromY
        if (deltaX != 0) view.animate().translationX(0f)
        if (deltaY != 0) view.animate().translationY(0f)

        val animation = view.animate()
        runningMoves.add(holder)
        animation.setDuration(moveDuration).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animator: Animator) {
                dispatchMoveStarting(holder)
            }

            override fun onAnimationCancel(animator: Animator) {
                if (deltaX != 0) view.translationX = 0f
                if (deltaY != 0) view.translationY = 0f
            }

            override fun onAnimationEnd(animator: Animator) {
                animation.setListener(null)
                dispatchMoveFinished(holder)
                runningMoves.remove(holder)
                maybeDispatchFinished()
            }
        }).start()
    }

    private fun executeChange(changeInfo: ChangeInfo) {
        val oldHolder = changeInfo.oldHolder
        val newHolder = changeInfo.newHolder
        if (oldHolder != null) {
            val oldView = oldHolder.itemView
            val oldViewAnim = oldView.animate().setDuration(changeDuration)
            runningChanges.add(oldHolder)
            oldViewAnim.translationX((changeInfo.toX - changeInfo.fromX).toFloat())
            oldViewAnim.translationY((changeInfo.toY - changeInfo.fromY).toFloat())
            oldViewAnim.alpha(0f).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animator: Animator) {
                    if (ElementsLogger.verbose()) {
                        ElementsLogger.v("Animator executeChange: oldHolder: onAnimationStart")
                    }
                    dispatchChangeStarting(oldHolder, true)
                }

                override fun onAnimationEnd(animator: Animator) {
                    if (ElementsLogger.verbose()) {
                        ElementsLogger.v("Animator executeChange: oldHolder: onAnimationEnd")
                    }
                    oldViewAnim.setListener(null)
                    oldView.alpha = 1f
                    oldView.translationX = 0f
                    oldView.translationY = 0f
                    dispatchChangeFinished(oldHolder, true)
                    runningChanges.remove(oldHolder)
                    maybeDispatchFinished()
                }

                override fun onAnimationCancel(animation: Animator?) {
                    super.onAnimationCancel(animation)
                    if (ElementsLogger.verbose()) {
                        ElementsLogger.v("Animator executeChange: oldHolder: onAnimationCancel")
                    }
                }
            }).start()
        }
        if (newHolder != null) {
            val newView = newHolder.itemView
            val newViewAnimation = newView.animate()
            runningChanges.add(newHolder)
            newViewAnimation.translationX(0f).translationY(0f).setDuration(changeDuration)
                    .alpha(1f).setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animator: Animator) {
                            if (ElementsLogger.verbose()) {
                                ElementsLogger.v("Animator executeChange: newHolder: onAnimationStart")
                            }
                            dispatchChangeStarting(newHolder, false)
                        }

                        override fun onAnimationEnd(animator: Animator) {
                            if (ElementsLogger.verbose()) {
                                ElementsLogger.v("Animator executeChange: newHolder: onAnimationEnd")
                            }
                            newViewAnimation.setListener(null)
                            newView.alpha = 1f
                            newView.translationX = 0f
                            newView.translationY = 0f
                            dispatchChangeFinished(newHolder, false)
                            runningChanges.remove(newHolder)
                            maybeDispatchFinished()
                        }

                        override fun onAnimationCancel(animation: Animator?) {
                            super.onAnimationCancel(animation)
                            if (ElementsLogger.verbose()) {
                                ElementsLogger.v("Animator executeChange: newHolder: onAnimationCancel")
                            }
                        }
                    }).start()
        }
    }

    override fun endAnimation(item: RecyclerView.ViewHolder) {
        item as Presenter.Holder
        val view = item.itemView
        // this will trigger end callback which should set properties to their target values.
        view.animate().cancel()
        // if some other animations are chained to end, how do we cancel them as well?
        for (i in pendingMoves.indices.reversed()) {
            val moveInfo = pendingMoves[i]
            if (moveInfo.holder === item) {
                view.translationY = 0f
                view.translationX = 0f
                dispatchMoveFinished(item)
                pendingMoves.removeAt(i)
            }
        }
        endChangeAnimation(pendingChanges, item)
        if (pendingRemovals.remove(item)) {
            item.onPostAnimate(AnimationType.REMOVE)
            dispatchRemoveFinished(item)
        }
        if (pendingAdditions.remove(item)) {
            item.onPostAnimate(AnimationType.ADD)
            dispatchAddFinished(item)
        }
        for (i in scheduledChanges.indices.reversed()) {
            val changes = scheduledChanges[i]
            endChangeAnimation(changes, item)
            if (changes.isEmpty()) {
                scheduledChanges.removeAt(i)
            }
        }
        for (i in scheduledMoves.indices.reversed()) {
            val moves = scheduledMoves[i]
            for (j in moves.indices.reversed()) {
                val moveInfo = moves[j]
                if (moveInfo.holder === item) {
                    view.translationY = 0f
                    view.translationX = 0f
                    dispatchMoveFinished(item)
                    moves.removeAt(j)
                    if (moves.isEmpty()) {
                        scheduledMoves.removeAt(i)
                    }
                    break
                }
            }
        }
        for (i in scheduledAdditions.indices.reversed()) {
            val additions = scheduledAdditions[i]
            if (additions.remove(item)) {
                item.onPostAnimate(AnimationType.ADD)
                dispatchAddFinished(item)
                if (additions.isEmpty()) {
                    scheduledAdditions.removeAt(i)
                }
            }
        }

        // animations should be ended by the cancel above.
        if (runningRemovals.remove(item)) {
            throw IllegalStateException("after animation is cancelled, item should not be in runningRemovals list")
        }
        if (runningAdditions.remove(item)) {
            throw IllegalStateException("after animation is cancelled, item should not be in runningAdditions list")
        }
        if (runningChanges.remove(item)) {
            throw IllegalStateException("after animation is cancelled, item should not be in runningChanges list")
        }
        if (runningMoves.remove(item)) {
            throw IllegalStateException("after animation is cancelled, item should not be in runningMoves list")
        }
        maybeDispatchFinished()
    }

    /**
     * Check the state of currently pending and running animations. If there are none
     * pending/running, call [.dispatchAnimationsFinished] to notify any
     * listeners.
     */
    private fun maybeDispatchFinished() {
        if (!isRunning) dispatchAnimationsFinished()
    }

    override fun endAnimations() {
        pendingMoves.consumeReversed {
            val view = it.holder.itemView
            view.translationY = 0f
            view.translationX = 0f
            dispatchMoveFinished(it.holder)
        }

        pendingRemovals.consumeReversed {
            it.onPostAnimate(AnimationType.REMOVE)
            dispatchRemoveFinished(it)
        }

        pendingAdditions.consumeReversed {
            it.onPostAnimate(AnimationType.ADD)
            dispatchAddFinished(it)
        }

        pendingChanges.forEachReversed {
            tryEndChangeAnimation(it)
        }
        pendingChanges.clear()

        if (!isRunning) return

        scheduledMoves.consumeReversed {
            it.consumeReversed {
                val item = it.holder
                val view = item.itemView
                view.translationY = 0f
                view.translationX = 0f
                dispatchMoveFinished(it.holder)
            }
        }

        scheduledAdditions.consumeReversed {
            it.consumeReversed {
                it.onPostAnimate(AnimationType.ADD)
                dispatchAddFinished(it)
            }
        }

        scheduledChanges.forEachReversed { changes ->
            changes.forEachReversed {
                tryEndChangeAnimation(it)
                if (changes.isEmpty()) {
                    scheduledChanges.remove(changes)
                }
            }
        }

        runningRemovals.forEachReversed { it.itemView.animate().cancel() }
        runningMoves.forEachReversed { it.itemView.animate().cancel() }
        runningAdditions.forEachReversed { it.itemView.animate().cancel() }
        runningChanges.forEachReversed { it.itemView.animate().cancel() }
        dispatchAnimationsFinished()
    }

    private fun endChangeAnimation(infoList: MutableList<ChangeInfo>, item: Presenter.Holder) {
        for (i in infoList.indices.reversed()) {
            val changeInfo = infoList[i]
            if (tryEndChangeAnimation(changeInfo, item)) {
                if (changeInfo.oldHolder == null && changeInfo.newHolder == null) {
                    infoList.remove(changeInfo)
                }
            }
        }
    }

    private fun tryEndChangeAnimation(changeInfo: ChangeInfo) {
        if (changeInfo.oldHolder != null) {
            tryEndChangeAnimation(changeInfo, changeInfo.oldHolder!!)
        }
        if (changeInfo.newHolder != null) {
            tryEndChangeAnimation(changeInfo, changeInfo.newHolder!!)
        }
    }

    private fun tryEndChangeAnimation(changeInfo: ChangeInfo, item: Presenter.Holder): Boolean {
        var oldItem = false
        if (changeInfo.newHolder === item) {
            changeInfo.newHolder = null
        } else if (changeInfo.oldHolder === item) {
            changeInfo.oldHolder = null
            oldItem = true
        } else {
            return false
        }
        item.itemView.alpha = 1f
        item.itemView.translationX = 0f
        item.itemView.translationY = 0f
        dispatchChangeFinished(item, oldItem)
        return true
    }

    override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder, payloads: List<Any>): Boolean {
        return !payloads.isEmpty() || super.canReuseUpdatedViewHolder(viewHolder, payloads)
    }
}