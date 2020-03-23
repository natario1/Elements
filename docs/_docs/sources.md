---
layout: page
title: "Sources"
description: "Business components responsible for providing model data."
order: 4
disqus: 1
---

In Elements, sources have the responsibility of providing model data. They can be extremely simple and extremely complex
if needed, through a [dependency mechanism](coordination).

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

### State restoration

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

### Overriding results

Source subclasses have the opportunity to **change the results that were posted**. See, for example, this code
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
In fact, this is already available in the [extensions](extensions) provided by the library.

### DiffUtils

Like the Google's Paging library, the adapter will use [`DiffUtil`](https://developer.android.com/reference/android/support/v7/util/DiffUtil)
to compute what changed and call the appropriate `notify*` methods on the adapter.

The computation is performed

- in a background thread
- at the page level. This means that it will deal with less elements and be fast
- anytime something in the page has changed

For this to work better, please provide an implementation of the equality callbacks:

```kotlin
class ContactsSource : Source<Contact> {

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