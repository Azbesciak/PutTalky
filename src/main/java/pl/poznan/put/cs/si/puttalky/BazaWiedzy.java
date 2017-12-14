package pl.poznan.put.cs.si.puttalky;


import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.base.Optional;
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

        OWLClass dodatek  = manager.getOWLDataFactory().getOWLClass(IRI.create("http://semantic.cs.put.poznan.pl/ontologie/pizza.owl#Dodatek"));
        for (org.semanticweb.owlapi.reasoner.Node<OWLClass> klasa: silnik.getSubClasses(dodatek, false)) {
          listaDodatkow.add(klasa.getRepresentativeElement());
        }

      } catch (OWLOntologyCreationException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    
    public Set<String> dopasujDodatek(String dodatek){
        return matchExtras(dodatek, s -> s.getIRI().toString());
    }

    public Set<String> getMatchingExtrasNames(String extra) {
        return matchExtras(extra, s -> s.getIRI().getRemainder())
                .stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    private <T> Set<T> matchExtras(String extra, Function<OWLClass, T> mapper) {
        if (extra.length() <= 2)
            return Collections.emptySet();
        String extraLowerCase = extra.toLowerCase();
        return listaDodatkow.stream()
                .filter(c -> c.toString().toLowerCase().contains(extraLowerCase))
                .map(mapper)
                .collect(Collectors.toSet());
    }
    
    public Set<String> wyszukajPizzePoDodatkach(String iri){
    	Set<String> pizze = new HashSet<>();
    	OWLObjectProperty maDodatek = manager.getOWLDataFactory().getOWLObjectProperty(IRI.create("http://semantic.cs.put.poznan.pl/ontologie/pizza.owl#maDodatek"));
    	Set<OWLClassExpression> ograniczeniaEgzystencjalne = new HashSet<>();

    	OWLClass dodatek = manager.getOWLDataFactory().getOWLClass(IRI.create(iri));
    	OWLClassExpression wyrazenie = manager.getOWLDataFactory().getOWLObjectSomeValuesFrom(maDodatek, dodatek);
    	ograniczeniaEgzystencjalne.add(wyrazenie);
  	
    	OWLClassExpression pozadanaPizza = manager.getOWLDataFactory().getOWLObjectIntersectionOf(ograniczeniaEgzystencjalne);
    	
      for (org.semanticweb.owlapi.reasoner.Node<OWLClass> klasa: silnik.getSubClasses(pozadanaPizza, false)) {
        pizze.add(klasa.getEntities().iterator().next().asOWLClass().getIRI().getFragment());
      }
	
		  return pizze;
    }
}
