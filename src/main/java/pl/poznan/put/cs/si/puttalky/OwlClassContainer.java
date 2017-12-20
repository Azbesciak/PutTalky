package pl.poznan.put.cs.si.puttalky;

import org.semanticweb.owlapi.model.OWLClass;

import java.util.Objects;
import java.util.Set;

public class OwlClassContainer {
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

        boolean isReal() {
            return !name.toLowerCase().equals("nothing");
        }
    }