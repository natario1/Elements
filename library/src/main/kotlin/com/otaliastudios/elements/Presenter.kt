package com.otaliastudios.elements

import android.arch.lifecycle.LifecycleOwner
import android.content.Context
import android.databinding.ViewDataBinding
import android.support.annotation.CallSuper
import android.support.annotation.UiThread
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    override fun getLifecycle() = owner.lifecycle

    /**
     * Returns the adapter that this presenter was bound to.
     * For this reason, you shouldn't bind a presenter to more than
     * one adapter.
     */
    protected fun getAdapter() = adapter

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
    public open fun onBind(page: Page, holder: Holder, element: Element<T>) {
        val click = holder.itemView.findViewById(R.id.click) ?: holder.itemView
        click.setOnClickListener {
            onElementClick?.invoke(page, holder, element)
        }
    }

    /**
     * A final class extending [RecyclerView.ViewHolder].
     * Holds an internal map of objects, that might be views or whatever else you need.
     * Just use [set] and [get] to retrieve them.
     */
    class Holder(view: View): RecyclerView.ViewHolder(view) {
        private val map: MutableMap<String, Any> = mutableMapOf()

        /**
         * Sets an object to be held by this Holder. Might be a view,
         * or any other object you need to attach.
         */
        public fun set(key: String, value: Any) { map[key] = value }

        /**
         * Returns an object that was previously attached to this
         * holder using [set].
         */
        @Suppress("UNCHECKED_CAST")
        public fun <T: Any> get(key: String): T = map[key] as T
    }

    companion object {

        /**
         * Creates a [SimplePresenter] with Kotlin-friendly syntax,
         * and restricted functionality. Extend the class for more freedom.
         */
        fun <T: Any> simple(context: Context, layoutRes: Int, elementType: Int, bind: ((View, T) -> Unit)? = null) = SimplePresenter(context, layoutRes, elementType, bind)

        /**
         * Creates a [ErrorPresenter] with Kotlin-friendly syntax,
         * and restricted functionality. Extend the class for more freedom.
         */
        fun forErrorIndicator(context: Context, layoutRes: Int, bind: ((View, Exception) -> Unit)? = null) = ErrorPresenter(context, layoutRes, bind)

        /**
         * Creates a [EmptyPresenter] with Kotlin-friendly syntax,
         * and restricted functionality. Extend the class for more freedom.
         */
        fun forEmptyIndicator(context: Context, layoutRes: Int) = EmptyPresenter(context, layoutRes)

        /**
         * Creates a [LoadingPresenter] with Kotlin-friendly syntax,
         * and restricted functionality. Extend the class for more freedom.
         */
        fun forLoadingIndicator(context: Context, layoutRes: Int, bind: ((View) -> Unit)? = null) = LoadingPresenter(context, layoutRes, bind)

        /**
         * Creates a [PaginationPresenter] with Kotlin-friendly syntax,
         * and restricted functionality. Extend the class for more freedom.
         */
        fun forPagination(context: Context, layoutRes: Int) = PaginationPresenter(context, layoutRes)

        /**
         * Creates a [DataBindingPresenter] with Kotlin-friendly syntax,
         * and restricted functionality. Extend the class for more freedom.
         */
        fun <T: Any, D: ViewDataBinding> withDataBinding(context: Context, elementType: Int, factory: (LayoutInflater, ViewGroup) -> D, bind: (D, T) -> Unit) = object : DataBindingPresenter<T, D>(context) {

            override val elementTypes = listOf(elementType)

            override fun onCreateBinding(parent: ViewGroup, elementType: Int): D {
                return factory(getLayoutInflater(), parent)
            }

            override fun onBind(page: Page, binding: D, element: Element<T>) {
                super.onBind(page, binding, element)
                bind(binding, element.data!!)
            }
        }
    }

}