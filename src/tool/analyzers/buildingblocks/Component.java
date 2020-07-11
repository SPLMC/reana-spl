package tool.analyzers.buildingblocks;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents a component in an asset base.
 *
 * @param <T> Type of asset.
 */
public class Component<T> {

    private String id;
    private String presenceCondition;
    private T asset;
    private Collection<Component<T>> dependencies;

    public Component(String id, String presenceCondition, T asset) {
        this.id = id;
        this.presenceCondition = presenceCondition;
        this.asset = asset;
        this.dependencies = new HashSet<Component<T>>();
    }

    public Component(String id, String presenceCondition, T asset, Collection<Component<T>> dependencies) {
        this(id, presenceCondition, asset);
        this.dependencies = dependencies;
    }

    public String getId() {
        return id;
    }

    public String getPresenceCondition() {
        return presenceCondition;
    }

    public T getAsset() {
        return asset;
    }

    public Collection<Component<T>> getDependencies() {
        return dependencies;
    }

    /**
     * Maps this Component<T> into a Component<U> by means of a function
     * from T to U.
     *
     * This characterizes Component as a functor.
     *
     * @param mapper
     * @return
     */
    public <U> Component<U> fmap(Function<T, U> mapper) {
        Collection<Component<U>> mappedDependencies = this.getDependencies().stream()
                .map(c -> c.fmap(mapper))
                .collect(Collectors.toSet());
        return new Component<U>(this.getId(),
                                this.getPresenceCondition(),
                                mapper.apply(this.getAsset()),
                                mappedDependencies);
    }

    // TODO Candidate!
    public static <P, A, V> V deriveFromMany(List<Component<A>> dependencies,
                                             DerivationFunction<P, A, V> derive,
                                             IsPresent<A, P> isPresent) {
        Map<String, V> derivedModels = new HashMap<String, V>();
        return dependencies.stream()
                .map(c -> deriveSingle(c, isPresent, derive, derivedModels))
                .reduce((first, actual) -> actual)
                .get();
    }

    public static <P, A, V> V newDeriveFromMany(List<Component<A>> dependencies,
                                             DerivationFunction<P, A, V> derive,
                                             IsPresent<A, P> isPresent, Map<String, V> map) {
        return dependencies.stream()
                           .map(c -> deriveSingle(c, isPresent, derive, map))
                           .reduce((first, actual) -> actual)
                           .get();
    }

    // TODO Candidate!
    private static <P, A, V> V deriveSingle(Component<A> component,
                                            IsPresent<A, P> isPresent,
                                            DerivationFunction<P, A, V> derive,
                                            Map<String, V> derivedModels) {
    	long nodeTime = System.currentTimeMillis();
        P presence = isPresent.apply(component);
        V derived = derive.apply(presence, component.getAsset(), derivedModels);
        derivedModels.put(component.getId(), derived);
        nodeTime = System.currentTimeMillis() - nodeTime;
        System.out.println ("Node "+ component.getId() +" deriveSingle: " + nodeTime);
        return derived;
    }

}
