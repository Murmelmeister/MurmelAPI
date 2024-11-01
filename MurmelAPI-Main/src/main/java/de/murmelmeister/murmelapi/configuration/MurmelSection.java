package de.murmelmeister.murmelapi.configuration;

public interface MurmelSection {
    MurmelSection getRoot();

    MurmelSection getParent();

    String getName();
}
