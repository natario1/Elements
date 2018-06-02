package com.otaliastudios.elements.sample

import android.os.Bundle
import android.databinding.DataBindingUtil
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.otaliastudios.elements.*
import com.otaliastudios.elements.extensions.*
import com.otaliastudios.elements.sample.presenters.BottomPresenter
import com.otaliastudios.elements.sample.sources.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        val recycler = findViewById<RecyclerView>(R.id.recycler)
        val list = listOf(R.string.menu_paged, R.string.menu_paged_on_click, R.string.menu_shuffle,
                R.string.menu_headers, R.string.menu_empty, R.string.menu_complete)
        val presenter = BottomPresenter(this, ::onElementClick)
        Adapter.builder(this)
                .addPresenter(presenter)
                .addSource(Source.fromList(list))
                .into(recycler)
        navigateTo(R.string.menu_paged)
    }

    private fun onElementClick(page: Page, holder: Presenter.Holder, element: Element<Int>) {
        navigateTo(element.data!!)
    }

    private fun navigateTo(where: Int) {
        val klass = when (where) {
            R.string.menu_paged -> PagedFragment::class
            R.string.menu_paged_on_click -> ClickPagedFragment::class
            R.string.menu_shuffle -> ShuffleFragment::class
            R.string.menu_headers -> HeadersFragment::class
            R.string.menu_empty -> EmptyFragment::class
            R.string.menu_complete -> CompleteFragment::class
            else -> null
        }
        if (klass != null) {
            val fragment = Fragment.instantiate(this, klass.java.name)
            supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commit()
        }
    }

    abstract class BaseFragment : Fragment() {

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.list, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            setUp(view as RecyclerView)
        }

        abstract fun setUp(recyclerView: RecyclerView)
    }

    class PagedFragment : BaseFragment() {

        override fun setUp(recyclerView: RecyclerView) {
            val message = "This list shows pages of 10 elements each. New pages are loaded when needed " +
                    "(that is, when you scroll down) depending on the pageSize value that is passed " +
                    "to the adapter constructor.\n\n" +
                    "Our Source simulates slow network fetching, so the loading indicator is displayed. " +
                    "To achieve this, extend BaseSource to provide data, and add a LoadingPresenter." +
                    "A good estimation of the page size can lead to seamless loads without even showing the indicator.\n\n" +
                    "Note: This message is also part of the list, using a separate source with the insertBefore feature."
            Adapter.builder(this, 9)
                .addSource(CheeseSource(10))
                .addSource(TopMessageSource(message, 1))
                .addPresenter(Presenter.simple<String>(context!!, R.layout.item_cheese, 0, { view, data -> (view as TextView).text = data}))
                .addPresenter(Presenter.simple<String>(context!!, R.layout.item_top, 1, { view, data -> (view as TextView).text = data}))
                .addPresenter(Presenter.forLoadingIndicator(context!!, R.layout.item_loading))
                .into(recyclerView)
        }
    }

    class ClickPagedFragment : BaseFragment() {

        override fun setUp(recyclerView: RecyclerView) {
            val message = "The Adapter loads new pages based on the position of the requested item " +
                    "and the pageSize value that is passed to the constructor. By default it is Int.MAX_VALUE: this means " +
                    "that the page opening behavior is disabled, and it acts as a single page Adapter. " +
                    "However, sources can still load just a few items.\n\n" +
                    "In this example we use both features: Int.MAX_VALUE (no page opening), a CheeseSource that loads just " +
                    "10 items per page, and a button that orders itself at the end of each page, and " +
                    "programmatically calls adapter.openNextPage().\n\n" +
                    "Note: This message is also part of the list, using a separate source with the insertBefore feature."
            Adapter.builder(this)
                .addSource(CheeseSource(10))
                .addSource(TopMessageSource(message, 1))
                .addSource(PaginationSource({ it is CheeseSource }))
                .addPresenter(Presenter.simple<String>(context!!, R.layout.item_cheese, 0, { view, data -> (view as TextView).text = data}))
                .addPresenter(Presenter.simple<String>(context!!, R.layout.item_top, 1, { view, data -> (view as TextView).text = data}))
                .addPresenter(Presenter.forLoadingIndicator(context!!, R.layout.item_loading))
                .addPresenter(Presenter.forPagination(context!!, R.layout.item_pagination))
                .into(recyclerView)
        }
    }

    class ShuffleFragment : BaseFragment() {

        override fun setUp(recyclerView: RecyclerView) {
            val message = "A single page list with some colors. Each 3 seconds, the list of colors is shuffled. " +
                    "It is extremely easy to send new results for the current page: they will replace the old results " +
                    "for this page coming from the same Source. The library will use DiffUtil " +
                    "in a background thread to compute the difference with the old list. This lets RecyclerView know exactly " +
                    "what changed and run nice animations.\n\n" +
                    "Note: This message is also part of the list, using a separate source with the insertBefore feature."
            Adapter.builder(this)
                .addSource(ShuffleColorsShource())
                .addSource(TopMessageSource(message, 1))
                .addPresenter(Presenter.simple<Int>(context!!, R.layout.item_color, 0, { view, data -> view.setBackgroundColor(data)}))
                .addPresenter(Presenter.simple<String>(context!!, R.layout.item_top, 1, { view, data -> (view as TextView).text = data}))
                .into(recyclerView)
        }
    }

    class EmptyFragment : BaseFragment() {

        override fun setUp(recyclerView: RecyclerView) {
            val message = "This is what happens when a BaseSource posts an empty list as a result. " +
                    "It will automatically pass an 'empty' element that you can present with a EmptyPresenter." +
                    "The same happens when such source posts an Exception. To handle this case, use an ErrorPresenter.\n\n" +
                    "Note: This message is also part of the list, using a separate source with the insertBefore feature."
            Adapter.builder(this)
                .addSource(EmptySource())
                .addSource(TopMessageSource(message, 1))
                .addPresenter(Presenter.forErrorIndicator(context!!, R.layout.item_empty))
                .addPresenter(Presenter.simple<String>(context!!, R.layout.item_top, 1, { view, data -> (view as TextView).text = data}))
                .into(recyclerView)
        }
    }

    class HeadersFragment : BaseFragment() {

        override fun setUp(recyclerView: RecyclerView) {
            val message = "An example of Source dependencies and ordering. " +
                    "We declare a CheeseSource for the main list and a separate CheeseHeaderSource for the alphabet headers. " +
                    "In the header class, we say that we depend on CheeseSource results, and compute the headers.\n\n" +
                    "Note: This message is also part of the list, using a separate source with the insertBefore feature."
            Adapter.builder(this)
                .addSource(CheeseSource(pageSize = Int.MAX_VALUE, loadingEnabled = false))
                .addSource(TopMessageSource(message, 1))
                .addSource(CheeseHeaderSource(2))
                .addPresenter(Presenter.simple<String>(context!!, R.layout.item_cheese, 0, { view, data -> (view as TextView).text = data}))
                .addPresenter(Presenter.simple<String>(context!!, R.layout.item_top, 1, { view, data -> (view as TextView).text = data}))
                .addPresenter(Presenter.simple<HeaderSource.Data<String, String>>(context!!, R.layout.item_cheese_header, 2, { view, data -> (view as TextView).text = data.header}))
                .into(recyclerView)
        }
    }

    class CompleteFragment : BaseFragment() {

        override fun setUp(recyclerView: RecyclerView) {
            val message = "All the previous features are expressed in Source and Presenter objects. This means " +
                    "that we can easily combine them by just adding them to the Adapter builder. " +
                    "Using addSource and addPresenter, we can compose all features in a single adapter, " +
                    "yet maintaining separate, testable classes for each functionality.\n\n" +
                    "Note: This message is also part of the list, using a separate source with the insertBefore feature."
            Adapter.builder(this)
                .addSource(CheeseSource(30))
                .addSource(TopMessageSource(message, 1))
                .addSource(CheeseHeaderSource(2))
                .addSource(PaginationSource({ it is CheeseSource }))
                .addPresenter(Presenter.simple<String>(context!!, R.layout.item_cheese, 0, { view, data -> (view as TextView).text = data}))
                .addPresenter(Presenter.simple<String>(context!!, R.layout.item_top, 1, { view, data -> (view as TextView).text = data}))
                .addPresenter(Presenter.simple<HeaderSource.Data<String, String>>(context!!, R.layout.item_cheese_header, 2, { view, data -> (view as TextView).text = data.header}))
                .addPresenter(Presenter.forLoadingIndicator(context!!, R.layout.item_loading))
                .addPresenter(Presenter.forPagination(context!!, R.layout.item_pagination))
                .into(recyclerView)
        }
    }

}
