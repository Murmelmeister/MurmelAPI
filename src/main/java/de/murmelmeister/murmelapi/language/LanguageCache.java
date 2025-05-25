package de.murmelmeister.murmelapi.language;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class LanguageCache {
    private final ConcurrentHashMap<Integer, Language> cache = new ConcurrentHashMap<>();

    public boolean containsKey(int id) {
        return cache.containsKey(id);
    }

    public Language get(int id) {
        return cache.get(id);
    }

    public void put(Language language) {
        cache.put(language.getId(), language);
    }

    public void remove(int id) {
        cache.remove(id);
    }

    public void clear() {
        cache.clear();
    }

    public List<Language> getLanguages() {
        return List.copyOf(cache.values());
    }
}
