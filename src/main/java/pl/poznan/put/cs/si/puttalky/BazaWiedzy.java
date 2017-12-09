package pl.poznan.put.cs.si.puttalky;

import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


public class BazaWiedzy {

    OWLReasoner silnik;
    private OWLOntologyManager manager = null;
    private OWLOntology ontologia;
    private Set<OWLClass> listaKlas;
    private Set<OWLClass> listaDodatkow;

    public static void main(String[] args) {
        BazaWiedzy baza = new BazaWiedzy();
        baza.inicjalizuj();

        OWLClass mieso = baza.manager.getOWLDataFactory().getOWLClass(IRI.create("http://semantic.cs.put.poznan.pl/ontologie/pizza.owl#DodatekMięsny"));
        for (org.semanticweb.owlapi.reasoner.Node<OWLClass> klasa : baza.silnik.getSubClasses(mieso, true)) {
            System.out.println("klasa:" + klasa.toString());
        }
        for (OWLClass d : baza.listaDodatkow) {
            System.out.println("dodatek: " + d.toString());
        }

    }

    public void inicjalizuj() {
        InputStream plik = this.getClass().getResourceAsStream("/pizza.owl");
        manager = OWLManager.createOWLOntologyManager();

        try {
            ontologia = manager.loadOntologyFromOntologyDocument(plik);
            silnik = new Reasoner.ReasonerFactory().createReasoner(ontologia);
            listaKlas = ontologia.getClassesInSignature();
            listaDodatkow = new HashSet<>();

            OWLClass dodatek = manager.getOWLDataFactory().getOWLClass(IRI.create("http://semantic.cs.put.poznan.pl/ontologie/pizza.owl#Dodatek"));
            for (org.semanticweb.owlapi.reasoner.Node<OWLClass> klasa : silnik.getSubClasses(dodatek, false)) {
                listaDodatkow.add(klasa.getRepresentativeElement());
            }


        } catch (OWLOntologyCreationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public Set<String> dopasujDodatek(String s) {
        Set<String> result = new HashSet<>();
        for (OWLClass klasa : listaDodatkow) {
            if (klasa.toString().toLowerCase().contains(s.toLowerCase()) && s.length() > 2) {
                result.add(klasa.getIRI().toString());
            }
        }
        return result;
    }

    public Set<String> wyszukajPizzePoDodatkach(String iri) {
        if (iri == null) {
            return Collections.emptySet();
        }
        OWLObjectProperty maDodatek = manager.getOWLDataFactory().getOWLObjectProperty(IRI.create("http://semantic.cs.put.poznan.pl/ontologie/pizza.owl#maDodatek"));
        Set<OWLClassExpression> ograniczeniaEgzystencjalne = new HashSet<>();

        OWLClass dodatek = manager.getOWLDataFactory().getOWLClass(IRI.create(iri));
        OWLClassExpression wyrazenie = manager.getOWLDataFactory().getOWLObjectSomeValuesFrom(maDodatek, dodatek);
        ograniczeniaEgzystencjalne.add(wyrazenie);

        OWLClassExpression pozadanaPizza = manager.getOWLDataFactory().getOWLObjectIntersectionOf(ograniczeniaEgzystencjalne);
        return StreamSupport.stream(silnik.getSubClasses(pozadanaPizza, false).spliterator(), true)
                .map(Node::getEntities)
                .map(Collection::stream)
                .map(Stream::findFirst)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(OWLClassExpression::asOWLClass)
                .map(OWLNamedObject::getIRI)
                .map(IRI::getFragment)
                .collect(Collectors.toSet());
    }

}
