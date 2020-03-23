---
layout: page
title: "Pagination"
description: "How to paginate results and the concept of Pager."
order: 5
disqus: 1
---

In Elements, pages divide (if configured to do so) the dataset in different
sets of data that can be displayed separately and most importantly, loaded
asynchronously.

Our `Source`s will be asked to load results for a given page, and each of
the registered sources is independent in the number of elements that it wants
to provide.

Different sources can provide a different number (or none) of items for
different pages, and still be coordinated through our dependency mechanism.

The library also provides utilities to show loading indicators, pagination prompt buttons, attach a `LiveData`
object to each page, and more: please take a look at the [extensions](extensions) section.

### The Pager

Whether a new page will be opened, this depends on the `Pager` that is bound to the `Adapter`.
A pager receives updates about elements that are about to be laid out, and can determine whether it's time
to load a new page or not.

The library offers a few pager implementations that can be passed to the adapter when configuring it.

##### PageSizePager

The `PageSizePager` will request new pages when the X-th
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

##### NoPagesPager

The `NoPagesPager` will never request a new page, which effectively means that the dataset will be loaded and shown
inside a single page (that is, pagination is disabled).

```kotlin
Adapter.builder(lifecycle)
    .setPager(NoPagesPager())
    .into(recyclerView)
```

##### PageFractionPager

The `PageFractionPager` is similar to `PageSizePager` but will request new pages when a fraction of the expected
elements has been shown. This is much better from a loading perspective to provide a seamless UI to the end user,
because elements will be loaded before reaching the end of the current dataset.

```kotlin
Adapter.builder(lifecycle)
    .setPager(PageFractionPager(expectedPageSize = 30, fraction = 0.7F))
    .into(recyclerView)
```

##### SourceResultsPager

The `SourceResultsPager` is an advanced pager, similar to `PageFractionPager`, but it counts the page size at runtime, after all sources
have provided their results. It also accepts a selector which will let you work only on specific sources.

### Page keys

For meaningful pagination in your `Source` implementation, you will often need a mechanism
to store a "key" object about pages, which helps formulating the query for the next page.
Sources can do this by assigning a private key of type `Any` to each page.

For example, after fetching data for page `page`, you can use `setKey(page, lastItem)`.
When a new page is requested, you can use `getKey(page.previous())` to know which was the last item
and pass meaningful constraints to your server.