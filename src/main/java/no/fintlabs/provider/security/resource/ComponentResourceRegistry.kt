package no.fintlabs.provider.security.resource

import lombok.Getter
import no.fintlabs.metamodel.MetamodelService
import org.springframework.stereotype.Component

@Getter
@Component
class ComponentResourceRegistry(
    metamodelService: MetamodelService
) {
    private val resourceIdentifiers: Set<String> = createResourceIdentifiers(metamodelService)

    /**
     * Checks if a resource defined by the given combination of domain, package, and resource name exists.
     * * @param domainName The domain component.
     * @param packageName The package component.
     * @param resourceName The resource name component.
     * @return true if the combined resource identifier exists in the registry, false otherwise.
     */
    fun containsResource(domainName: String, packageName: String, resourceName: String) =
        resourceIdentifiers.contains(formatResourceIdentifier(domainName, packageName, resourceName))

    private fun createResourceIdentifiers(metamodelService: MetamodelService) =
        metamodelService.getComponents().flatMap { component ->
            component.resources.map { resource ->
                formatResourceIdentifier(component.domainName, component.packageName, resource.name)
            }
        }.toSet()

    private fun formatResourceIdentifier(domainName: String, packageName: String, resourceName: String) =
        "$domainName-$packageName-$resourceName".lowercase()

}
