internal interface GradleInjectedSyncActionRunner {
    fun <T> runActions(actionsToRun: List<ActionToRun<T>>): List<T>
}

data class ActionToRun<T>(
    private val action: (String) -> T,
)

class SyncActionRunner : GradleInjectedSyncActionRunner {
    override fun <T> runActions(actionsToRun: List<ActionToRun<T>>): List<T> {
        return emptyList()
    }
}

private fun test(
    runner: SyncActionRunner,
    moduleConfigurationsToRequest: List<String>,
    preResolvedVariants: Map<String, SyncVariantResult>
) {
    val actions =
        moduleConfigurationsToRequest.map { moduleConfiguration ->
            val prefetchedModel = preResolvedVariants[moduleConfiguration]
            if (prefetchedModel != null) {
                // Return an action that simply returns the `prefetchedModel`.
                val action = (fun(_: String) = prefetchedModel)
                ActionToRun(action)
            }
            else {
                getVariantAndModuleDependenciesAction(
                    moduleConfiguration
                )
            }
        }
    runner.runActions(<!ARGUMENT_TYPE_MISMATCH!>actions<!>)
}

private fun getVariantAndModuleDependenciesAction(moduleConfiguration: String): ActionToRun<SyncVariantResult?> =
    ActionToRun(
        { _ -> null }
    )

internal class SyncVariantResult
