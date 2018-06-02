package com.otaliastudios.elements.extensions

import android.content.Context
import android.databinding.ViewDataBinding
import android.view.View
import android.view.ViewGroup
import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.Presenter

abstract class DataBindingPresenter<T: Any, DB: ViewDataBinding>(
        context: Context,
        onElementClick: ((Page, Holder, Element<T>) -> Unit)? = null
) : Presenter<T>(context, onElementClick) {


    final override fun onCreate(parent: ViewGroup, elementType: Int): Holder {
        val binding = onCreateBinding(parent, elementType)
        val holder = Holder(binding.root)
        holder.set("binding", binding)
        return holder
    }

    protected abstract fun onCreateBinding(parent: ViewGroup, elementType: Int): DB

    override fun onBind(page: Page, holder: Holder, element: Element<T>) {
        super.onBind(page, holder, element)
        val binding = holder.get<DB>("binding")
        onBind(page, binding, element)
        binding.executePendingBindings()
    }

    protected open fun onBind(page: Page, binding: DB, element: Element<T>) {

    }

}