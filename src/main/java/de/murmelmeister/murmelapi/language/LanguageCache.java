package de.murmelmeister.murmelapi.language;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LanguageCache is a thread-safe cache for storing Language objects.
 * It uses a ConcurrentHashMap to allow concurrent access and modifications.
 */
public final class LanguageCache {
    private final ConcurrentHashMap<Integer, Language> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> nameToId = new ConcurrentHashMap<>();

    public boolean containsId(int id) {
        return cache.containsKey(id);
    }

    public boolean containsName(String name) {
        return name != null && nameToId.containsKey(name.toLowerCase());
    }

    public Language getById(int id) {
        return cache.get(id);
    }

    public Language getByName(String name) {
        if (name == null) return null;
        Integer id = nameToId.get(name.toLowerCase());
        return id != null ? cache.get(id) : null;
    }

    public void put(Language language) {
        cache.put(language.getId(), language);
        nameToId.put(language.getName().toLowerCase(), language.getId());
    }

    public void remove(int id) {
        Language removed = cache.remove(id);
        if (removed != null)
            nameToId.remove(removed.getName().toLowerCase());
    }

    public void clear() {
        cache.clear();
        nameToId.clear();
    }

    public List<Language> getLanguages() {
        return List.copyOf(cache.values());
    }

    public Set<String> getNames() {
        return Set.copyOf(nameToId.keySet());
    }
}
