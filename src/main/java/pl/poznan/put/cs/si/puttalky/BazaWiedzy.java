package pl.poznan.put.cs.si.puttalky;


import com.google.common.base.Optional;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import pl.poznan.put.cs.si.puttalky.model.Pizza;

import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static java.util.stream.Stream.*;


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

    private static class OwlClassContainer {
        OWLClass clas;
        Set<String> words;
        String name;

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

        OwlClassContainer(OWLClass clas, Set<String> words, String name) {
            this.clas = clas;

            this.name = name;
            this.words = words;
        }
    }

    private OwlClassContainer create(OWLClass clas) {
        String name = parseName(clas);
        Set<String> normalizedNames = name != null
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

    private static String[] splitCamelCase(String camel) {
        return camel.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
    }

    private static IRI getIri(String name) {
        return IRI.create(OWL_ROOT + name);
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
                .filter(c -> c.toString().toLowerCase()
                        .substring(0, c.toString().length() - 1)
                        .contains(toMachLower))
                .map(mapper)
                .collect(toSet());
    }

    public Set<String> lookForMatching(String[] keys) {
        final Set<OwlClassContainer> classes = mapClassesToContainers(listaPizz);
        return stream(keys)
                .map(s -> lookForMatchingPizzas(classes, s))
                .filter(s -> !s.isEmpty())
                .reduce((a,b) -> {a.retainAll(b); return a;})
                .orElse(emptySet())
                .stream()
                .flatMap(s -> concat(getSubClasses(s.name).stream(), of(s.clas)))
                .map(this::create)
                .map(s -> s.name)
                .filter(Pizza::isPizza)
                .collect(toSet());
    }

    private Set<OwlClassContainer> mapClassesToContainers(Set<OWLClass> classes) {
        return classes.stream()
                .map(this::create)
                .filter(p -> !p.words.isEmpty())
                .collect(toSet());
    }

    private Set<OwlClassContainer> lookForMatchingPizzas(Set<OwlClassContainer> classes, String s) {
        return classes.stream()
                .filter(c -> !match(c.words, s, x -> x).isEmpty())
                .collect(toSet());
    }

    public Set<String> lookForPizzaByName(Set<String> pizzaWords) {
        Set<String> pizze = new HashSet<>();
        final OWLDataFactory factory = manager.getOWLDataFactory();
        OWLObjectProperty pizzaOWL = factory
                .getOWLObjectProperty(getIri("Pizza"));
        Set<OWLClassExpression> ograniczeniaEgzystencjalne =
                pizzaWords.stream()
                        .map(iri -> factory.getOWLClass(IRI.create(iri)))
                        .map(p -> factory.getOWLObjectSomeValuesFrom(pizzaOWL, p))
                        .collect(toSet());

        OWLClassExpression pozadanaPizza = factory.getOWLObjectIntersectionOf(ograniczeniaEgzystencjalne);

        for (org.semanticweb.owlapi.reasoner.Node<OWLClass> klasa : silnik.getSubClasses(pozadanaPizza, false)) {
            pizze.add(klasa.getEntities().iterator().next().asOWLClass().getIRI().getFragment());
        }

        return pizze;
    }

    public Set<String> wyszukajPizzePoDodatkach(String iri) {
        Set<String> pizze = new HashSet<>();
        OWLObjectProperty maDodatek = manager.getOWLDataFactory().getOWLObjectProperty(getIri("maDodatek"));
        Set<OWLClassExpression> ograniczeniaEgzystencjalne = new HashSet<>();

        OWLClass dodatek = manager.getOWLDataFactory().getOWLClass(IRI.create(iri));
        OWLClassExpression wyrazenie = manager.getOWLDataFactory().getOWLObjectSomeValuesFrom(maDodatek, dodatek);
        ograniczeniaEgzystencjalne.add(wyrazenie);

        OWLClassExpression pozadanaPizza = manager.getOWLDataFactory().getOWLObjectIntersectionOf(ograniczeniaEgzystencjalne);

        for (org.semanticweb.owlapi.reasoner.Node<OWLClass> klasa : silnik.getSubClasses(pozadanaPizza, false)) {
            pizze.add(klasa.getEntities().iterator().next().asOWLClass().getIRI().getFragment());
        }

        return pizze;
    }
}
