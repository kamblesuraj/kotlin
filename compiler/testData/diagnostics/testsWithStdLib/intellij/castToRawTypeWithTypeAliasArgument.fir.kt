interface FlameGraphModel<T>

internal typealias CallUsageNode = CallTreeNode<BaseCallStackElement>

interface CallTreeNode<out T : Any> : TreeNodeWithParent<CallWithValue<T>>

interface TreeNodeWithParent<out Data>

interface CallWithValue<out T : Any>

abstract class BaseCallStackElement

open class CallUsageNodeFlameGraphModel<Call : Any> : FlameGraphModel<CallTreeNode<Call>>

fun foo(model: FlameGraphModel<CallUsageNode>) {
    val afterCast = model as <!NO_TYPE_ARGUMENTS_ON_RHS!>CallUsageNodeFlameGraphModel<!>
}
