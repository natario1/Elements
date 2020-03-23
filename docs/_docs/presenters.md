---
layout: page
title: "Presenters"
description: "UI components responsible for displaying Elements on screen."
order: 3
disqus: 1
---

In Elements, presenters have the responsibility of laying down elements, that is, creating `View`s and binding Element data to them.
This is the simplest component and most of the time you don't even need to subclass the `Presenter` class, thanks
to the [extensions](extensions) provided by the library.

These are the main tasks:

|Task|Function|Description|
|----|--------|-----------|
|Holder creation|`onCreate(ViewGroup, Int)`|Here you must provide a `Holder` instance, typically inflating a layout resource.|
|Holder initialization|`onInitialize(Holder, Int)`|The holder was created. You can perform here initialization task that do not depend on data (like color filters to icon), or add Views and object to the Holder cache using `Holder.set(key, data)` and `Holder.get(key)`.|
|Binding|`onBind(Page, Holder, Element<T>)`|Bind data, contained in the given `Element`, to the view held by `Holder`.|

Presenters also **accept a click listener** that will be automatically added to each view.
The click listener will be added to the root view of the Holder, or, if found, to a child view that
has the id `R.id.click`. This way you can still use the provided listener for internal clicks.