---
layout: page
title: "The Adapter"
description: "The final object managing Elements components"
order: 1
disqus: 1
---

In Elements, the adapter is final and can't be extended, as you would
typically do with other list management library. The adapter acts as an
orchestrator of the different, modular components that will be involved
in displaying the list items.

### Responsibilities

Describing what the adapter does will be helpful to introduce these
components and for you to understand what the library can do.
The adapter responsibilities are:

- manage `Presenter`s: for example, dispatches elements to the correct presenter [[docs]](presenters)
- manage `Source`s: for example, creates a dependency tree among them, throwing in case of circular dependencies [[docs]](sources)
- manage `Page`s: for example, if `pageSizeHint` is specified, the adapter will automatically open a new page when the `pageSizeHint`-th element has been requested for the current page [[docs]](paging)
- manage `Element`s: for example, [finds the correct presenter](elements), and [orders them based on the source behavior](coordination).
- manage [page updates](sources#diffutils): computes the difference between pages using `DiffUtil`, like the Paging library does.

As a side feature, the adapter will install an `Animator` as default item animator for your `RecyclerView`.
See [Animations](#animations) for more informations.