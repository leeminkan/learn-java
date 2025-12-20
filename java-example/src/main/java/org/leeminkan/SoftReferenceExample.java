package org.leeminkan;

import java.lang.ref.SoftReference;
import java.util.function.Predicate;

public class SoftReferenceExample {
    public static void main(String[] args) {
        // Create a strong reference to an object
        String data = new String("Large Data Object");

        // Create a SoftReference to the same object
        SoftReference<String> softRef = new SoftReference<>(data);

        // Nullify the strong reference, making the object softly reachable
        data = null;

        // The garbage collector might clear the soft reference if memory is low
        // or at its discretion.
        System.gc(); // Request garbage collection (not guaranteed to run immediately)

        // Attempt to retrieve the object
        String retrievedData = softRef.get();

        if (retrievedData != null) {
            System.out.println("Object retrieved: " + retrievedData);
        } else {
            System.out.println("Object has been garbage collected.");
        }
    }
}