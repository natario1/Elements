package com.otaliastudios.elements.sample

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.otaliastudios.elements.*
import com.otaliastudios.elements.extensions.*
import com.otaliastudios.elements.sample.presenters.AnimatedCheesePresenter
import com.otaliastudios.elements.sample.presenters.BottomPresenter
import com.otaliastudios.elements.sample.presenters.PlaygroundPresenter
import com.otaliastudios.elements.sample.presenters.TopMessagePresenter
import com.otaliastudios.elements.sample.sources.*
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        ElementsLogger.setLevel(ElementsLogger.VERBOSE)
        Timber.plant(Timber.DebugTree())
        val recycler = findViewById<RecyclerView>(R.id.recycler)
        val list = listOf(
                R.string.menu_paged, R.string.menu_paged_on_click,
                R.string.menu_shuffle, R.string.menu_headers,
                R.string.menu_animated, R.string.menu_empty,
                R.string.menu_complete, R.string.menu_playground)
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
            R.string.menu_animated -> AnimatedFragment::class
            R.string.menu_empty -> EmptyFragment::class
            R.string.menu_complete -> CompleteFragment::class
            R.string.menu_playground -> PlaygroundFragment::class
            else -> null
        }
        if (klass != null) {
            val fragment = supportFragmentManager.fragmentFactory.instantiate(classLoader, klass.java.name)
            supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commit()
        }
    }

    abstract class BaseFragment : androidx.fragment.app.Fragment() {

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.list, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            setUp(context!!, view as RecyclerView)
        }

        abstract fun setUp(context: Context, recyclerView: RecyclerView)
    }

    class PagedFragment : BaseFragment() {

        override fun setUp(context: Context, recyclerView: RecyclerView) {
            val message = "This list shows pages of 10 elements each. New pages are loaded when needed " +
                    "(that is, when you scroll down) depending on the pageSize value that is passed " +
                    "to the adapter constructor.\n\n" +
                    "Our Source simulates slow network fetching, so the loading indicator is displayed. " +
                    "To achieve this, extend MainSource to provide data, and add a LoadingPresenter." +
                    "A good estimation of the page size can lead to seamless loads without even showing the indicator.\n\n" +
                    "Note: This message is also part of the list, using a separate source with the insertBefore feature."
            Adapter.builder(this, 9)
                .addSource(CheeseSource(10))
                .addSource(TopMessageSource(message))
                .addPresenter(Presenter.simple<String>(context, R.layout.item_cheese, 0) { view, data -> (view as TextView).text = data})
                .addPresenter(TopMessagePresenter(context))
                .addPresenter(Presenter.forLoadingIndicator(context, R.layout.item_loading))
                .into(recyclerView)
        }
    }

    class ClickPagedFragment : BaseFragment() {

        override fun setUp(context: Context, recyclerView: RecyclerView) {
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
                .addSource(TopMessageSource(message))
                .addSource(PaginationSource { it is CheeseSource })
                .addPresenter(Presenter.simple<String>(context, R.layout.item_cheese, 0) { view, data -> (view as TextView).text = data})
                .addPresenter(TopMessagePresenter(context))
                .addPresenter(Presenter.forLoadingIndicator(context, R.layout.item_loading))
                .addPresenter(Presenter.forPagination(context, R.layout.item_pagination))
                .into(recyclerView)
        }
    }

    class ShuffleFragment : BaseFragment() {

        override fun setUp(context: Context, recyclerView: RecyclerView) {
            val message = "A single page list with some colors. Each 3 seconds, the list of colors is shuffled. " +
                    "It is extremely easy to send new results for the current page: they will replace the old results " +
                    "for this page coming from the same Source. The library will use DiffUtil " +
                    "in a background thread to compute the difference with the old list. This lets RecyclerView know exactly " +
                    "what changed and run nice animations.\n\n" +
                    "Note: This message is also part of the list, using a separate source with the insertBefore feature."
            Adapter.builder(this)
                .addSource(ShuffleColorsShource())
                .addSource(TopMessageSource(message))
                .addPresenter(Presenter.simple<Int>(context, R.layout.item_color, 0) { view, data -> view.setBackgroundColor(data)})
                .addPresenter(TopMessagePresenter(context))
                .into(recyclerView)
        }
    }

    class AnimatedFragment : BaseFragment() {

        override fun setUp(context: Context, recyclerView: RecyclerView) {
            val message = "A simple list. Each second, we add a new item. " +
                    "The animation is chosen randomly by the presenter by simply overriding the animate methods " +
                    "for each item.\n\n" +
                    "Note: This message is also part of the list, using a separate source with the insertBefore feature."
            Adapter.builder(this)
                    .addSource(AnimatedCheeseSource())
                    .addSource(TopMessageSource(message))
                    .addPresenter(AnimatedCheesePresenter(context))
                    .addPresenter(TopMessagePresenter(context))
                    .into(recyclerView)
        }
    }

    class EmptyFragment : BaseFragment() {

        override fun setUp(context: Context, recyclerView: RecyclerView) {
            val message = "This is what happens when a MainSource posts an empty list as a result. " +
                    "It will automatically pass an 'empty' element that you can present with a EmptyPresenter." +
                    "The same happens when such source posts an Exception. To handle this case, use an ErrorPresenter.\n\n" +
                    "Note: This message is also part of the list, using a separate source with the insertBefore feature."
            Adapter.builder(this)
                .addSource(EmptySource())
                .addSource(TopMessageSource(message))
                .addPresenter(Presenter.forEmptyIndicator(context, R.layout.item_empty))
                .addPresenter(TopMessagePresenter(context))
                .into(recyclerView)
        }
    }

    class HeadersFragment : BaseFragment() {

        override fun setUp(context: Context, recyclerView: RecyclerView) {
            val message = "An example of Source dependencies and ordering. " +
                    "We declare a CheeseSource for the main list and a separate CheeseHeaderSource for the alphabet headers. " +
                    "In the header class, we say that we depend on CheeseSource results, and compute the headers.\n\n" +
                    "Similarly, we use a CheeseFooterSource for the dividers at the end of each group.\n\n" +
                    "Note: This message is also part of the list, using a separate source with the insertBefore feature."
            Adapter.builder(this)
                .addSource(CheeseSource(pageSize = Int.MAX_VALUE, loadingEnabled = false))
                .addSource(TopMessageSource(message))
                .addSource(CheeseHeaderSource(2))
                .addSource(CheeseFooterSource(3))
                .addPresenter(Presenter.simple<String>(context, R.layout.item_cheese, 0) { view, data -> (view as TextView).text = data})
                .addPresenter(TopMessagePresenter(context))
                .addPresenter(Presenter.simple<HeaderSource.Data<String, String>>(context, R.layout.item_cheese_header, 2) { view, data -> (view as TextView).text = data.header})
                .addPresenter(Presenter.simple<FooterSource.Data<String, String>>(context, R.layout.item_cheese_footer, 3))
                .into(recyclerView)
        }
    }

    class CompleteFragment : BaseFragment() {

        override fun setUp(context: Context, recyclerView: RecyclerView) {
            val message = "All the previous features are expressed in Source and Presenter objects. This means " +
                    "that we can easily combine them by just adding them to the Adapter builder. " +
                    "Using addSource and addPresenter, we can compose all features in a single adapter, " +
                    "yet maintaining separate, testable classes for each functionality.\n\n" +
                    "Note: This message is also part of the list, using a separate source with the insertBefore feature."
            Adapter.builder(this)
                .addSource(CheeseSource(30))
                .addSource(TopMessageSource(message))
                .addSource(CheeseHeaderSource(2))
                .addSource(CheeseFooterSource(3))
                .addSource(PaginationSource { it is CheeseSource })
                .addPresenter(Presenter.simple<String>(context, R.layout.item_cheese, 0) { view, data -> (view as TextView).text = data})
                .addPresenter(TopMessagePresenter(context))
                .addPresenter(Presenter.simple<HeaderSource.Data<String, String>>(context, R.layout.item_cheese_header, 2) { view, data -> (view as TextView).text = data.header})
                .addPresenter(Presenter.simple<FooterSource.Data<String, String>>(context, R.layout.item_cheese_footer, 3))
                .addPresenter(Presenter.forLoadingIndicator(context, R.layout.item_loading))
                .addPresenter(Presenter.forPagination(context, R.layout.item_pagination))
                .into(recyclerView)
        }
    }


    class PlaygroundFragment : BaseFragment() {

        override fun setUp(context: Context, recyclerView: RecyclerView) {
            val message = "Just experimenting here to reproduce bugs. This is not a feature of the library at all."
            Adapter.builder(this)
                    .addSource(TopMessageSource(message))
                    .addPresenter(TopMessagePresenter(context))
                    .addSource(PlaygroundSource())
                    .addPresenter(PlaygroundPresenter(context))
                    .into(recyclerView)
        }
    }

}
