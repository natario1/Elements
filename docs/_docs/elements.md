---
layout: page
title: "Elements"
description: "Model components responsible for holding data."
order: 2
disqus: 1
---

The base block of the library is the `Element` class. Each element holds our model data,
a reference to the source that created it, and an `Int` value called the element type.

```kotlin
class Element<T: Any>(source: Source<T>, type: Int, data: T?, extra: Any?)
```

The **element type** is the mechanism through which we link [presenters](presenters)
(the UI component) and [sources](sources) (the data loading component):

- Each [source](source) emits elements with one or more types, for example through the `Source.getElementType(T): Int` callback
- Each [presenter](presenter) declares the types it can lay out, in the `Presenter.elementTypes` abstract collection

In case more presenters declare to be able to lay out a given type, the order in which they are added
to the adapter counts.

Keep reading to learn about presenters and sources!