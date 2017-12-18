package pl.poznan.put.cs.si.puttalky;


import com.google.common.base.Optional;
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
import java.util.stream.StreamSupport;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;


public class BazaWiedzy {

    public static final String OWL_ROOT = "http://semantic.cs.put.poznan.pl/ontologie/pizza.owl#";
    OWLReasoner silnik;
    private OWLOntologyManager manager = null;
    private OWLOntology ontologia;
    private Set<OWLClass> listaKlas;
    private Set<OWLClass> listaDodatkow;

    private Set<OWLClass> listaPizz;

    public static void main(String[] args) {
        BazaWiedzy baza = new BazaWiedzy();
        baza.inicjalizuj();

        OWLClass mieso = baza.manager.getOWLDataFactory().getOWLClass(getIri("DodatekMięsny"));
        for (org.semanticweb.owlapi.reasoner.Node<OWLClass> klasa : baza.silnik.getSubClasses(mieso, true)) {
            System.out.println("klasa:" + klasa.toString());
        }
        for (OWLClass d : baza.listaDodatkow) {
            System.out.println("dodatek: " + d.toString());
        }
    }

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
            listaKlas = ontologia.getClassesInSignature();
            listaDodatkow = new HashSet<>();

            listaDodatkow = getSubClasses("Dodatek");
            listaPizz = getSubClasses("Pizza");

            listaPizz.forEach(System.out::println);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
    }

    private Set<OWLClass> getSubClasses(String type) {
        final OWLClass classes = manager.getOWLDataFactory().getOWLClass(getIri(type));
        return StreamSupport
                .stream(silnik.getSubClasses(classes, false).spliterator(), false)
                .map(Node::getRepresentativeElement)
                .collect(toSet());
    }

    public Set<String> dopasujDodatek(String dodatek) {
        return matchExtras(dodatek, s -> s.getIRI().toString());
    }

    public Set<String> getMatchingExtrasNames(String extra) {
        return removeOptionals(matchExtras(extra, this::getNameOfClass));

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

    private <T> Set<T> matchExtras(String extra, Function<OWLClass, T> mapper) {
        return match(listaDodatkow, extra, mapper);
    }

    private <T, C> Set<T> match(Set<C> classes, String toMatch, Function<C, T> mapper) {
        if (toMatch.length() <= 2)
            return emptySet();
        String toMachLower = toMatch.toLowerCase().substring(0, toMatch.length() - 1);
        return classes.stream()
                .filter(c -> doesContainKeyWord(toMachLower, c))
                .map(mapper)
                .sorted(Comparator.comparingInt(a -> a.toString().length()))
                .limit(1)
                .collect(toSet());
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

    public Set<String> lookForMatching(String[] keys) {
        final Set<OwlClassContainer> classes = mapClassesToContainers(listaPizz);
        final Set<OwlClassContainer> possibleOptions =
                getPizzasWhichContainsAtLeastPartOfKeyWords(keys, classes);
        return getFirstHierarchySubClassesAndThisOne(possibleOptions);
    }

    private Set<OwlClassContainer> mapClassesToContainers(Set<OWLClass> classes) {
        return classes.stream()
                .map(this::create)
                .filter(p -> !p.words.isEmpty())
                .collect(toSet());
    }

    private Set<OwlClassContainer> getPizzasWhichContainsAtLeastPartOfKeyWords(
            String[] keyWords, Set<OwlClassContainer> classes) {
        return stream(keyWords)
                .map(s -> lookForMatchingPizzas(classes, s))
                .filter(s -> !s.isEmpty())
                .reduce(toIntersectionOfAll())
                .orElse(emptySet());
    }

    private Set<OwlClassContainer> lookForMatchingPizzas(Set<OwlClassContainer> classes, String s) {
        return classes.stream()
                .filter(c -> !match(c.words, s, x -> x).isEmpty())
                .collect(toSet());
    }

    private BinaryOperator<Set<OwlClassContainer>> toIntersectionOfAll() {
        return (a, b) -> {
            a.retainAll(b);
            return a;
        };
    }

    private Set<String> getFirstHierarchySubClassesAndThisOne(Set<OwlClassContainer> possibleOptions) {
        return possibleOptions
                .stream()
                .flatMap(s -> concat(getSubClasses(s.name).stream(), of(s.clas)))
                .map(this::create)
                .map(s -> s.name)
                .filter(Pizza::isPizza)
                .collect(toSet());
    }

    public Set<String> wyszukajPizzePoDodatkach(Iterable<?> iris) {

        final OWLDataFactory factory = manager.getOWLDataFactory();
        OWLObjectProperty maDodatek = factory.getOWLObjectProperty(getIri("maDodatek"));
        Set<OWLClassExpression> ograniczeniaEgzystencjalne = StreamSupport
                .stream(iris.spliterator(), false)
                .map(o -> (o instanceof Fakt) ? ((Fakt) o).getWartosc() : o.toString())
                .map(IRI::create)
                .map(factory::getOWLClass)
                .map(dodatek -> factory.getOWLObjectSomeValuesFrom(maDodatek, dodatek))
                .collect(Collectors.toSet());

        OWLClassExpression pozadanaPizza = factory.getOWLObjectIntersectionOf(ograniczeniaEgzystencjalne);

        Set<String> pizze = new HashSet<>();
        for (org.semanticweb.owlapi.reasoner.Node<OWLClass> klasa : silnik.getSubClasses(pozadanaPizza, false)) {
            pizze.add(klasa.getEntities().iterator().next().asOWLClass().getIRI().getFragment());
        }

        return pizze;
    }

    public Set<String> wyszukajPizzePoDodatkach(String iri) {
        return wyszukajPizzePoDodatkach(Collections.singleton(iri));
    }

    private static class OwlClassContainer {
        OWLClass clas;
        Set<String> words;
        String name;

        OwlClassContainer(OWLClass clas, Set<String> words, String name) {
            this.clas = clas;
            this.name = name;
            this.words = words;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OwlClassContainer that = (OwlClassContainer) o;
            return Objects.equals(clas, that.clas) &&
                    Objects.equals(words, that.words) &&
                    Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(clas, words, name);
        }

        @Override
        public String toString() {
            return "OwlClassContainer{" +
                    "clas=" + clas +
                    ", words=" + words +
                    ", name='" + name + '\'' +
                    '}';
        }
    }
}
