package pl.poznan.put.cs.si.puttalky;

import io.vavr.Value;
import io.vavr.collection.Array;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import pl.poznan.put.cs.si.puttalky.model.Fakt;
import pl.poznan.put.cs.si.puttalky.model.Pizza;

import java.io.InputStream;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;


public class BazaWiedzy {

    private static final String OWL_ROOT = "http://semantic.cs.put.poznan.pl/ontologie/pizza.owl#";
    OWLReasoner silnik;
    private OWLOntologyManager manager;
    private OWLOntology ontologia;
    private Set<OwlClassContainer> extras;
    private Set<OwlClassContainer> pizzas;
    private Set<String> namedPizzas;
    private OWLDataFactory factory;

    private static String[] splitCamelCase(String camel) {
        return camel.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
    }

    private static IRI getIri(String name) {
        return IRI.create(OWL_ROOT + name);
    }

    private OwlClassContainer create(OWLClass clas) {
        String name = parseName(clas);
        Set<String> normalizedNames =
                name != null
                        ? new HashSet<>(asList(getNameKeyWords(name)))
                        : emptySet();
        return new OwlClassContainer(clas, normalizedNames, name);
    }

    private String parseName(OWLClass clas) {
        return clas.getIRI().getRemainder().orNull();
    }

    private String[] getNameKeyWords(String name) {
        final String[] strings = splitCamelCase(name);
        return Parser.normalize(strings);
    }

