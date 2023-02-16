fun foo(): EditorData {
    return EditorData(
        meta = OpenMap {
            set(SomeKey) { _, _ -> }
        }
    )
}

data class EditorData(val meta: OpenMap<EditorData>)

inline fun <Domain> OpenMap(builder: MutableOpenMap<Domain>.() -> Unit = {}): OpenMap<Domain> = TODO()

typealias OpenMap<D> = BoundedOpenMap<D, Any>

typealias MutableOpenMap<D> = MutableBoundedOpenMap<D, Any>

interface OpenMapView<Domain>

interface BoundedOpenMap<Domain, V : Any> : OpenMapView<Domain>

interface MutableBoundedOpenMap<Domain, V : Any> : BoundedOpenMap<Domain, V> {
    operator fun <T : V> set(k: Key<T, Domain>, v: T)
}

interface Key<V : Any, in Domain>

interface EditorDataKey<T : Any> : Key<T, EditorData>

object SomeKey : EditorDataKey<(String, bar: Any) -> Unit>
