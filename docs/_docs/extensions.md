---
layout: page
title: "Extensions"
description: "Handy implementations of components"
order: 8
disqus: 1
---

The `elements.extensions` package provides useful implementations for sources and presenters.
Typically we provide also static methods in the `Presenter` and `Source` classes to have even simpler
implementations to avoid subclasses.

MainSource related:

- [`MainSource`](#mainsource)
- [`EmptyPresenter` and `Presenter.forEmptyIndicator()`](#emptypresenter)
- [`ErrorPresenter` and `Presenter.forErrorIndicator()`](#errorpresenter)
- [`LoadingPresenter` and `Presenter.forLoadingIndicator()`](#loadingpresenter)

Pagination prompts:

- [`PaginationSource` and `Source.forPagination()`](#paginationsource-and-paginationpresenter)
- [`PaginationPresenter` and `Presenter.forPagination()`](#paginationsource-and-paginationpresenter)

Dividers:

- [`DividerSource` and `Source.forDividers()`](#dividersource-and-dividerpresenter)
- [`DividerPresenter` and `Presenter.forDividers()`](#dividersource-and-dividerpresenter)


Simple sources:

- [`ListSource` and `Source.fromList()`](#listsource)
- [`LiveDataSource` and `Source.fromLiveData()`](#livedatasource)

Simple presenters:

- [`SimplePresenter` and `Presenter.simple()`](#simplepresenter)
- [`DataBindingPresenter` and `Presenter.withDataBinding()`](#databindingpresenter)

Ordering utilities:

- [`FooterSource` and `HeaderSource`](#footersource-and-headersource)

##### MainSource

This is meant to be extended and incorporates standard behavior for paged, asynchronous content.

- when the results are empty, it emits a `ELEMENT_TYPE_EMPTY` element that can be presented by [`EmptyPresenter`](#emptypresenter)
- when you post an error, it emits a `ELEMENT_TYPE_ERROR` element that can be presented by [`ErrorPresenter`](#errorpresenter)
- as soon as a page is opened, it emits a `ELEMENT_TYPE_LOADING` element that can be presented by [`LoadingPresenter`](#loadingpresenter). When real elements come, the loading element is replaced.

You typically don't want to have more than one `MainSource` in the same adapter.

##### EmptyPresenter

Reads `ELEMENT_TYPE_EMPTY` elements from [`MainSource`](#mainsource). You can display a "This list is empty." indicator.
Subclass for more functionality.

```kotlin
Adapter.builder(this)
    .addPresenter(Presenter.forEmptyIndicator(this, R.layout.empty))
    .into(recycler)
```

##### ErrorPresenter

Reads `ELEMENT_TYPE_ERROR` elements from [`MainSource`](#mainsource). You can display a "There was an error." indicator.
Subclass for more functionality.

```kotlin
Adapter.builder(this)
    .addPresenter(Presenter.forErrorIndicator(this, R.layout.error, { view, exception ->
        (view as TextView).text = "There was an error: $exception"
    }))
    .into(recycler)
```

##### LoadingPresenter

Reads `ELEMENT_TYPE_LOADING` elements from [`MainSource`](#mainsource). You can display a loading indicator.
Subclass for more functionality.

```kotlin
Adapter.builder(this)
    .addPresenter(Presenter.forLoadingIndicator(this, R.layout.loading))
    .into(recycler)
```

##### PaginationSource and PaginationPresenter

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

##### DividerSource and DividerPresenter

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

##### ListSource

A list source will simply display items from a list in a single page.

```kotlin
Adapter.builder(this)
    .addSource(Source.fromList(listOf("Red", "Green", "Blue")))
    .into(recycler)
```

##### LiveDataSource

A LiveData source will simply bind results from a LiveData object into a single adapter page.

```kotlin
Adapter.builder(this)
    .addSource(Source.fromLiveData(ContactsLiveData()))
    .into(recycler)
```

##### SimplePresenter

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

##### DataBindingPresenter

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

##### FooterSource and HeaderSource

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
