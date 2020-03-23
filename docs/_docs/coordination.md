---
layout: page
title: "Coordination"
description: "Mechanism for declaring dependencies among Sources."
order: 6
disqus: 1
---


In Elements, sources can declare internal dependencies with respect to other sources,
similarly to what `CoordinatorLayout.Behavior`s do. This is done by overriding
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