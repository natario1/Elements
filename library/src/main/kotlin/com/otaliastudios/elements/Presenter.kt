package com.otaliastudios.elements

import androidx.lifecycle.LifecycleOwner
import android.content.Context
import androidx.annotation.CallSuper
import androidx.annotation.UiThread
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Lifecycle
import com.otaliastudios.elements.extensions.*

/**
 * The base class for presenting [Element]s in the UI.
 * Provides hook to create holders and bind them to real data.
 *
 * Assigns a click listener to the root view of the element,
 * or, if present, to any other internal view with id [com.otaliastudios.elements.R.id.click].
 *
 * @param context an activity context
 * @param onElementClick a click callback
 */
public abstract class Presenter<T: Any>(
        protected val context: Context,
        protected var onElementClick: ((Page, Holder, Element<T>) -> Unit)? = null
) : LifecycleOwner {

    internal lateinit var owner: LifecycleOwner

    internal lateinit var adapter: Adapter

    /**
     * Each [Presenter] implements the [LifecycleOwner] interface,
     * although this was not needed until now.
     */
    override fun getLifecycle(): Lifecycle = owner.lifecycle

    /**
     * Returns the adapter that this presenter was bound to.
     * For this reason, you shouldn't bind a presenter to more than
     * one adapter.
     */
    protected fun getAdapter(): Adapter = adapter

    /**
     * Returns a legit layout inflater to be used when
     * creating holders.
     */
    protected fun getLayoutInflater(): LayoutInflater = LayoutInflater.from(context)

    /**
     * Assigns a click listener. The click listener is applied to the root view, or,
     * if present, to child views with the id [com.otaliastudios.elements.R.id.click].
     */
    public open fun setOnElementClickListener(listener: ((Page, Holder, Element<T>) -> Unit)?) {
        onElementClick = listener
    }

    internal fun createHolder(parent: ViewGroup, elementType: Int): Holder {
        val holder = onCreate(parent, elementType)
        onInitialize(holder, elementType)
        return holder
    }

    /**
     * Asks to return an Holder for the given elementType.
     * You can retrieve a layout inflater using [getLayoutInflater].
     */
    protected abstract fun onCreate(parent: ViewGroup, elementType: Int): Holder


    /**
     * Called when the holder is instantiated, has a root view and eventually child references.
     *
     * This is the point where you would perform basic view initialization, e.g. setting a color
     * filter to a drawable, before having actual values to bind. The good part is that this
     * is called just once per Holder.
     */
    protected open fun onInitialize(holder: Holder, elementType: Int) {}

    /**
     * Here this presenter can register to respond to certain element types, as returned by
     * [Source.getElementType].
     * If multiple presenters ask to layout a certain element type, the priority is given
     * based on the order of presenters passed to the adapter builder.
     */
    public abstract val elementTypes: Collection<Int>


    /**
     * Called when it's time to bind model values, represented by the [Element], to views
     * represented by the given [Holder], for the given [Page].
     */
    @CallSuper
    @UiThread
    public open fun onBind(page: Page, holder: Holder, element: Element<T>, payloads: List<Any>) {
        onBind(page, holder, element)
    }

    /**
     * Called when it's time to bind model values, represented by the [Element], to views
     * represented by the given [Holder], for the given [Page].
     */
    @CallSuper
    @UiThread
    public open fun onBind(page: Page, holder: Holder, element: Element<T>) {
        val click = holder.itemView.findViewById(R.id.click) ?: holder.itemView
        click.setOnClickListener {
            onElementClick?.invoke(page, holder, element)
        }
    }

    /**
     * Called to understand whether we should perform animations for the given animation type
     * and for the given holder. Presenters have fine grained control over what is animated and
     * how.
     */
    public open fun animates(animation: AnimationType, holder: Holder): Boolean {
        return true
    }

    /**
     * Animation will start at some point in the future. Subclasses can save info about the view
     * state and set initial values.
     *
     * Examples:
     * - Fade in animation: you should call view.setAlpha(0) here.
     * - Fade out animation: do nothing, we already start from full alpha.
     */
    public open fun onPreAnimate(animation: AnimationType, holder: Holder, view: View) {
        when (animation) {
            AnimationType.REMOVE -> {}
            AnimationType.ADD -> { view.alpha = 0F }
        }
    }

    /**
     * Restore the view state to its initial values, so the view holder can be reused.
     * Here you should revert any changes that were done during [onPreAnimate] or [onAnimate].
     *
     * If the animation is canceled at some point, this might be called even if
     * [onAnimate] was never called. So this is the good moment to restore the initial state
     * (as opposed to animation listeners).
     *
     * Examples:
     * - Fade in animation: restore the alpha using view.setAlpha(1F)
     * - Fade out animation: same
     */
    public open fun onPostAnimate(animation: AnimationType, holder: Holder, view: View) {
        when (animation) {
            AnimationType.REMOVE -> { view.alpha = 1F }
            AnimationType.ADD -> { view.alpha = 1F }
        }
    }

    /**
     * Animate this view using the given animator.
     * You are not required to:
     * - set a duration: we already use a reasonable default from RecyclerView
     * - set interpolator: we already use a reasonable default
     * - set a listener: it will be overriden by the library, so you should not.
     *
     * Examples:
     * - Fade in animation: animator.alpha(1F)
     * - Fade out animation: animator.alpha(0F)
     */
    public open fun onAnimate(animation: AnimationType, holder: Holder, animator: ViewPropertyAnimator) {
        when (animation) {
            AnimationType.REMOVE -> { animator.alpha(0F) }
            AnimationType.ADD -> { animator.alpha(1F) }
        }
    }

    /**
     * A final class extending [RecyclerView.ViewHolder].
     * Holds an internal map of objects, that might be views or whatever else you need.
     * Just use [set] and [get] to retrieve them.
     */
    public class Holder(view: View): RecyclerView.ViewHolder(view) {

        private val map: MutableMap<String, Any?> = mutableMapOf()

        /**
         * Sets an object to be held by this Holder. Might be a view,
         * or any other object you need to attach.
         */
        public operator fun set(key: String, value: Any?) { map[key] = value }

        /**
         * Returns an object that was previously attached to this
         * holder using [set].
         */
        @Suppress("UNCHECKED_CAST")
        public operator fun <T> get(key: String): T = map[key] as T

        /**
         * Shorthand for itemViewType.
         */
        public val elementType: Int get() = itemViewType

    }

    @Suppress("unused")
    public companion object {

        /**
         * Creates a [SimplePresenter] with Kotlin-friendly syntax,
         * and restricted functionality. Extend the class for more freedom.
         */
        public fun <T: Any> simple(
                context: Context,
                layoutRes: Int,
                elementType: Int,
                bind: ((View, T) -> Unit)? = null
        ): Presenter<T> = SimplePresenter(context, layoutRes, elementType, bind)

        /**
         * Creates a [ErrorPresenter] with Kotlin-friendly syntax,
         * and restricted functionality. Extend the class for more freedom.
         */
        public fun forErrorIndicator(
                context: Context,
                layoutRes: Int,
                bind: ((View, Exception) -> Unit)? = null
        ): Presenter<Unit> = ErrorPresenter(context, layoutRes, bind)

        /**
         * Creates a [EmptyPresenter] with Kotlin-friendly syntax,
         * and restricted functionality. Extend the class for more freedom.
         */
        public fun forEmptyIndicator(
                context: Context,
                layoutRes: Int
        ): Presenter<Unit> = EmptyPresenter(context, layoutRes)

        /**
         * Creates a [LoadingPresenter] with Kotlin-friendly syntax,
         * and restricted functionality. Extend the class for more freedom.
         */
        public fun forLoadingIndicator(
                context: Context,
                layoutRes: Int,
                bind: ((View) -> Unit)? = null
        ): Presenter<Unit> = LoadingPresenter(context, layoutRes, bind)

        /**
         * Creates a [PaginationPresenter] with Kotlin-friendly syntax,
         * and restricted functionality. Extend the class for more freedom.
         */
        public fun forPagination(
                context: Context,
                layoutRes: Int
        ): Presenter<Unit> = PaginationPresenter(context, layoutRes)

        /**
         * Creates a [DividerPresenter] to display dividers.
         */
        public fun forDividers(
                context: Context,
                layoutRes: Int
        ): Presenter<Unit> = DividerPresenter(context, layoutRes)

        /**
         * Creates a [DataBindingPresenter] with Kotlin-friendly syntax,
         * and restricted functionality. Extend the class for more freedom.
         */
        public fun <T: Any, D: ViewDataBinding> withDataBinding(
                context: Context,
                elementType: Int,
                factory: (LayoutInflater, ViewGroup) -> D,
                bind: (D, T) -> Unit
        ): Presenter<T> = object : DataBindingPresenter<T, D>(context) {

            override val elementTypes = listOf(elementType)

            override fun onCreateBinding(parent: ViewGroup, elementType: Int): D {
                return factory(getLayoutInflater(), parent)
            }

            override fun onBind(page: Page, holder: Holder, binding: D, element: Element<T>) {
                super.onBind(page, holder, binding, element)
                bind(binding, element.data!!)
            }
        }
    }

}