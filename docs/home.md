---
layout: main
title: "Elements"
---

# Elements

Elements is collection of modular elements for RecyclerView lists, alternative to
[Google's Paging library](https://developer.android.com/topic/libraries/architecture/paging/),
designed in Kotlin with separation of concerns, modularity and testability in mind.

<p align="center">
  <img src="static/banner.png" vspace="10" width="100%">
</p>

- Separation of concerns: we split the model component into `Source`s, and the UI component into `Presenter`s. [[docs]](docs/sources)
- Simplicity: No need to extend Adapters, ViewHolders or all that Paging lib. boilerplate.
- Reusability: as a result, each `Source` and `Presenter` is an independent piece of code that can be reused.
- Modularity: let the adapter accept multiple `Source`s and `Presenter`s. [[docs]](docs/adapter)
- Testability: a consequence of the above, each component can be independently tested.
- Coordination: let `Source`s declare dependencies among them, in a `CoordinatorLayout.Behavior` fashion. [[docs]](docs/coordination)
- Paging: built-in concept of `Page`. [[docs]](docs/pagination)
- Integration with Arch components: heavy use of `LiveData` and `Lifecycle`s, extensions for data binding.
- Animations: give `Presenters`s fine grained control over how to animate each item [[docs]](docs/animations)

### Get started

Get started with [install info](about/install) or start reading the in-depth [documentation](docs/adapter).

If you are curious about how Elements works in practice, take a look at the demo app in the `app` directory in GitHub.

### Support

If you like the project, use it with profit, and want to thank back, please consider [donating or
becoming a sponsor](extra/donate).

