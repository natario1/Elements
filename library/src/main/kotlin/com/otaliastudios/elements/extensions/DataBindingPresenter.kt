package com.otaliastudios.elements.extensions

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.Presenter

/**
 * A handy class to deal with [ViewDataBinding] from the official data binding
 * library, instead of views. This is useful because the binding object will
 * incapsulate views and automatically hold references to them.
 *
 * @property context an activity context
 * @property onElementClick a click listener
 */
public abstract class DataBindingPresenter<T: Any, DB: ViewDataBinding>(
        context: Context,
        onElementClick: ((Page, Holder, Element<T>) -> Unit)? = null
) : Presenter<T>(context, onElementClick) {

    final override fun onCreate(parent: ViewGroup, elementType: Int): Holder {
        val binding = onCreateBinding(parent, elementType)
        binding.lifecycleOwner = this
        val holder = Holder(binding.root)
        holder["binding"] = binding
        lifecycle.addObserver(object: LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                binding.setLifecycleOwner(null)
                lifecycle.removeObserver(this)
            }
        })
        return holder
    }

    /**
     * Called when it's time to create the data binding object.
     * You can retrieve an inflater using [getLayoutInflater].
     */
    protected abstract fun onCreateBinding(parent: ViewGroup, elementType: Int): DB

    override fun onBind(page: Page, holder: Holder, element: Element<T>) {
        super.onBind(page, holder, element)
        val binding = holder.get<DB>("binding")
        onBind(page, holder, binding, element)
        binding.executePendingBindings()
    }

    /**
     * Called when it's time to bind the data to the binding object.
     */
    protected open fun onBind(page: Page, holder: Holder, binding: DB, element: Element<T>) {

    }

}