# Elements

A collection of modular elements for `RecyclerView` lists, alternative to
[Google's Paging library](https://developer.android.com/topic/libraries/architecture/paging/), designed in Kotlin with these goals in mind:

- **Separation of concerns**: we split the model component into `Source`s, and the UI component into `Presenter`s.
- **Simplicity**: No need to extend Adapters, ViewHolders or all that Paging lib. boilerplate.
- **Reusability**: as a result, each `Source` and `Presenter` is an independent piece of code that can be reused.
- **Modularity**: let the adapter accept multiple `Source`s and `Presenter`s.
- **Testability**: a consequence of the above, each component can be independently tested.
- **Coordination**: let `Source`s declare dependencies among them, in a `CoordinatorLayout.Behavior` fashion.
- **Paging**: built-in concept of `Page`.
- **Integration with Arch components**: heavy use of `LiveData` and `Lifecycle`s, extensions for data binding.
- **Animations**: give `Presenters`s fine grained control over how to animate each item

```groovy
implementation 'com.otaliastudios:elements:0.3.3'
```

If you are curious about how it works in practice, take a look at the sample app in the `app` module.

# Basics

Let's start with some basic examples:

```kotlin
// Simple, single-paged list of days.
val data = listOf("Monday", "Tuesday", "Wednesday", "Friday", "Saturday", "Sunday")
Adapter.builder(lifecycle)
    .addSource(Source.fromList(data))
    .addPresenter(Presenter.simple(context, R.layout.item, { view, day -> view.text = day }))
    .into(recyclerView)
```

```kotlin
// Using an existing LiveData object for model data.
val data: LiveData<String> = WeekDaysLiveData()
Adapter.builder(lifecycle)
    .addSource(Source.fromLiveData(data))
    .addPresenter(Presenter.simple(context, R.layout.item, { view, day -> view.text = day }))
    .into(recyclerView)
```

```kotlin  
// More complex example:
// these contacts classes do not exist, but are easy to create.
Adapter.builder(lifecycle)
    .addSource(ContactsSource()) // Add a paged list of contacts
    .addSource(ContactsHeaderSource()) // Add the letters A, B, C as a header
    .addSource(AdSource(5)) // Add some ads each 5 items
    .addPresenter(ContactsPresenter(context))
    .addPresenter(ContactsHeaderPresenter(context))
    .addPresenter(AdPresenter(context))
    .addPresenter(Presenter.forLoadingIndicator(context, R.layout.loading))
    .addPresenter(Presenter.forErrorIndicator(context, R.layout.error))
    .addPresenter(Presenter.forEmptyIndicator(context, R.layout.empty))
    .into(recyclerView)
```

## The Adapter

The adapter is final and can't be extended. All it does is:

- manage [`Source`s](#sources): create a dependency tree among them. Throws exception in case of circular dependencies
- manage [`Presenter`s](#presenters): dispatch the item requests to the correct presenter
- manage [`Page`s](#paging): if the constructor value `pageSizeHint` is specified, the adapter will automatically open a new page when the `pageSizeHint`-th element has been requested for the current page.
- manage `Element`s: [find a presenter](#elements:-binding-presenters-and-sources), and [order them based on the source behavior](#coordination-and-ordering).
- manage [page updates](#diffutils): computes the difference between pages using `DiffUtil`, like the Paging library does.

As a side feature, the adapter will install an `Animator` as default item animator for your `RecyclerView`.
See [Animations](#animations) for more informations.

## Paging

The key concept is that each `Source` is independent in the number of elements that it wants to lay down
for a given page.

Whether a new page will be opened, this depends on the `Pager` that is bound to the `Adapter`.
A pager receives updates about elements that are about to be laid out, and can determine whether it's time
to load a new page or not.

We offer a base implementation called `PageSizePager` which will request new pages when the X-th 
element is loaded. Let's show this by examples  - let's say we have a `Source` called `ContactsSource` 
that can display X items per page. 

```kotlin
// Single page with all elements.
Adapter.builder(lifecycle)
    .addSource(ContactsSource(itemsPerPage = Int.MAX_VALUE))
    .into(recyclerView)
```

```kotlin    
// Good: Split into pages of 10 contacts. When the 10-th element is requested,
// the adapter will request the source for a new page of elements.
Adapter.builder(lifecycle)
    .setPager(PageSizePager(10))
    .addSource(ContactsSource(itemsPerPage = 10))
    .into(recyclerView)
```

Note: as a shorthand, instead of `setPager(PageSizePager(10))`, you can simply pass `10` to the builder constructor.

```kotlin    
// Better: Split into pages of 10 contacts, but request the next page when the 7-th
// is shown. This means that our network request will be fired earlier and the user will
// wait less time when scrolling.
Adapter.builder(lifecycle)
    .setPager(PageSizePager(7))
    .addSource(ContactsSource(itemsPerPage = 10))
    .into(recyclerView)
```

```kotlin        
// The source loads 10 item, and the adapter won't ask for more.
// You can, however, call adapter.openPage() when needed, for example
// when a 'load more...' button is clicked.
Adapter.builder(lifecycle)
    .addSource(ContactsSource(itemsPerPage = 10))
    .into(recyclerView)
```

```kotlin
// Error: the source loads everything, but when the 10-th element is requested,
// the adapter tries to open another page. No big deal anyway.
Adapter.builder(lifecycle)
    .setPager(PageSizePager(10))
    .addSource(ContactsSource(itemsPerPage = Int.MAX_VALUE))
    .into(recyclerView)
```

The library provides utilities to show loading indicators, pagination prompt buttons, attach a `LiveData` 
object to each page, and more. See the [Extensions](#extensions) section.

## Presenters

`Presenter`s have the responsibility of laying down elements, that is, creating `View`s and binding data to them.
This is the simplest component and most of the time you don't even need to subclass the `Presenter` class, thanks
to [extensions](#extensions).

These are the main tasks:

|Task|Function|Description|
|----|--------|-----------|
|Holder creation|`onCreate(ViewGroup, Int)`|Here you must provide a `Holder` instance, typically inflating a layout resource.|
|Holder initialization|`onInitialize(Holder, Int)`|The holder was created. You can perform here initialization task that do not depend on data (like color filters to icon), or add Views and object to the Holder cache using `Holder.set(key, data)` and `Holder.get(key)`.|
|Binding|`onBind(Page, Holder, Element<T>)`|Bind data, contained in the given `Element`, to the view held by `Holder`.|

Presenters also **accept a click listener** that will be automatically added to each view.
The click listener will be added to the root view of the Holder, or, if found, to a child view that
has the id `R.id.click`. This way you can still use the provided listener for internal clicks.

## Sources

`Source`s have the responsibility of providing model data. They can be extremely simple and extremely complex
if needed, through a [dependency mechanism](#coordination-and-ordering).

For now, let's look at the main callbacks and APIs:

|Callback / API|Description|
|--------------|-----------|
|`onPageOpened(page: Page, dependencies: List<Element<*>>)`|The given page was opened. You are required to provide results using one of the `postResult` methods. If the source declared some dependencies, this method will be called once the dependencies are resolved, and will be passed a list of the elements provided by the dependencies.|
|`onPageChanged(page: Page, dependencies: List<Element<*>>)`|The given page was changed. This is only called if the source declares some dependencies. At this point it can process the list of dependencies elements and, if needed, update its results, as above, using one of the `postResult` methods.|
|`postResult(page: Page, data: Collection<T>)`|Posts results for the given page. Can be called at any time, from any thread.|
|`postResult(page: Page, error: Exception)`|Posts an error for the given page. This means we have no results for this page.|
|`postResult(page: Page, liveData: LiveData<List<T>>)`|Binds this page to the given `LiveData` object. This means that anytime this `LiveData` provides results, we will post them to the given page.|

When posting results, the list of `T` data is converted to a list of `Element<T>` object.
The source can declare the element type for each `T` data in the `getElementType(T)` callback.

Subclasses also have the opportunity to **change the results that were posted**. See, for example, this code
that replaces empty lists with a *this list is empty* Element:

```kotlin
override fun onPostResult(page: Page, result: Result<T>): List<Element<T>> {
    if (result.error != null) {
        return listOf(createEmptyElement(ELEMENT_TYPE_ERROR, result.error))
    } else if (result.values.isEmpty()) {
        return listOf(createEmptyElement(ELEMENT_TYPE_EMPTY))
    } else {
        return result.values
    }
}
```    

At this point, you will need a presenter for the `ELEMENT_TYPE_ERROR` and `ELEMENT_TYPE_EMPTY` types.
In fact, this is all available in our [extensions](#extensions).

### Effective pagination: page keys

For meaningful pagination, you will need a mechanism to store a key object about pages, which helps formulating
the query for the next page. `Source`s can do this by assigning a private key of type `Any` to each page.

For example, after fetching data for page `page`, you can use `setKey(page, lastItem)`.
When a new page is requested, you can use `getKey(page.previous())` to know which was the last item
and pass meaningful constraints to your server.

## Elements: Binding presenters and sources

The base block of the library is the `Element` class. Each element holds our model data, 
a reference to the source that created it, and an `Int` value called the element type.

```kotlin
class Element<T: Any>(source: Source<T>, type: Int, data: T?, extra: Any?)
```

This **element type** is the mechanism through which we link sources and presenters:

- Each source emits elements with one or more types, for example through the `getElementType(T): Int` callback
- Each presenter declares the types it can lay out, in the `elementTypes` abstract collection

In case more presenters declare to be able to lay out a given type, the order in which they are added
to the adapter counts.

## Coordination and ordering

Sources can declare internal dependencies, like `CoordinatorLayout.Behavior`s do. This is done overriding
the `dependsOn()` function:

```kotlin
override fun dependsOn(other: Source<*>): Boolean {
    return other is ContactsSource
}
```

This has a few consequences:

- the source receives all the dependencies objects in the `onPageOpened`. This also means that the callback is only fired when the dependency has provided items for that page.
- the source receives the `onPageChanged` callback, when the dependency has changed the page content.
- the source receives the `insertBefore()` and `insertAfter()` callbacks, for relative ordering

When a source declares dependencies, we give this source the opportunity to choose how and where to insert its elements.
If source `A` depends on source `B` and `C`, you can imagine, for each actual page, an imaginary page made of items from `B` and `C` for that page.
 
 ```kotlin
public open fun insertBefore(page: Page, dependencies: List<Element<*>>, element: Element<*>, position: Int, available: Int): Int {
    return available // Insert all our A items before B and C items
    return if (element.data == "foo") 1 else 0 // Add a header to "foo" objects
    return if (position % 4 == 0) 1 else 0 // Add an item each 4 items
}
 ```
 
Same goes for `insertAfter()`. Once the source has inserted all its `available` items, the methods are not called anymore.
 
 
## State restoration

Sources hold strong references to pages and model data. This has a pleasant consequence:

> If you hold sources in your architecture `ViewModel`, and pass them to a new adapter, the adapter
> will restore pages and data seamlessly during configuration changes.

The approach should be the following:

```kotlin
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    Adapter.builder(this)
        .addSource(viewModel.contactsSource)
        .addPresenter(presenter)
        .into(recyclerView)
}
```

## DiffUtils

Like the Google's Paging library, the adapter will use [`DiffUtil`](https://developer.android.com/reference/android/support/v7/util/DiffUtil)
to compute what changed and call the appropriate `notify*` methods on the adapter.

The computation is performed

- in a background thread
- at the page level. This means that it will deal with less elements and be fast
- anytime something in the page has changed

For this to work better, provide an implementation of the equality callbacks:

```kotlin
class ContactsSource() : Source<Contact> {

    // Whether two items point to the same model entity (that might be changed). 
    override fun areItemsTheSame(first: Contact, second: Contact): Boolean {
        return first.id() == second.id()
    }

    // Used to detect whether the model entity has changed. Will call notifyItemChanged().
    // Defaults to full equality.
    override fun areContentsTheSame(first: Contact, second: Contact): Boolean {
        return first == second
    }
}
```

# Animations

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

# Extensions

The `elements.extensions` package provides useful implementations for sources and presenters.
Typically we provide also static methods in the `Presenter` and `Source` classes to have even simpler
implementations to avoid subclasses.

MainSource related:

- [`MainSource`](#mainsource)
- [`EmptyPresenter` / `Presenter.forEmptyIndicator()`](#emptypresenter)
- [`ErrorPresenter` / `Presenter.forErrorIndicator()`](#errorpresenter)
- [`LoadingPresenter` / `Presenter.forLoadingIndicator()`](#loadingpresenter)

Pagination prompts:

- [`PaginationSource` / `Source.forPagination()`](#paginationsource-and-paginationpresenter)
- [`PaginationPresenter` / `Presenter.forPagination()`](#paginationsource-and-paginationpresenter)

Dividers:

- [`DividerSource` / `Source.forDividers()`](#dividersource-and-dividerpresenter)
- [`DividerPresenter` / `Presenter.forDividers()`](#dividersource-and-dividerpresenter)


Simple sources:

- [`ListSource` / `Source.fromList()`](#listsource)
- [`LiveDataSource` / `Source.fromLiveData()`](#livedatasource)

Simple presenters:

- [`SimplePresenter` / `Presenter.simple()`](#simplepresenter)
- [`DataBindingPresenter` / `Presenter.withDataBinding()`](#databindingpresenter)

Ordering utilities:

- [`FooterSource` and `HeaderSource`](#footersource-and-headersource)

#### MainSource

This is meant to be extended and incorporates standard behavior for paged, asynchronous content.

- when the results are empty, it emits a `ELEMENT_TYPE_EMPTY` element that can be presented by [`EmptyPresenter`](#emptypresenter)
- when you post an error, it emits a `ELEMENT_TYPE_ERROR` element that can be presented by [`ErrorPresenter`](#errorpresenter)
- as soon as a page is opened, it emits a `ELEMENT_TYPE_LOADING` element that can be presented by [`LoadingPresenter`](#loadingpresenter). When real elements come, the loading element is replaced.

You typically don't want to have more than one `MainSource` in the same adapter.

#### EmptyPresenter

Reads `ELEMENT_TYPE_EMPTY` elements from [`MainSource`](#mainsource). You can display a "This list is empty." indicator.
Subclass for more functionality.

```kotlin
Adapter.builder(this)
    .addPresenter(Presenter.forEmptyIndicator(this, R.layout.empty))
    .into(recycler)
```

#### ErrorPresenter

Reads `ELEMENT_TYPE_ERROR` elements from [`MainSource`](#mainsource). You can display a "There was an error." indicator.
Subclass for more functionality.

```kotlin
Adapter.builder(this)
    .addPresenter(Presenter.forErrorIndicator(this, R.layout.error, { view, exception -> 
        (view as TextView).text = "There was an error: $exception"
    }))
    .into(recycler)
```

#### LoadingPresenter

Reads `ELEMENT_TYPE_LOADING` elements from [`MainSource`](#mainsource). You can display a loading indicator.
Subclass for more functionality.

```kotlin
Adapter.builder(this)
    .addPresenter(Presenter.forLoadingIndicator(this, R.layout.loading))
    .into(recycler)
```

#### PaginationSource and PaginationPresenter

A pagination source emits `PaginationSource.ELEMENT_TYPE` elements that are meant to be displayed as "Load more..." buttons.
These elements are appended at the end of a page. The `PaginationPresenter` will receive clicks on these items
and ask the adapter for a new page.

```kotlin
val source = ContactsSource()
Adapter.builder(this)
    .addSource(source)
    .addSource(Source.forPagination(source)) // Add below contacts
    .addPresenter(Presenter.forPagination(this, R.layout.load_more))
    .into(recycler)
```

#### DividerSource and DividerPresenter

When you have multiple sources you might want to add dividers among the items of a Source, but not all of the others.
For example, you might now want dividers between ads.
In this case, a divider source emits `DividerSource.ELEMENT_TYPE` elements that are caught and displayed
by the divider presenter.

```kotlin
val source = ContactsSource()
Adapter.builder(this)
    .addSource(source)
    .addSource(Source.forDividers(source))
    .addPresenter(Presenter.forDividers(this, R.layout.divider))
    .into(recycler)
```

#### ListSource

A list source will simply display items from a list in a single page.

```kotlin
Adapter.builder(this)
    .addSource(Source.fromList(listOf("Red", "Green", "Blue")))
    .into(recycler)
```

#### LiveDataSource

A LiveData source will simply bind results from a LiveData object into a single adapter page.

```kotlin
Adapter.builder(this)
    .addSource(Source.fromLiveData(ContactsLiveData()))
    .into(recycler)
```
#### SimplePresenter

An extremely simple presenter that can be declared in a single line. It just requires a layout and,
optionally, binding logic.

```kotlin
Adapter.builder(this)
    .addSource(ContactsSource())
    .addPresenter(Presenter.simple(this, R.layout.contact, 0, { view, contact -> 
        (view as TextView).text = contact.fullName()
    }))
    .into(recycler)
```

#### DataBindingPresenter

An useful presenter for users of Android official Data Binding mechanism. You must provide a data binding
factory and binding logic, or extend the class for more functionality.
This will call `executePendingBindings()` for you after binding.

```kotlin
Adapter.builder(this)
    .addSource(ContactsSource())
    .addPresenter(Presenter.withDataBinding(this, 0, { inflater, viewGroup -> 
        ContactBinding.inflate(inflater, viewGroup, false)
    }, { binding, contact ->
        binding.contact = contact
    }))
    .into(recycler)
```

#### FooterSource and HeaderSource

Ordering utilities for appending or prepending items to other elements of the page.
Internally, these use dependencies and `insert()` callbacks. Implementors should subclass and
provide an implementation for `computeHeaders` or `computeFooters`.

The class below will add header letters (A, B, C...) above contacts,
without duplicates.

```kotlin
class ContactsHeaderSource(): HeaderSource<Contact, String>() {

    // Store the last header that was added, even if it belongs to a previous page.
    private var lastHeader: String = ""

    override fun dependsOn(source: Source<*>) = source is ContactsSource

    override fun computeHeaders(page: Page, list: List<Contact>): List<Data<Contact, String>> {
        val results = arrayListOf<Data<Contact, String>>()
        for (contact in list) {
            val header = contact.fullName().substring(0, 1).toUpperCase()
            if (header != lastHeader) {
                results.add(Data(contact, letter))
                lastHeader = letter
            }
        }
        return results
    }
}
```