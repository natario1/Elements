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

public abstract class Presenter<T: Any>(
        protected val context: Context,
        protected var onElementClick: ((Page, Holder, Element<T>) -> Unit)? = null
) : LifecycleOwner {

    internal lateinit var owner: LifecycleOwner

    internal lateinit var adapter: Adapter

    override fun getLifecycle() = owner.lifecycle

    protected fun getAdapter() = adapter

    protected fun getLayoutInflater(): LayoutInflater = LayoutInflater.from(context)

    public open fun setOnElementClickListener(listener: ((Page, Holder, Element<T>) -> Unit)?) {
        onElementClick = listener
    }

    internal fun createHolder(parent: ViewGroup, elementType: Int): Holder {
        val holder = onCreate(parent, elementType)
        onInitialize(holder)
        return holder
    }

    /**
     * Asks to return an Holder for the given elementType.
     * You can retrieve a layout inflater using [getLayoutInflater].
     */
    protected abstract fun onCreate(parent: ViewGroup, elementType: Int): Holder


    /**
     * Called when the holder is instantiated, has a root view and eventually child references.
     * The element type can be retrieved with [Holder.getElementType].
     *
     * This is the point where you would perform basic view initialization, e.g. setting a color
     * filter to a drawable, *before* having actual values to bind. The advantage is that this
     * is called just once per Holder.
     */
    protected open fun onInitialize(holder: Holder) {}

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

    class Holder(view: View): RecyclerView.ViewHolder(view) {
        private val map: MutableMap<String, Any> = mutableMapOf()

        public fun set(key: String, value: Any) { map[key] = value }

        @Suppress("UNCHECKED_CAST")
        public fun <T: Any> get(key: String): T = map[key] as T
    }

    companion object {

        fun <T: Any> simple(context: Context, layoutRes: Int, elementType: Int, bind: ((View, T) -> Unit)) = SimplePresenter(context, layoutRes, elementType, bind)

        fun forErrorIndicator(context: Context, layoutRes: Int, bind: ((View, Exception) -> Unit)? = null) = ErrorPresenter(context, layoutRes, bind)

        fun forEmptyIndicator(context: Context, layoutRes: Int) = EmptyPresenter(context, layoutRes)

        fun forLoadingIndicator(context: Context, layoutRes: Int, bind: ((View) -> Unit)? = null) = LoadingPresenter(context, layoutRes, bind)

        fun forPagination(context: Context, layoutRes: Int) = PaginationPresenter(context, layoutRes)

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