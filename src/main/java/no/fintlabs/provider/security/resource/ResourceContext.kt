package no.fintlabs.provider.security.resource

import lombok.Getter
import no.fintlabs.metamodel.MetamodelService
import org.springframework.stereotype.Component

@Getter
@Component
class ResourceContext(
    metamodelService: MetamodelService
) {
    val validResources: Set<String> = createValidResources(metamodelService)

    private fun createValidResources(metamodelService: MetamodelService) =
        metamodelService.getComponents().flatMap { component ->
            component.resources.map { resource ->
                "${component.domainName}-${component.packageName}-${resource.name}"
            }
        }.toSet()

}
