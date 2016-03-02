package io.fabric8.kubeflix.examples.hellohystrix;


public class Main {

    public static void main(String[] args) {
        for (int i=0; i < 1000000000; i++) {
            HelloCommand command = new HelloCommand();
            command.execute();
        }
    }
}