    public void init() {
        InputStream knowledgeSource = this.getClass().getResourceAsStream("/pizza.owl");
        manager = OWLManager.createOWLOntologyManager();
        factory = manager.getOWLDataFactory();
        try {
            ontologia = manager.loadOntologyFromOntologyDocument(knowledgeSource);
            silnik = new Reasoner.ReasonerFactory().createReasoner(ontologia);
            extras = getSubClasses("Dodatek");
            pizzas = getSubClasses("Pizza");
            namedPizzas = getSubClasses("NazwanaPizza").stream().map(s -> s.name).collect(toSet());
            printMenuFor(pizzas, "pizze");
            printMenuFor(extras, "dodatki");
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
    }

    private void printMenuFor(Set<OwlClassContainer> data, String header) {
        System.out.println(header + " :");
        data.stream().map(s -> "\t" + s.name).forEach(System.out::println);
    }

    private Set<OwlClassContainer> getSubClasses(String type) {
        final OWLClass clas = manager.getOWLDataFactory().getOWLClass(getIri(type));
        return getSubClasses(clas);
    }
    private Set<OwlClassContainer> getSubClasses(OWLClass clas) {
        return StreamSupport
                .stream(silnik.getSubClasses(clas, false).spliterator(), false)
                .map(Node::getRepresentativeElement)
                .map(this::create)
                .filter(p -> !p.words.isEmpty())
                .filter(OwlClassContainer::isReal)
                .collect(toSet());
    }

    public Set<String> matchExtras(String[] keyWords) {
        return getAllMatchingToKeysClasses(keyWords, extras, true)
                .flatMap(Collection::stream)
                .map(c -> c.name)
                .collect(Collectors.toSet());
    }

    public Set<String> lookForPizzas(String[] keys) {
        return lookForMatching(keys, pizzas);
    }

    private Set<String> lookForMatching(String[] keys, Set<OwlClassContainer> toMatch) {
        final Set<OwlClassContainer> possibleOptions =
                getIntersectionOfClassesSharingKeyWords(keys, toMatch);
        return getFirstHierarchySubClassesAndThisOne(possibleOptions, s -> s.name);
    }

    private <T, C> Set<T> match(Set<C> classes, String toMatch, Function<C, T> mapper) {
        final int length = toMatch.length();
        toMatch = toMatch.toLowerCase();
        if (length <= 2 || toMatch.matches("pizz[aeęy]"))
            return emptySet();
        String toMachLower = length <= 4 ? toMatch : toMatch.substring(0, length - 1);
        return getTheBestMatch(
                classes.stream()
                .filter(c -> doesContainKeyWord(toMachLower, c))
                .map(mapper), false
        );
    }

    private <T> Set<T> getTheBestMatch(Stream<T> results, boolean single) {
        if (single) {
            results = results
                .sorted(Comparator.comparingInt(a -> a.toString().length()))
                .limit(1);
        }
        return results.collect(Collectors.toSet());
    }

    private <C> boolean doesContainKeyWord(String toMachLower, C c) {
        final String lowerCased = c.toString().toLowerCase();
        final int indexOfHash = lowerCased.lastIndexOf("#");
        final int indexOfEnd = lowerCased.lastIndexOf(">");

        final String origin = lowerCased.substring(
                indexOfHash > 0 ? indexOfHash + 1 : 0,
                indexOfEnd > 0 ? indexOfEnd : lowerCased.length());
        return origin.contains(toMachLower);
    }

    private Set<OwlClassContainer> getIntersectionOfClassesSharingKeyWords(
            String[] keyWords, Set<OwlClassContainer> classes) {
        return getAllMatchingToKeysClasses(keyWords, classes, false)
                .reduce(toIntersectionOfAll())
                .orElse(emptySet());
    }

    private Stream<Set<OwlClassContainer>> getAllMatchingToKeysClasses(String[] keyWords, Set<OwlClassContainer> classes, boolean single) {
        return stream(keyWords)
                .map(s -> lookForMatching(classes, s, single))
                .filter(s -> !s.isEmpty());
    }

    private Set<OwlClassContainer> lookForMatching(Set<OwlClassContainer> classes, String s, boolean single) {
        return getTheBestMatch(
                classes.stream()
                    .filter(c -> !match(c.words, s, x -> x).isEmpty()), single
        );
    }

    private BinaryOperator<Set<OwlClassContainer>> toIntersectionOfAll() {
        return (a, b) -> {
            a.retainAll(b);
            return a;
        };
    }

    private <T> Set<T> getFirstHierarchySubClassesAndThisOne(
            Set<OwlClassContainer> possibleOptions,
            Function<OwlClassContainer, T> mapper) {
        return possibleOptions
                .stream()
                .flatMap(s -> concat(getSubClasses(s.name).stream(), of(s)))
                .filter(OwlClassContainer::isReal)
                .map(mapper)
                .collect(toSet());
    }

    public Set<String> lookForPizzasByExtras(
            Set<Fakt> withExtras, Set<Fakt> withoutExtras,
            Set<Fakt> withPizzas, Set<Fakt> withoutPizzas) {
        final Set<String> ok = lookForPizzasByExtras(withExtras, withPizzas.isEmpty());
        final Set<String> orderPizzas = mapFactsToStrings(withPizzas);
        ok.addAll(orderPizzas);
        if (!withoutExtras.isEmpty()) {
            final Set<String> notOk = lookForPizzasByExtras(withoutExtras, false);
            ok.removeAll(notOk);
        }
        final Set<String> noPizzaOrders = mapFactsToStrings(withoutPizzas);
        ok.removeAll(noPizzaOrders);
        ok.retainAll(namedPizzas);
        return ok;
    }

    private Set<String> mapFactsToStrings(Set<Fakt> facts) {
        return facts.stream().map(Fakt::getWartosc).collect(Collectors.toSet());
    }

    public Set<String> lookForPizzasByExtras(Set<Fakt> extras, boolean allIfEmpty) {
        if (extras.isEmpty() && allIfEmpty) {
            return pizzas.stream().map(s -> s.name).collect(Collectors.toSet());
        }
        OWLObjectProperty hasExtra = factory.getOWLObjectProperty(getIri("maDodatek"));
        final Array<Set<OwlClassContainer>> subclasses = Array.ofAll(extras)
                .map(Fakt::getWartosc)
                .map(BazaWiedzy::getIri)
                .map(factory::getOWLClass)
                .map(this::create)
                .map(s -> getFirstHierarchySubClassesAndThisOne(singleton(s), x -> x));

        final Array<Array<OWLClass>> extrasWithSubclasses = subclasses
                .map(s -> s.stream().map(c -> c.clas))
                .map(Array::ofAll);

        final Array<OWLObjectIntersectionOf> expressions = Permutator.permute(extrasWithSubclasses)
                .map(extra -> extra.map(d -> factory.getOWLObjectSomeValuesFrom(hasExtra, d)))
                .map(Value::toJavaSet)
                .map(factory::getOWLObjectIntersectionOf);

        final Array<String> pizzas = expressions
                .map(exp -> silnik.getSubClasses(exp, false))
                .map(Array::ofAll)
                .flatMap(classes -> classes.flatMap(clas ->
                        Array.ofAll(clas.getEntities()).map(c -> c.asOWLClass().getIRI().getFragment())));
        return pizzas
                .filter(Pizza::isPizza)
                .toJavaSet();
    }
}
