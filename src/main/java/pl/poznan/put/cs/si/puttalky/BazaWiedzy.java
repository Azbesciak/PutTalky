package pl.poznan.put.cs.si.puttalky;


import com.google.common.base.Optional;
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

    public static final String OWL_ROOT = "http://semantic.cs.put.poznan.pl/ontologie/pizza.owl#";
    OWLReasoner silnik;
    private OWLOntologyManager manager = null;
    private OWLOntology ontologia;
    private Set<OwlClassContainer> listaDodatkow;

    private Set<OwlClassContainer> listaPizz;

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
        return getNameOfClass(clas).orNull();
    }

    private String[] getNameKeyWords(String name) {
        final String[] strings = splitCamelCase(name);
        return Parser.normalize(strings);
    }

    public void inicjalizuj() {
        InputStream plik = this.getClass().getResourceAsStream("/pizza.owl");
        manager = OWLManager.createOWLOntologyManager();

        try {
            ontologia = manager.loadOntologyFromOntologyDocument(plik);
            silnik = new Reasoner.ReasonerFactory().createReasoner(ontologia);
            listaDodatkow = new HashSet<>();

            listaDodatkow = getSubClasses("Dodatek");
            listaPizz = getSubClasses("Pizza");
            System.out.println("pizze");
            listaPizz.forEach(System.out::println);
            System.out.println("dodatki");
            listaDodatkow.forEach(System.out::println);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
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
        return getAllMatchingToKeysClasses(keyWords, listaDodatkow, true)
                .flatMap(Collection::stream)
                .map(c -> c.name)
                .collect(Collectors.toSet());
    }

    public Set<String> lookForPizzas(String[] keys) {
        return lookForMatching(keys, listaPizz);
    }

    private Set<String> lookForMatching(String[] keys, Set<OwlClassContainer> toMatch) {
        final Set<OwlClassContainer> possibleOptions =
                getIntersectionOfClassesSharingKeyWords(keys, toMatch);
        return getFirstHierarchySubClassesAndThisOne(possibleOptions, s -> s.name);
    }

    public Set<String> getMatchingExtrasNames(String extra) {
        return removeOptionals(matchExtras(extra, this::getNameOfClass, true));
    }

    private Optional<String> getNameOfClass(OwlClassContainer s) {
        return getNameOfClass(s.clas);
    }

    private Optional<String> getNameOfClass(OWLClass s) {
        return s.getIRI().getRemainder();
    }

    private <T> Set<T> removeOptionals(Set<Optional<T>> setOfOpt) {
        return setOfOpt.stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toSet());
    }

    private <T> Set<T> matchExtras(String extra, Function<OwlClassContainer, T> mapper, boolean single) {
        return match(listaDodatkow, extra, mapper, single);
    }

    private <T, C> Set<T> match(Set<C> classes, String toMatch, Function<C, T> mapper, boolean single) {
        if (toMatch.length() <= 2)
            return emptySet();
        String toMachLower = toMatch.toLowerCase().substring(0, toMatch.length() - 1);
        return getTheBestMatch(
                classes.stream()
                .filter(c -> doesContainKeyWord(toMachLower, c))
                .map(mapper), single
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
                    .filter(c -> !match(c.words, s, x -> x, false).isEmpty()), single
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

    public Set<String> lookForPizzasByExtras(Set<Fakt> with, Set<Fakt> without, Set<Fakt> fromOrder) {
        final Set<String> ok = wyszukajPizzePoDodatkach(with, fromOrder.isEmpty());
        final Set<String> orderPizzas = fromOrder.stream().map(Fakt::getWartosc).collect(Collectors.toSet());
        ok.addAll(orderPizzas);
        if (!without.isEmpty()) {
            final Set<String> notOk = wyszukajPizzePoDodatkach(without, false);
            ok.removeAll(notOk);
        }
        return ok;
    }

    public Set<String> wyszukajPizzePoDodatkach(Set<Fakt> dodatki, boolean allIfEmpty) {

        final OWLDataFactory factory = manager.getOWLDataFactory();
        if (dodatki.isEmpty() && allIfEmpty) {
            return listaPizz.stream().map(s -> s.name).collect(Collectors.toSet());
        }
        OWLObjectProperty maDodatek = factory.getOWLObjectProperty(getIri("maDodatek"));
        final Array<Set<OwlClassContainer>> subclasses = Array.ofAll(dodatki)
                .map(Fakt::getWartosc)
                .map(BazaWiedzy::getIri)
                .map(factory::getOWLClass)
                .map(this::create)
                .map(s -> getFirstHierarchySubClassesAndThisOne(singleton(s), x -> x));

        final Array<Array<OWLClass>> extrasWithSubclasses = subclasses
                .map(s -> s.stream().map(c -> c.clas))
                .map(Array::ofAll);

        final Array<OWLObjectIntersectionOf> expressions = Permutator.permute(extrasWithSubclasses)
                .map(extras -> extras.map(d -> factory.getOWLObjectSomeValuesFrom(maDodatek, d)))
                .map(Value::toJavaSet)
                .map(factory::getOWLObjectIntersectionOf);

        return expressions
                .map(exp -> silnik.getSubClasses(exp, false))
                .map(Array::ofAll)
                .flatMap(classes -> classes.map(clas -> clas.getEntities().iterator().next().asOWLClass().getIRI().getFragment()))
                .filter(Pizza::isPizza)
                .toJavaSet();
    }
}
