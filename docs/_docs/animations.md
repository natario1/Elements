---
layout: page
title: "Animations"
description: "Built-in support for item animations right where it should be, in the Presenter component."
order: 7
disqus: 1
---

Elements has built-in support for animations. On top of calling the correct `notify*` methods,
we also install an `Animator` as the default item animator for your `RecyclerView`.

This way each `Presenter` can have **fine-grained control** over elements animations, based on their
type or their current data. The animations default to the standard fade animations and can be
controlled by overriding a few methods.

```kotlin
/**
 * Called to understand whether we should perform animations for the given animation type
 * and for the given holder. 
 */
fun animates(animation: AnimationType, holder: Holder): Boolean {
    return true
}

/**
 * Animation will start at some point in the future. Subclasses can save info about the view
 * state and set initial values.
 */
fun onPreAnimate(animation: AnimationType, holder: Holder, view: View) {
    when (animation) {
        AnimationType.REMOVE -> {} // Fade out: do nothing
        AnimationType.ADD -> view.alpha = 0F // Fade in: start from 0
    }
}

/**
 * Animate this view using the given animator.
 * You are not required to:
 * - set a duration: we already use a reasonable default from RecyclerView
 * - set interpolator: we already use a reasonable default
 * - set a listener: it will be overriden by the library, so you should not.
 */
fun onAnimate(animation: AnimationType, holder: Holder, animator: ViewPropertyAnimator) {
    when (animation) {
        AnimationType.REMOVE -> animator.alpha(0F) // Fade out
        AnimationType.ADD -> animator.alpha(1F) // Fade in
    }
}

/**
 * Restore the view state to its initial values, so the view holder can be reused.
 * Here you should revert any changes that were done during [onPreAnimate] or [onAnimate].
 *
 * If the animation is canceled at some point, this will be called even if
 * [onAnimate] was never called. So this is the good moment to restore the initial state
 * (as opposed to animation listeners).
 */
fun onPostAnimate(animation: AnimationType, holder: Holder, view: View) {
    when (animation) {
        AnimationType.REMOVE -> view.alpha = 1F // Fade out: restore to 1
        AnimationType.ADD -> view.alpha = 1F // Fade in: restore to 1
    }
}
```
